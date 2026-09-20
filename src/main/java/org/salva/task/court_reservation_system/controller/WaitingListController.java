package org.salva.task.court_reservation_system.controller;

import org.salva.task.court_reservation_system.dto.request.WaitingListRequestDTO;
import org.salva.task.court_reservation_system.dto.response.WaitingListResponseDTO;
import org.salva.task.court_reservation_system.service.WaitingListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Controller para gestión de lista de espera
 */
@RestController
@RequestMapping("/api/waiting-list")
@RequiredArgsConstructor
@Tag(name = "Waiting List", description = "API para lista de espera de canchas")
public class WaitingListController {

    private final WaitingListService waitingListService;
    private final AccessControlService accessControl;

    @PostMapping
    @Operation(summary = "Agregar a lista de espera", description = "Agrega un usuario a la lista de espera para un horario específico")
    public ResponseEntity<WaitingListResponseDTO> addToWaitingList(
            @Valid @RequestBody WaitingListRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        requestDTO.setUserId(userDetails.getId());
        WaitingListResponseDTO response = waitingListService.addToWaitingList(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener solicitud por ID")
    public ResponseEntity<WaitingListResponseDTO> getWaitingListById(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        WaitingListResponseDTO response = waitingListService.getWaitingListById(id);
        accessControl.requireOwnerOrAdmin(response.getUserId(), userDetails);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener todas las solicitudes de un usuario")
    public ResponseEntity<List<WaitingListResponseDTO>> getWaitingListByUser(@PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireOwnerOrAdmin(userId, userDetails);
        List<WaitingListResponseDTO> response = waitingListService.getWaitingListByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/court/{courtId}/pending")
    @Operation(summary = "Obtener usuarios esperando por cancha y horario")
    public ResponseEntity<List<WaitingListResponseDTO>> getPendingByCourtAndTimeSlot(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        List<WaitingListResponseDTO> response = waitingListService.getPendingByCourtAndTimeSlot(
                courtId, date, startTime, endTime
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar de lista de espera")
    public ResponseEntity<Void> removeFromWaitingList(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireOwnerOrAdmin(waitingListService.getWaitingListById(id).getUserId(), userDetails);
        waitingListService.removeFromWaitingList(id);
        return ResponseEntity.noContent().build();
    }
}
