package org.salva.task.court_reservation_system.controller;

import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.CancellationRequestDTO;
import org.salva.task.court_reservation_system.dto.request.RecurrentBookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.RescheduleBookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.CheckInRequestDTO;
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
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Controller para gestión de reservas
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "API para gestión de reservas de canchas")
public class BookingController {

    private final BookingService bookingService;
    private final AccessControlService accessControl;

    @PostMapping
    @Operation(summary = "Crear una reserva simple", description = "Crea una nueva reserva validando disponibilidad y calculando precios")
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        requestDTO.setUserId(userDetails.getId());
        BookingResponseDTO response = bookingService.createBooking(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/recurrent")
    @Operation(summary = "Crear reservas recurrentes", description = "Crea múltiples reservas semanales automáticamente")
    public ResponseEntity<RecurrentBookingResponseDTO> createRecurrentBooking(
            @Valid @RequestBody RecurrentBookingRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        requestDTO.setUserId(userDetails.getId());
        RecurrentBookingResponseDTO response = bookingService.createRecurrentBooking(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reserva por ID", description = "Obtiene los detalles completos de una reserva")
    public ResponseEntity<BookingDetailResponseDTO> getBookingById(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BookingDetailResponseDTO response = bookingService.getBookingById(id);
        accessControl.requireOwnerOrAdmin(response.getUserId(), userDetails);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar reservas paginadas (personal)", description = "Filtra por estado y texto; el personal de sede solo ve su sede")
    public ResponseEntity<org.salva.task.court_reservation_system.dto.response.PageResponseDTO<BookingResponseDTO>> searchBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(bookingService.searchBookings(accessControl.resolveVenueFilter(userDetails), status, q, page, size));
    }

    @GetMapping
    @Operation(summary = "Listar todas las reservas (admin)")
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<BookingResponseDTO> response = bookingService.getAllBookings(accessControl.resolveVenueFilter(userDetails));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener todas las reservas de un usuario")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUser(@PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireOwnerOrAdmin(userId, userDetails);
        List<BookingResponseDTO> response = bookingService.getBookingsByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/status/{status}")
    @Operation(summary = "Obtener reservas de un usuario por estado")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable BookingStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        accessControl.requireOwnerOrAdmin(userId, userDetails);
        List<BookingResponseDTO> response = bookingService.getBookingsByUserAndStatus(userId, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/future")
    @Operation(summary = "Obtener reservas futuras de un usuario")
    public ResponseEntity<List<BookingResponseDTO>> getFutureBookingsByUser(@PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireOwnerOrAdmin(userId, userDetails);
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
            @Valid @RequestBody CancellationRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        accessControl.requireOwnerOrAdmin(bookingService.getBookingById(requestDTO.getBookingId()).getUserId(), userDetails);
        CancellationResponseDTO response = bookingService.cancelBooking(requestDTO);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reschedule")
    @Operation(summary = "Reprogramar una reserva")
    public ResponseEntity<BookingResponseDTO> rescheduleBooking(@PathVariable Long id,
            @Valid @RequestBody RescheduleBookingRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireOwnerOrAdmin(bookingService.getBookingById(id).getUserId(), userDetails);
        return ResponseEntity.ok(bookingService.rescheduleBooking(id, requestDTO));
    }

    @GetMapping("/{id}/check-in-code")
    @Operation(summary = "Obtener código QR de check-in")
    public ResponseEntity<CheckInCodeResponseDTO> getCheckInCode(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireOwnerOrAdmin(bookingService.getBookingById(id).getUserId(), userDetails);
        return ResponseEntity.ok(bookingService.getCheckInCode(id));
    }

    @PutMapping("/{id}/check-in")
    @Operation(summary = "Registrar check-in mediante código QR")
    public ResponseEntity<Void> checkIn(@PathVariable Long id, @Valid @RequestBody CheckInRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireSameVenueOrAdmin(bookingService.getVenueIdOfBooking(id), userDetails);
        bookingService.checkIn(id, requestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/check-in/scan")
    @Operation(summary = "Registrar check-in solo con el código del QR", description = "Usado por el lector de cámara de Recepción: resuelve la reserva a partir del código")
    public ResponseEntity<BookingDetailResponseDTO> checkInByCode(@Valid @RequestBody CheckInRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long bookingId = bookingService.findBookingIdByCheckInCode(requestDTO.getCode());
        accessControl.requireSameVenueOrAdmin(bookingService.getVenueIdOfBooking(bookingId), userDetails);
        CheckInRequestDTO trimmed = new CheckInRequestDTO();
        trimmed.setCode(requestDTO.getCode().trim());
        bookingService.checkIn(bookingId, trimmed);
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    @PatchMapping("/{id}/no-show")
    @Operation(summary = "Marcar reserva como no-show")
    public ResponseEntity<Void> markNoShow(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireSameVenueOrAdmin(bookingService.getVenueIdOfBooking(id), userDetails);
        bookingService.markNoShow(id);
        return ResponseEntity.noContent().build();
    }
}
