package org.salva.task.court_reservation_system.service;

import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.CancellationRequestDTO;
import org.salva.task.court_reservation_system.dto.request.RecurrentBookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.RescheduleBookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.CheckInRequestDTO;
import org.salva.task.court_reservation_system.dto.response.*;
import org.salva.task.court_reservation_system.enums.BookingStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface para servicios de Booking (Reserva)
 */
public interface BookingService {

    /**
     * Crea una reserva simple
     */
    BookingResponseDTO createBooking(BookingRequestDTO requestDTO);

    /**
     * Crea reservas recurrentes (semanales)
     */
    RecurrentBookingResponseDTO createRecurrentBooking(RecurrentBookingRequestDTO requestDTO);

    /**
     * Obtiene una reserva por ID
     */
    BookingDetailResponseDTO getBookingById(Long id);

    /**
     * Obtiene todas las reservas de un usuario
     */
    List<BookingResponseDTO> getBookingsByUser(Long userId);

    /**
     * Obtiene todas las reservas del sistema (solo admin)
     */
    List<BookingResponseDTO> getAllBookings(Long venueId);

    /**
     * Obtiene reservas de un usuario filtradas por estado
     */
    List<BookingResponseDTO> getBookingsByUserAndStatus(Long userId, BookingStatus status);

    /**
     * Obtiene reservas futuras de un usuario
     */
    List<BookingResponseDTO> getFutureBookingsByUser(Long userId);

    /**
     * Obtiene disponibilidad de una cancha en una fecha
     */
    CourtAvailabilityResponseDTO getCourtAvailability(Long courtId, LocalDate date);

    /**
     * Verifica si existe solapamiento de horarios
     */
    boolean checkOverlap(Long courtId, LocalDate date, String startTime, String endTime);

    /**
     * Cancela una reserva
     */
    CancellationResponseDTO cancelBooking(CancellationRequestDTO requestDTO);

    BookingResponseDTO rescheduleBooking(Long id, RescheduleBookingRequestDTO requestDTO);
    CheckInCodeResponseDTO getCheckInCode(Long id);
    void checkIn(Long id, CheckInRequestDTO requestDTO);
    void markNoShow(Long id);

    /**
     * Marca reservas pasadas como completadas (job automático)
     */
    void markPastBookingsAsCompleted();

    /**
     * Devuelve el id de la sede a la que pertenece la cancha de la reserva (null si la cancha no tiene sede)
     */
    Long getVenueIdOfBooking(Long id);
}
