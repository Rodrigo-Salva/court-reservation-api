package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.exception.BusinessException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.repository.BookingRepository;
import org.salva.task.court_reservation_system.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Implementación del servicio de validaciones
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ValidationServiceImpl implements ValidationService {

    private final BookingRepository bookingRepository;

    // Constantes de configuración
    private static final int MIN_ADVANCE_HOURS = 2;
    private static final int MIN_DURATION_HOURS = 1;
    private static final int MAX_DURATION_HOURS = 4;
    private static final LocalTime OPERATION_START_TIME = LocalTime.of(6, 0);
    private static final LocalTime OPERATION_END_TIME = LocalTime.of(23, 0);

    @Override
    public void validateBookingRequest(BookingRequestDTO requestDTO, User user, Court court) {
        log.debug("Validating booking request for user: {} on court: {}", user.getId(), court.getId());

        validateUserActive(user);
        validateCourtActive(court);
        validateOperatingHours(requestDTO);
        validateDuration(requestDTO);
        validateMinimumAdvance(requestDTO);
        validateMaxDaysAdvance(requestDTO, user);
        validateNoOverlap(requestDTO);

        log.debug("All validations passed for booking request");
    }

    @Override
    public void validateOperatingHours(BookingRequestDTO requestDTO) {
        // RN-003: Horario de operación 06:00 - 23:00
        if (requestDTO.getStartTime().isBefore(OPERATION_START_TIME)) {
            throw new ValidationException(
                    "El horario de inicio debe ser después de las " + OPERATION_START_TIME
            );
        }

        if (requestDTO.getEndTime().isAfter(OPERATION_END_TIME)) {
            throw new ValidationException(
                    "El horario de fin debe ser antes de las " + OPERATION_END_TIME
            );
        }

        if (requestDTO.getStartTime().isAfter(requestDTO.getEndTime()) ||
                requestDTO.getStartTime().equals(requestDTO.getEndTime())) {
            throw new ValidationException(
                    "El horario de inicio debe ser anterior al horario de fin"
            );
        }
    }

    @Override
    public void validateDuration(BookingRequestDTO requestDTO) {
        // RN-002: Duración entre 1 y 4 horas
        long durationHours = ChronoUnit.HOURS.between(
                requestDTO.getStartTime(),
                requestDTO.getEndTime()
        );

        if (durationHours < MIN_DURATION_HOURS) {
            throw new ValidationException(
                    "La duración mínima de una reserva es de " + MIN_DURATION_HOURS + " hora(s)"
            );
        }

        if (durationHours > MAX_DURATION_HOURS) {
            throw new ValidationException(
                    "La duración máxima de una reserva es de " + MAX_DURATION_HOURS + " horas"
            );
        }
    }

    @Override
    public void validateMinimumAdvance(BookingRequestDTO requestDTO) {
        // RN-004: Mínimo 2 horas de anticipación
        LocalDateTime bookingDateTime = LocalDateTime.of(
                requestDTO.getBookingDate(),
                requestDTO.getStartTime()
        );

        long hoursUntilBooking = ChronoUnit.HOURS.between(LocalDateTime.now(), bookingDateTime);

        if (hoursUntilBooking < MIN_ADVANCE_HOURS) {
            throw new ValidationException(
                    "Debe reservar con al menos " + MIN_ADVANCE_HOURS + " horas de anticipación. " +
                            "Horas restantes: " + hoursUntilBooking
            );
        }
    }

    @Override
    public void validateMaxDaysAdvance(BookingRequestDTO requestDTO, User user) {
        // RN-005: Límite de días según membresía
        long daysUntilBooking = ChronoUnit.DAYS.between(
                LocalDate.now(),
                requestDTO.getBookingDate()
        );

        int maxDays = user.getMembershipType().getMaxDaysAdvance();

        if (daysUntilBooking > maxDays) {
            throw new ValidationException(
                    "Con su tipo de membresía (" + user.getMembershipType() + ") " +
                            "solo puede reservar hasta " + maxDays + " días de anticipación. " +
                            "Está intentando reservar con " + daysUntilBooking + " días de anticipación."
            );
        }
    }

    @Override
    public void validateNoOverlap(BookingRequestDTO requestDTO) {
        // RN-001: No permitir solapamiento de horarios
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                requestDTO.getCourtId(),
                requestDTO.getBookingDate(),
                requestDTO.getStartTime(),
                requestDTO.getEndTime()
        );

        if (hasOverlap) {
            throw new BusinessException(
                    "Ya existe una reserva confirmada que se solapa con el horario solicitado. " +
                            "Por favor elija otro horario."
            );
        }
    }

    @Override
    public void validateCourtActive(Court court) {
        if (!court.getActive()) {
            throw new BusinessException(
                    "La cancha '" + court.getName() + "' no está disponible en este momento"
            );
        }
    }

    @Override
    public void validateUserActive(User user) {
        if (!user.getActive()) {
            throw new BusinessException(
                    "El usuario no está activo. Por favor contacte al administrador."
            );
        }
    }
}