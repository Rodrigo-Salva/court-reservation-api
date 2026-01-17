package org.salva.task.court_reservation_system.controller;

import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.CancellationRequestDTO;
import org.salva.task.court_reservation_system.dto.request.RecurrentBookingRequestDTO;
import org.salva.task.court_reservation_system.dto.response.*;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller para gestión de reservas
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "API para gestión de reservas de canchas")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Crear una reserva simple", description = "Crea una nueva reserva validando disponibilidad y calculando precios")
    public ResponseEntity<BookingResponseDTO> createBooking(@Valid @RequestBody BookingRequestDTO requestDTO) {
        BookingResponseDTO response = bookingService.createBooking(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/recurrent")
    @Operation(summary = "Crear reservas recurrentes", description = "Crea múltiples reservas semanales automáticamente")
    public ResponseEntity<RecurrentBookingResponseDTO> createRecurrentBooking(
            @Valid @RequestBody RecurrentBookingRequestDTO requestDTO
    ) {
        RecurrentBookingResponseDTO response = bookingService.createRecurrentBooking(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reserva por ID", description = "Obtiene los detalles completos de una reserva")
    public ResponseEntity<BookingDetailResponseDTO> getBookingById(@PathVariable Long id) {
        BookingDetailResponseDTO response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener todas las reservas de un usuario")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUser(@PathVariable Long userId) {
        List<BookingResponseDTO> response = bookingService.getBookingsByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/status/{status}")
    @Operation(summary = "Obtener reservas de un usuario por estado")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable BookingStatus status
    ) {
        List<BookingResponseDTO> response = bookingService.getBookingsByUserAndStatus(userId, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/future")
    @Operation(summary = "Obtener reservas futuras de un usuario")
    public ResponseEntity<List<BookingResponseDTO>> getFutureBookingsByUser(@PathVariable Long userId) {
        List<BookingResponseDTO> response = bookingService.getFutureBookingsByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/court/{courtId}/availability")
    @Operation(summary = "Obtener disponibilidad de una cancha", description = "Muestra horarios disponibles y ocupados para una fecha específica")
    public ResponseEntity<CourtAvailabilityResponseDTO> getCourtAvailability(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        CourtAvailabilityResponseDTO response = bookingService.getCourtAvailability(courtId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-overlap")
    @Operation(summary = "Verificar solapamiento de horarios")
    public ResponseEntity<Boolean> checkOverlap(
            @RequestParam Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        boolean hasOverlap = bookingService.checkOverlap(courtId, date, startTime, endTime);
        return ResponseEntity.ok(hasOverlap);
    }

    @PutMapping("/cancel")
    @Operation(summary = "Cancelar una reserva", description = "Cancela una reserva aplicando penalizaciones según anticipación")
    public ResponseEntity<CancellationResponseDTO> cancelBooking(
            @Valid @RequestBody CancellationRequestDTO requestDTO
    ) {
        CancellationResponseDTO response = bookingService.cancelBooking(requestDTO);
        return ResponseEntity.ok(response);
    }
}
