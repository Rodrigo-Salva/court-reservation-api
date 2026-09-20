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
import org.springframework.data.domain.Sort;
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
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseGet(() -> Payment.builder().booking(booking).user(booking.getUser()).operationCode("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()).build());
        if (payment.getStatus() == PaymentStatus.APROBADO || payment.getStatus() == PaymentStatus.REEMBOLSADO) throw new BusinessException("Esta reserva ya tiene un pago finalizado");
        boolean rejected = request.getMethod().name().equals("TARJETA") && request.getSimulatedCardNumber() != null && request.getSimulatedCardNumber().replaceAll("\\D", "").endsWith("0000");
        payment.setMethod(request.getMethod()); payment.setAmount(booking.getTotalPrice()); payment.setStatus(rejected ? PaymentStatus.RECHAZADO : PaymentStatus.APROBADO);
        payment.setRejectionReason(rejected ? "Pago simulado rechazado: tarjeta terminada en 0000" : null);
        payment.setPaidAt(rejected ? null : LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        notificationService.notify(booking.getUser(), rejected ? NotificationType.PAGO_RECHAZADO : NotificationType.PAGO_APROBADO,
                rejected ? "Pago no aprobado" : "Pago aprobado", rejected ? "Puedes intentar nuevamente con otro método de pago." : "Tu reserva en " + booking.getCourt().getName() + " quedó pagada. Operación " + saved.getOperationCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto(saved));
    }

    @GetMapping("/my") public List<PaymentResponseDTO> mine(@AuthenticationPrincipal CustomUserDetails currentUser) { return paymentRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream().map(this::dto).toList(); }
    @GetMapping public List<PaymentResponseDTO> all(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Long venueId = accessControl.resolveVenueFilter(currentUser);
        List<Payment> payments = venueId == null ? paymentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")) : paymentRepository.findByBookingCourtVenueIdOrderByCreatedAtDesc(venueId);
        return payments.stream().map(this::dto).toList();
    }
    @PatchMapping("/{id}/refund") @Transactional public PaymentResponseDTO refund(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        accessControl.requireSameVenueOrAdmin(payment.getBooking().getCourt().getVenue() != null ? payment.getBooking().getCourt().getVenue().getId() : null, currentUser);
        if (payment.getStatus() != PaymentStatus.APROBADO) throw new BusinessException("Solo se pueden reembolsar pagos aprobados");
        payment.setStatus(PaymentStatus.REEMBOLSADO); payment.setRefundedAt(LocalDateTime.now()); return dto(payment);
    }
    private PaymentResponseDTO dto(Payment p) { return PaymentResponseDTO.builder().id(p.getId()).bookingId(p.getBooking().getId()).courtName(p.getBooking().getCourt().getName()).amount(p.getAmount()).method(p.getMethod()).status(p.getStatus()).operationCode(p.getOperationCode()).rejectionReason(p.getRejectionReason()).paidAt(p.getPaidAt()).refundedAt(p.getRefundedAt()).userName(p.getUser().getName()).createdAt(p.getCreatedAt()).build(); }
}
