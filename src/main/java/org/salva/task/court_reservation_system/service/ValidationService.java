package org.salva.task.court_reservation_system.service;

import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.User;

/**
 * Interface para servicios de validación centralizados
 */
public interface ValidationService {

    /**
     * Valida todas las reglas de negocio para una reserva
     */
    void validateBookingRequest(BookingRequestDTO requestDTO, User user, Court court);

    /**
     * Valida horarios de operación
     */
    void validateOperatingHours(BookingRequestDTO requestDTO);

    /**
     * Valida duración de la reserva
     */
    void validateDuration(BookingRequestDTO requestDTO);

    /**
     * Valida anticipación mínima
     */
    void validateMinimumAdvance(BookingRequestDTO requestDTO);

    /**
     * Valida límite de días según membresía
     */
    void validateMaxDaysAdvance(BookingRequestDTO requestDTO, User user);

    /**
     * Valida que no haya solapamiento
     */
    void validateNoOverlap(BookingRequestDTO requestDTO);

    /**
     * Valida que la cancha esté activa
     */
    void validateCourtActive(Court court);

    /**
     * Valida que el usuario esté activo
     */
    void validateUserActive(User user);
}
