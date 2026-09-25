package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.CancellationRequestDTO;
import org.salva.task.court_reservation_system.dto.request.RecurrentBookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.RescheduleBookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.CheckInRequestDTO;
import org.salva.task.court_reservation_system.dto.response.*;
import org.salva.task.court_reservation_system.entity.*;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.salva.task.court_reservation_system.exception.BusinessException;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.mapper.BookingMapper;
import org.salva.task.court_reservation_system.repository.*;
import org.salva.task.court_reservation_system.service.BookingService;
import org.salva.task.court_reservation_system.service.AuditService;
import org.salva.task.court_reservation_system.service.NotificationService;
import org.salva.task.court_reservation_system.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final UserPackageRepository userPackageRepository;
    private final CourtBlockRepository courtBlockRepository;
    private final BookingMapper bookingMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;

    /** Si es true, las reservas nuevas quedan PENDIENTE hasta que se pague dentro del plazo. */
    @org.springframework.beans.factory.annotation.Value("${app.business-rules.booking.require-payment:false}")
    private boolean requirePayment;

    @org.springframework.beans.factory.annotation.Value("${app.business-rules.booking.payment-window-minutes:15}")
    private int paymentWindowMinutes;

    // Constantes de configuración (idealmente desde application.yml)
    private static final int MIN_ADVANCE_HOURS = 2;
    private static final int MIN_DURATION_HOURS = 1;
    private static final int MAX_DURATION_HOURS = 4;
    private static final LocalTime OPERATION_START_TIME = LocalTime.of(6, 0);
    private static final LocalTime OPERATION_END_TIME = LocalTime.of(23, 0);
    private static final LocalTime PEAK_HOURS_START = LocalTime.of(18, 0);
    private static final LocalTime PEAK_HOURS_END = LocalTime.of(22, 0);
    private static final LocalTime VALLEY_HOURS_START = LocalTime.of(6, 0);
    private static final LocalTime VALLEY_HOURS_END = LocalTime.of(12, 0);

    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO requestDTO) {
        log.info("Creating booking for user: {} on court: {}", requestDTO.getUserId(), requestDTO.getCourtId());

        // 1. Obtener entidades
        User user = getUserOrThrow(requestDTO.getUserId());
        Court court = getCourtForBookingOrThrow(requestDTO.getCourtId());

        // 2. Validaciones de reglas de negocio
        validateBookingRules(requestDTO, user, court);

        // 3. Crear booking básico
        Booking booking = bookingMapper.toEntity(requestDTO);
        booking.setUser(user);
        booking.setCourt(court);
        // Las reservas pagadas con paquete ya están cubiertas y se confirman de inmediato.
        boolean awaitingPayment = requirePayment && !Boolean.TRUE.equals(requestDTO.getUsesPackage());
        booking.setStatus(awaitingPayment ? BookingStatus.PENDIENTE : BookingStatus.CONFIRMADA);
        if (awaitingPayment) {
            booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(paymentWindowMinutes));
        }

        // 4. Calcular precios
        calculateAndSetPrices(booking, court, user, false);

        // 5. Manejar paquete si aplica
        if (Boolean.TRUE.equals(requestDTO.getUsesPackage())) {
            handlePackageUsage(booking, requestDTO.getUserPackageId());
        }

        // 6. Guardar
        booking = bookingRepository.save(booking);
        auditService.record("CREAR", "RESERVA", booking.getId(), "Reserva creada para cancha " + court.getId(), venueIdOf(court));
        notificationService.notify(user, NotificationType.RESERVA_CREADA,
                awaitingPayment ? "Reserva pendiente de pago" : "Reserva confirmada",
                awaitingPayment ? "Paga tu reserva en " + court.getName() + " dentro de " + paymentWindowMinutes + " minutos para confirmarla."
                        : "Tu reserva en " + court.getName() + " fue confirmada.");

        log.info("Booking created successfully with id: {}", booking.getId());

        return bookingMapper.toResponseDTO(booking);
    }

    @Override
    public RecurrentBookingResponseDTO createRecurrentBooking(RecurrentBookingRequestDTO requestDTO) {
        log.info("Creating recurrent booking for user: {} on court: {}",
                requestDTO.getUserId(), requestDTO.getCourtId());

        User user = getUserOrThrow(requestDTO.getUserId());
        Court court = getCourtForBookingOrThrow(requestDTO.getCourtId());

        List<RecurrentBookingResponseDTO.RecurrentBookingDetail> details = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;

        // Crear booking padre (primer reserva)
        LocalDate currentDate = requestDTO.getStartDate();
        Booking parentBooking = null;

        for (int week = 0; week < requestDTO.getNumberOfWeeks(); week++) {
            try {
                // Validar disponibilidad
                if (bookingRepository.existsOverlappingBooking(
                        requestDTO.getCourtId(),
                        currentDate,
                        requestDTO.getStartTime(),
                        requestDTO.getEndTime()
                ) || courtBlockRepository.existsOverlappingBlock(requestDTO.getCourtId(), currentDate,
                        requestDTO.getStartTime(), requestDTO.getEndTime())) {
                    // Cancha ocupada
                    details.add(RecurrentBookingResponseDTO.RecurrentBookingDetail.builder()
                            .bookingDate(currentDate)
                            .status("FAILED")
                            .reason("Cancha no disponible o bloqueada en ese horario")
                            .build());
                    failCount++;

                } else {
                    // Crear reserva
                    Booking booking = Booking.builder()
                            .user(user)
                            .court(court)
                            .bookingDate(currentDate)
                            .startTime(requestDTO.getStartTime())
                            .endTime(requestDTO.getEndTime())
                            .status(BookingStatus.CONFIRMADA)
                            .isRecurrent(true)
                            .recurrenceFrequency(requestDTO.getFrequency())
                            .build();

                    // Calcular precios (con descuento recurrente)
                    calculateAndSetPrices(booking, court, user, true);

                    // Establecer padre
                    if (week == 0) {
                        parentBooking = bookingRepository.save(booking);
                        booking.setParentBookingId(parentBooking.getId());
                    } else {
                        booking.setParentBookingId(parentBooking.getId());
                    }

                    // Manejar paquete
                    if (Boolean.TRUE.equals(requestDTO.getUsesPackage())) {
                        handlePackageUsage(booking, requestDTO.getUserPackageId());
                    }

                    booking = bookingRepository.save(booking);

                    details.add(RecurrentBookingResponseDTO.RecurrentBookingDetail.builder()
                            .bookingId(booking.getId())
                            .bookingDate(currentDate)
                            .status("CREATED")
                            .price(booking.getTotalPrice())
                            .build());

                    successCount++;
                    totalPrice = totalPrice.add(booking.getTotalPrice());
                }

            } catch (Exception e) {
                log.error("Error creating booking for date: {}", currentDate, e);
                details.add(RecurrentBookingResponseDTO.RecurrentBookingDetail.builder()
                        .bookingDate(currentDate)
                        .status("FAILED")
                        .reason("Error interno: " + e.getMessage())
                        .build());
                failCount++;
            }

            // Siguiente semana
            currentDate = currentDate.plusWeeks(1);
        }

        log.info("Recurrent booking completed: {} successful, {} failed", successCount, failCount);

        return RecurrentBookingResponseDTO.builder()
                .totalRequested(requestDTO.getNumberOfWeeks())
                .successfulBookings(successCount)
                .failedBookings(failCount)
                .bookingDetails(details)
                .estimatedTotalPrice(totalPrice)
                .recurrentDiscountApplied(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponseDTO getBookingById(Long id) {
        log.debug("Getting booking by id: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con id: " + id));

        return bookingMapper.toDetailResponseDTO(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByUser(Long userId) {
        log.debug("Getting bookings for user: {}", userId);

        List<Booking> bookings = bookingRepository.findByUserId(userId);

        return bookingMapper.toResponseDTOList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getAllBookings(Long venueId) {
        log.debug("Getting all bookings (admin), venue filter: {}", venueId);

        List<Booking> bookings = venueId == null ? bookingRepository.findAll() : bookingRepository.findByCourtVenueId(venueId);

        return bookingMapper.toResponseDTOList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByUserAndStatus(Long userId, BookingStatus status) {
        log.debug("Getting bookings for user: {} with status: {}", userId, status);

        List<Booking> bookings = bookingRepository.findByUserIdAndStatus(userId, status);

        return bookingMapper.toResponseDTOList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getFutureBookingsByUser(Long userId) {
        log.debug("Getting future bookings for user: {}", userId);

        List<Booking> bookings = bookingRepository.findFutureBookingsByUser(userId, LocalDate.now());

        return bookingMapper.toResponseDTOList(bookings);
    }

    // Continúa en siguiente parte...
    // ... continuación de BookingServiceImpl

    @Override
    @Transactional(readOnly = true)
    public CourtAvailabilityResponseDTO getCourtAvailability(Long courtId, LocalDate date) {
        log.debug("Getting availability for court: {} on date: {}", courtId, date);

        Court court = getCourtOrThrow(courtId);

        // Obtener reservas confirmadas del día
        List<Booking> confirmedBookings = bookingRepository
                .findConfirmedBookingsByCourtAndDate(courtId, date);

        // Generar slots ocupados
        List<CourtAvailabilityResponseDTO.TimeSlotDTO> occupiedSlots = new ArrayList<>();
        for (Booking booking : confirmedBookings) {
            occupiedSlots.add(CourtAvailabilityResponseDTO.TimeSlotDTO.builder()
                    .startTime(booking.getStartTime())
                    .endTime(booking.getEndTime())
                    .build());
        }

        // Generar slots disponibles (cada 1 hora desde 06:00 hasta 22:00)
        List<CourtAvailabilityResponseDTO.TimeSlotDTO> availableSlots = new ArrayList<>();
        LocalTime currentTime = OPERATION_START_TIME;

        while (currentTime.isBefore(OPERATION_END_TIME)) {
            LocalTime slotEnd = currentTime.plusHours(1);

            // Verificar si este slot está libre
            boolean isOccupied = false;
            for (Booking booking : confirmedBookings) {
                if (hasOverlap(currentTime, slotEnd, booking.getStartTime(), booking.getEndTime())) {
                    isOccupied = true;
                    break;
                }
            }

            if (!isOccupied) {
                BigDecimal estimatedPrice = estimatePrice(court, date, currentTime, slotEnd);
                BigDecimal priceFactor = calculatePriceFactor(date, currentTime);

                availableSlots.add(CourtAvailabilityResponseDTO.TimeSlotDTO.builder()
                        .startTime(currentTime)
                        .endTime(slotEnd)
                        .estimatedPrice(estimatedPrice)
                        .priceFactor(priceFactor)
                        .build());
            }

            currentTime = slotEnd;
        }

        return CourtAvailabilityResponseDTO.builder()
                .courtId(courtId)
                .courtName(court.getName())
                .date(date)
                .availableSlots(availableSlots)
                .occupiedSlots(occupiedSlots)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkOverlap(Long courtId, LocalDate date, String startTimeStr, String endTimeStr) {
        LocalTime startTime = LocalTime.parse(startTimeStr);
        LocalTime endTime = LocalTime.parse(endTimeStr);

        return bookingRepository.existsOverlappingBooking(courtId, date, startTime, endTime);
    }

    @Override
    public CancellationResponseDTO cancelBooking(CancellationRequestDTO requestDTO) {
        log.info("Cancelling booking id: {}", requestDTO.getBookingId());

        Booking booking = bookingRepository.findById(requestDTO.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        // Validar que esté confirmada (o pendiente de pago, que se cancela sin penalidad)
        boolean unpaid = booking.getStatus() == BookingStatus.PENDIENTE;
        if (booking.getStatus() != BookingStatus.CONFIRMADA && !unpaid) {
            throw new BusinessException("Solo se pueden cancelar reservas confirmadas");
        }

        // Calcular horas de anticipación
        LocalDateTime bookingDateTime = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        long hoursInAdvance = ChronoUnit.HOURS.between(LocalDateTime.now(), bookingDateTime);

        // Calcular penalización según RN-020 a RN-025
        BigDecimal penaltyPercentage = unpaid ? BigDecimal.ZERO : calculateCancellationPenalty(booking, hoursInAdvance);
        BigDecimal penaltyAmount = booking.getTotalPrice().multiply(penaltyPercentage);
        BigDecimal refundAmount = booking.getTotalPrice().subtract(penaltyAmount);

        // Actualizar booking
        booking.setStatus(BookingStatus.CANCELADA);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(requestDTO.getReason());
        booking.setPenaltyPercentage(penaltyPercentage);
        booking.setPenaltyAmount(penaltyAmount);

        bookingRepository.save(booking);
        auditService.record("CANCELAR", "RESERVA", booking.getId(), "Reserva cancelada", venueIdOf(booking.getCourt()));
        notificationService.notify(booking.getUser(), NotificationType.CANCELACION, "Reserva cancelada", "Tu reserva en " + booking.getCourt().getName() + " fue cancelada.");

        // Manejar paquete (devolver horas si no hay penalización tardía)
        Integer hoursRefunded = null;
        if (Boolean.TRUE.equals(booking.getUsesPackage()) && penaltyPercentage.compareTo(new BigDecimal("0.5")) < 0) {
            hoursRefunded = refundHoursToPackage(booking);
        }

        // Cancelar reservas recurrentes si se solicita
        Integer totalCancelled = 1;
        if (Boolean.TRUE.equals(requestDTO.getCancelAllRecurrent()) && booking.getParentBookingId() != null) {
            totalCancelled += cancelRecurrentBookings(booking.getParentBookingId());
        }

        log.info("Booking cancelled successfully. Penalty: {}%, Refund: {}",
                penaltyPercentage.multiply(new BigDecimal("100")), refundAmount);

        return CancellationResponseDTO.builder()
                .bookingId(booking.getId())
                .status("CANCELLED")
                .cancelledAt(LocalDateTime.now())
                .hoursInAdvance(hoursInAdvance)
                .penaltyPercentage(penaltyPercentage)
                .penaltyAmount(penaltyAmount)
                .refundAmount(refundAmount)
                .message(buildCancellationMessage(penaltyPercentage))
                .usedPackage(booking.getUsesPackage())
                .hoursRefunded(hoursRefunded)
                .wasRecurrent(booking.getIsRecurrent())
                .totalCancelled(totalCancelled)
                .build();
    }

    @Override
    public void markPastBookingsAsCompleted() {
        log.info("Marking past bookings as completed");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Booking> pastBookings = bookingRepository.findPastConfirmedBookings(today, now);

        for (Booking booking : pastBookings) {
            booking.setStatus(BookingStatus.COMPLETADA);
        }

        bookingRepository.saveAll(pastBookings);

        log.info("Marked {} bookings as completed", pastBookings.size());
    }

    // ========== MÉTODOS PRIVADOS DE VALIDACIÓN ==========

    private void validateBookingRules(BookingRequestDTO requestDTO, User user, Court court) {
        // RN-004: Mínimo 2 horas de anticipación
        LocalDateTime bookingDateTime = LocalDateTime.of(requestDTO.getBookingDate(), requestDTO.getStartTime());
        long hoursUntilBooking = ChronoUnit.HOURS.between(LocalDateTime.now(), bookingDateTime);

        if (hoursUntilBooking < MIN_ADVANCE_HOURS) {
            throw new ValidationException("Debe reservar con al menos " + MIN_ADVANCE_HOURS + " horas de anticipación");
        }

        // RN-002: Duración entre 1 y 4 horas
        long duration = ChronoUnit.HOURS.between(requestDTO.getStartTime(), requestDTO.getEndTime());
        if (duration < MIN_DURATION_HOURS || duration > MAX_DURATION_HOURS) {
            throw new ValidationException("La duración debe ser entre " + MIN_DURATION_HOURS +
                    " y " + MAX_DURATION_HOURS + " horas");
        }

        // RN-003: Horario de operación
        if (requestDTO.getStartTime().isBefore(OPERATION_START_TIME) ||
                requestDTO.getEndTime().isAfter(OPERATION_END_TIME)) {
            throw new ValidationException("El horario de operación es de " + OPERATION_START_TIME +
                    " a " + OPERATION_END_TIME);
        }

        // RN-005: Límite de días de anticipación según membresía
        long daysUntilBooking = ChronoUnit.DAYS.between(LocalDate.now(), requestDTO.getBookingDate());
        int maxDays = user.getMembershipType().getMaxDaysAdvance();

        if (daysUntilBooking > maxDays) {
            throw new ValidationException("Con su membresía " + user.getMembershipType() +
                    " solo puede reservar hasta " + maxDays + " días adelante");
        }

        // RN-001: Verificar solapamiento
        if (bookingRepository.existsOverlappingBooking(
                requestDTO.getCourtId(),
                requestDTO.getBookingDate(),
                requestDTO.getStartTime(),
                requestDTO.getEndTime()
        )) {
            throw new BusinessException("Ya existe una reserva en ese horario");
        }
        if (courtBlockRepository.existsOverlappingBlock(requestDTO.getCourtId(), requestDTO.getBookingDate(),
                requestDTO.getStartTime(), requestDTO.getEndTime())) {
            throw new BusinessException("La cancha está bloqueada por mantenimiento, feriado o evento");
        }
    }

    // ========== MÉTODOS PRIVADOS DE CÁLCULO DE PRECIOS ==========

    private void calculateAndSetPrices(Booking booking, Court court, User user, boolean isRecurrent) {
        BigDecimal basePrice = court.getPriceBaseHour();
        int hours = (int) ChronoUnit.HOURS.between(booking.getStartTime(), booking.getEndTime());
        BigDecimal baseTotal = basePrice.multiply(new BigDecimal(hours));

        booking.setBasePrice(baseTotal);

        // Aplicar factores dinámicos
        BigDecimal priceFactor = calculatePriceFactor(booking.getBookingDate(), booking.getStartTime());
        BigDecimal subtotal = baseTotal.multiply(priceFactor);
        BigDecimal surcharges = subtotal.subtract(baseTotal);

        booking.setDynamicSurcharges(surcharges);

        // Aplicar descuentos
        BigDecimal discount = BigDecimal.ZERO;

        // Descuento por membresía (RN-011 a RN-013)
        if (user.getMembershipType() != MembershipType.NINGUNA) {
            BigDecimal membershipDiscount = subtotal.multiply(
                    new BigDecimal(user.getMembershipType().getDiscountPercentage())
            );
            discount = discount.add(membershipDiscount);
        }

        // Descuento recurrente adicional (RN-029)
        if (isRecurrent) {
            BigDecimal recurrentDiscount = subtotal.multiply(new BigDecimal("0.05"));
            discount = discount.add(recurrentDiscount);
        }

        booking.setAppliedDiscount(discount);

        // Precio final
        BigDecimal totalPrice = subtotal.subtract(discount);
        booking.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculatePriceFactor(LocalDate date, LocalTime time) {
        BigDecimal factor = BigDecimal.ONE;

        // RN-007: Fin de semana (+30%)
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            factor = factor.multiply(new BigDecimal("1.3"));
        }

        // RN-006: Horario pico (+50%)
        if (!time.isBefore(PEAK_HOURS_START) && time.isBefore(PEAK_HOURS_END)) {
            factor = factor.multiply(new BigDecimal("1.5"));
        }

        // RN-009: Horario valle (-20%)
        if (!time.isBefore(VALLEY_HOURS_START) && time.isBefore(VALLEY_HOURS_END)) {
            factor = factor.multiply(new BigDecimal("0.8"));
        }

        return factor;
    }

    private BigDecimal estimatePrice(Court court, LocalDate date, LocalTime startTime, LocalTime endTime) {
        int hours = (int) ChronoUnit.HOURS.between(startTime, endTime);
        BigDecimal basePrice = court.getPriceBaseHour().multiply(new BigDecimal(hours));
        BigDecimal factor = calculatePriceFactor(date, startTime);

        return basePrice.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== MÉTODOS PRIVADOS DE CANCELACIÓN ==========

    private BigDecimal calculateCancellationPenalty(Booking booking, long hoursInAdvance) {
        // RN-025: VIP tiene cancelación gratuita hasta 12 horas antes
        if (booking.getUser().getMembershipType() == MembershipType.VIP && hoursInAdvance >= 12) {
            return BigDecimal.ZERO;
        }

        // RN-020: Más de 24 horas = sin penalización
        if (hoursInAdvance >= 24) {
            return BigDecimal.ZERO;
        }

        // RN-021: Entre 12-24 horas = 30%
        if (hoursInAdvance >= 12) {
            return new BigDecimal("0.30");
        }

        // RN-022: Menos de 12 horas = 50%
        return new BigDecimal("0.50");
    }

    private String buildCancellationMessage(BigDecimal penaltyPercentage) {
        if (penaltyPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return "Cancelación exitosa sin penalización";
        } else {
            int percentage = penaltyPercentage.multiply(new BigDecimal("100")).intValue();
            return "Cancelación procesada con penalización del " + percentage + "%";
        }
    }

    private int cancelRecurrentBookings(Long parentBookingId) {
        List<Booking> recurrentBookings = bookingRepository.findByParentBookingId(parentBookingId);
        int count = 0;

        for (Booking booking : recurrentBookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMADA) {
                booking.setStatus(BookingStatus.CANCELADA);
                booking.setCancelledAt(LocalDateTime.now());
                count++;
            }
        }

        bookingRepository.saveAll(recurrentBookings);
        return count;
    }

    // ========== MÉTODOS PRIVADOS DE PAQUETES ==========

    private void handlePackageUsage(Booking booking, Long userPackageId) {
        if (userPackageId == null) {
            throw new ValidationException("Debe especificar el ID del paquete a usar");
        }

        UserPackage userPackage = userPackageRepository.findById(userPackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado"));

        if (!userPackage.getUser().getId().equals(booking.getUser().getId())) {
            throw new BusinessException("El paquete no pertenece al usuario de la reserva");
        }

        int hours = (int) ChronoUnit.HOURS.between(booking.getStartTime(), booking.getEndTime());

        if (!userPackage.deductHours(hours)) {
            throw new BusinessException("El paquete no tiene suficientes horas o está vencido");
        }

        userPackageRepository.save(userPackage);

        // Al usar paquete, el precio es 0
        booking.setUsesPackage(true);
        booking.setUserPackageId(userPackageId);
        booking.setHoursDeducted(new BigDecimal(hours));
        booking.setTotalPrice(BigDecimal.ZERO);
        booking.setAppliedDiscount(BigDecimal.ZERO);
        booking.setDynamicSurcharges(BigDecimal.ZERO);
    }

    private Integer refundHoursToPackage(Booking booking) {
        if (booking.getUserPackageId() == null) {
            return null;
        }

        UserPackage userPackage = userPackageRepository.findById(booking.getUserPackageId())
                .orElse(null);

        if (userPackage != null && !userPackage.isExpired()) {
            int hoursToRefund = booking.getHoursDeducted().intValue();
            userPackage.setRemainingHours(userPackage.getRemainingHours() + hoursToRefund);
            userPackage.setActive(true);
            userPackageRepository.save(userPackage);
            return hoursToRefund;
        }

        return null;
    }

    // ========== MÉTODOS AUXILIARES ==========

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));
    }

    private Court getCourtOrThrow(Long courtId) {
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada con id: " + courtId));
    }

    private void validateReschedule(Booking booking, RescheduleBookingRequestDTO requestDTO) {
        LocalDateTime bookingDateTime = LocalDateTime.of(requestDTO.getBookingDate(), requestDTO.getStartTime());
        if (ChronoUnit.HOURS.between(LocalDateTime.now(), bookingDateTime) < MIN_ADVANCE_HOURS) {
            throw new ValidationException("Debe reprogramar con al menos " + MIN_ADVANCE_HOURS + " horas de anticipación");
        }
        long duration = ChronoUnit.HOURS.between(requestDTO.getStartTime(), requestDTO.getEndTime());
        if (duration < MIN_DURATION_HOURS || duration > MAX_DURATION_HOURS ||
                requestDTO.getStartTime().isBefore(OPERATION_START_TIME) ||
                requestDTO.getEndTime().isAfter(OPERATION_END_TIME)) {
            throw new ValidationException("El nuevo horario no cumple las reglas de duración u operación");
        }
        long daysUntilBooking = ChronoUnit.DAYS.between(LocalDate.now(), requestDTO.getBookingDate());
        if (daysUntilBooking > booking.getUser().getMembershipType().getMaxDaysAdvance()) {
            throw new ValidationException("La nueva fecha supera el límite de anticipación de su membresía");
        }
        if (bookingRepository.existsOverlappingBookingExcludingId((long) booking.getCourt().getId(),
                requestDTO.getBookingDate(), requestDTO.getStartTime(), requestDTO.getEndTime(), booking.getId())) {
            throw new BusinessException("Ya existe una reserva en el nuevo horario");
        }
        if (courtBlockRepository.existsOverlappingBlock((long) booking.getCourt().getId(), requestDTO.getBookingDate(),
                requestDTO.getStartTime(), requestDTO.getEndTime())) {
            throw new BusinessException("La cancha está bloqueada en el nuevo horario");
        }
    }

    @Override
    public BookingResponseDTO rescheduleBooking(Long id, RescheduleBookingRequestDTO requestDTO) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        // Serializa con las reservas nuevas de la misma cancha para evitar solapamientos concurrentes.
        getCourtForBookingOrThrow((long) booking.getCourt().getId());
        if (booking.getStatus() != BookingStatus.CONFIRMADA) {
            throw new BusinessException("Solo se pueden reprogramar reservas confirmadas");
        }
        if (Boolean.TRUE.equals(booking.getUsesPackage()) &&
                !Duration.between(requestDTO.getStartTime(), requestDTO.getEndTime())
                        .equals(Duration.between(booking.getStartTime(), booking.getEndTime()))) {
            throw new ValidationException("Una reserva pagada con paquete solo puede reprogramarse con la misma duración");
        }
        validateReschedule(booking, requestDTO);
        booking.setBookingDate(requestDTO.getBookingDate());
        booking.setStartTime(requestDTO.getStartTime());
        booking.setEndTime(requestDTO.getEndTime());
        calculateAndSetPrices(booking, booking.getCourt(), booking.getUser(), Boolean.TRUE.equals(booking.getIsRecurrent()));
        if (Boolean.TRUE.equals(booking.getUsesPackage())) {
            booking.setTotalPrice(BigDecimal.ZERO);
        }
        Booking savedBooking = bookingRepository.save(booking);
        auditService.record("REPROGRAMAR", "RESERVA", savedBooking.getId(), "Reserva reprogramada", venueIdOf(savedBooking.getCourt()));
        notificationService.notify(savedBooking.getUser(), NotificationType.REPROGRAMACION, "Reserva reprogramada", "Tu reserva en " + savedBooking.getCourt().getName() + " fue actualizada.");
        return bookingMapper.toResponseDTO(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckInCodeResponseDTO getCheckInCode(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        return CheckInCodeResponseDTO.builder().bookingId(id).code(booking.getCheckInCode()).build();
    }

    @Override
    public void checkIn(Long id, CheckInRequestDTO requestDTO) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        if (booking.getStatus() != BookingStatus.CONFIRMADA || booking.getCheckedInAt() != null) {
            throw new BusinessException("La reserva no está disponible para check-in");
        }
        if (!booking.getCheckInCode().equals(requestDTO.getCode())) {
            throw new ValidationException("Código de check-in inválido");
        }
        booking.setCheckedInAt(LocalDateTime.now());
        bookingRepository.save(booking);
        auditService.record("CHECK_IN", "RESERVA", booking.getId(), "Check-in registrado", venueIdOf(booking.getCourt()));
        notificationService.notify(booking.getUser(), NotificationType.CHECK_IN, "Check-in registrado", "Tu ingreso a " + booking.getCourt().getName() + " fue registrado.");
    }

    @Override
    public void markNoShow(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        if (booking.getStatus() != BookingStatus.CONFIRMADA || booking.getCheckedInAt() != null) {
            throw new BusinessException("Solo una reserva confirmada sin check-in puede marcarse como no-show");
        }
        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setPenaltyPercentage(BigDecimal.ONE);
        booking.setPenaltyAmount(booking.getTotalPrice());
        bookingRepository.save(booking);
        auditService.record("NO_SHOW", "RESERVA", booking.getId(), "Reserva marcada como no-show", venueIdOf(booking.getCourt()));
    }

    @Override
    @Transactional(readOnly = true)
    public Long findBookingIdByCheckInCode(String code) {
        return bookingRepository.findByCheckInCode(code == null ? "" : code.trim())
                .map(Booking::getId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una reserva con ese código de check-in"));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getVenueIdOfBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        return venueIdOf(booking.getCourt());
    }

    @Override
    @Transactional(readOnly = true)
    public org.salva.task.court_reservation_system.dto.response.PageResponseDTO<BookingResponseDTO> searchBookings(
            Long venueId, BookingStatus status, String text, int page, int size) {
        org.springframework.data.jpa.domain.Specification<Booking> spec = org.springframework.data.jpa.domain.Specification
                .where(org.salva.task.court_reservation_system.repository.spec.BookingSpecifications.inVenue(venueId))
                .and(org.salva.task.court_reservation_system.repository.spec.BookingSpecifications.hasStatus(status))
                .and(org.salva.task.court_reservation_system.repository.spec.BookingSpecifications.matches(text));
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("bookingDate"), org.springframework.data.domain.Sort.Order.asc("startTime"));
        return org.salva.task.court_reservation_system.dto.response.PageResponseDTO.of(
                bookingRepository.findAll(spec, org.salva.task.court_reservation_system.dto.response.PageResponseDTO.pageable(page, size, sort))
                        .map(bookingMapper::toResponseDTO));
    }

    @Override
    public int cancelExpiredUnpaidBookings() {
        List<Booking> expired = bookingRepository.findByStatusAndPaymentDeadlineBefore(BookingStatus.PENDIENTE, LocalDateTime.now());
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.CANCELADA);
            booking.setCancelledAt(LocalDateTime.now());
            booking.setCancellationReason("Pago no realizado dentro del plazo");
            bookingRepository.save(booking);
            auditService.record("EXPIRAR", "RESERVA", booking.getId(), "Reserva cancelada por falta de pago", venueIdOf(booking.getCourt()));
            notificationService.notify(booking.getUser(), NotificationType.CANCELACION, "Reserva cancelada por falta de pago",
                    "Tu reserva en " + booking.getCourt().getName() + " se canceló porque el pago no se realizó a tiempo.");
        }
        if (!expired.isEmpty()) {
            log.info("Cancelled {} unpaid bookings past their payment deadline", expired.size());
        }
        return expired.size();
    }

    private Long venueIdOf(Court court) {
        return court.getVenue() != null ? court.getVenue().getId() : null;
    }

    private Court getCourtForBookingOrThrow(Long courtId) {
        return courtRepository.findByIdForBooking(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada con id: " + courtId));
    }

    private boolean hasOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}
