package org.salva.task.court_reservation_system.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.PaymentRequestDTO;
import org.salva.task.court_reservation_system.dto.response.PaymentResponseDTO;
import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.entity.Payment;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.enums.PaymentStatus;
import org.salva.task.court_reservation_system.exception.BusinessException;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.repository.BookingRepository;
import org.salva.task.court_reservation_system.repository.PaymentRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.salva.task.court_reservation_system.service.NotificationService;
import org.salva.task.court_reservation_system.enums.NotificationType;
import org.salva.task.court_reservation_system.dto.response.PageResponseDTO;
import org.salva.task.court_reservation_system.dto.response.PaymentPageResponseDTO;
import org.salva.task.court_reservation_system.repository.spec.PaymentSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentRepository paymentRepository; private final BookingRepository bookingRepository;
    private final AccessControlService accessControl;
    private final NotificationService notificationService;

    @PostMapping @Transactional
    public ResponseEntity<PaymentResponseDTO> pay(@Valid @RequestBody PaymentRequestDTO request, @AuthenticationPrincipal CustomUserDetails currentUser) {
        Booking booking = bookingRepository.findById(request.getBookingId()).orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        accessControl.requireOwnerOrAdmin(booking.getUser().getId(), currentUser);
        if (booking.getStatus() == BookingStatus.CANCELADA) throw new BusinessException("No se puede pagar una reserva cancelada");
        if (booking.getStatus() == BookingStatus.PENDIENTE && booking.getPaymentDeadline() != null && booking.getPaymentDeadline().isBefore(LocalDateTime.now())) throw new BusinessException("El plazo para pagar esta reserva venció");
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseGet(() -> Payment.builder().booking(booking).user(booking.getUser()).operationCode("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()).build());
        if (payment.getStatus() == PaymentStatus.APROBADO || payment.getStatus() == PaymentStatus.REEMBOLSADO) throw new BusinessException("Esta reserva ya tiene un pago finalizado");
        boolean rejected = request.getMethod().name().equals("TARJETA") && request.getSimulatedCardNumber() != null && request.getSimulatedCardNumber().replaceAll("\\D", "").endsWith("0000");
        payment.setMethod(request.getMethod()); payment.setAmount(booking.getTotalPrice()); payment.setStatus(rejected ? PaymentStatus.RECHAZADO : PaymentStatus.APROBADO);
        payment.setRejectionReason(rejected ? "Pago simulado rechazado: tarjeta terminada en 0000" : null);
        payment.setPaidAt(rejected ? null : LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        if (!rejected && booking.getStatus() == BookingStatus.PENDIENTE) { booking.setStatus(BookingStatus.CONFIRMADA); booking.setPaymentDeadline(null); bookingRepository.save(booking); }
        notificationService.notify(booking.getUser(), rejected ? NotificationType.PAGO_RECHAZADO : NotificationType.PAGO_APROBADO,
                rejected ? "Pago no aprobado" : "Pago aprobado", rejected ? "Puedes intentar nuevamente con otro método de pago." : "Tu reserva en " + booking.getCourt().getName() + " quedó pagada. Operación " + saved.getOperationCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto(saved));
    }

    @GetMapping("/my") public List<PaymentResponseDTO> mine(@AuthenticationPrincipal CustomUserDetails currentUser) { return paymentRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream().map(this::dto).toList(); }
    @GetMapping public PaymentPageResponseDTO all(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PaymentStatus status, @RequestParam(required = false) String q,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long venueId = accessControl.resolveVenueFilter(currentUser);
        Specification<Payment> spec = Specification.where(PaymentSpecifications.inVenue(venueId))
                .and(PaymentSpecifications.hasStatus(status)).and(PaymentSpecifications.matches(q));
        Page<PaymentResponseDTO> result = paymentRepository
                .findAll(spec, PageResponseDTO.pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::dto);
        return PaymentPageResponseDTO.builder().content(result.getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .approvedTotal(sum(PaymentStatus.APROBADO, venueId)).refundedTotal(sum(PaymentStatus.REEMBOLSADO, venueId)).build();
    }
    private java.math.BigDecimal sum(PaymentStatus status, Long venueId) {
        return venueId == null ? paymentRepository.sumAmountByStatus(status) : paymentRepository.sumAmountByStatusAndVenue(status, venueId);
    }
    @PatchMapping("/{id}/refund") @Transactional public PaymentResponseDTO refund(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        accessControl.requireSameVenueOrAdmin(payment.getBooking().getCourt().getVenue() != null ? payment.getBooking().getCourt().getVenue().getId() : null, currentUser);
        if (payment.getStatus() != PaymentStatus.APROBADO) throw new BusinessException("Solo se pueden reembolsar pagos aprobados");
        payment.setStatus(PaymentStatus.REEMBOLSADO); payment.setRefundedAt(LocalDateTime.now()); return dto(payment);
    }
    private PaymentResponseDTO dto(Payment p) { return PaymentResponseDTO.builder().id(p.getId()).bookingId(p.getBooking().getId()).courtName(p.getBooking().getCourt().getName()).amount(p.getAmount()).method(p.getMethod()).status(p.getStatus()).operationCode(p.getOperationCode()).rejectionReason(p.getRejectionReason()).paidAt(p.getPaidAt()).refundedAt(p.getRefundedAt()).userName(p.getUser().getName()).createdAt(p.getCreatedAt()).build(); }
}
