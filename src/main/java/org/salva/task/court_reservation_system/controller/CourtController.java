package org.salva.task.court_reservation_system.controller;

import org.salva.task.court_reservation_system.dto.request.CourtRequestDTO;
import org.salva.task.court_reservation_system.dto.response.CourtResponseDTO;
import org.salva.task.court_reservation_system.enums.SportType;
import org.salva.task.court_reservation_system.service.CourtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller para gestión de canchas
 */
@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
@Tag(name = "Courts", description = "API para gestión de canchas deportivas")
public class CourtController {

    private final CourtService courtService;

    @PostMapping
    @Operation(summary = "Crear una nueva cancha", description = "Crea una nueva cancha en el sistema")
    public ResponseEntity<CourtResponseDTO> createCourt(@Valid @RequestBody CourtRequestDTO requestDTO) {
        CourtResponseDTO response = courtService.createCourt(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cancha por ID", description = "Obtiene los detalles de una cancha específica")
    public ResponseEntity<CourtResponseDTO> getCourtById(@PathVariable Long id) {
        CourtResponseDTO response = courtService.getCourtById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar todas las canchas activas", description = "Obtiene todas las canchas activas del sistema")
    public ResponseEntity<List<CourtResponseDTO>> getAllActiveCourts() {
        List<CourtResponseDTO> response = courtService.getAllActiveCourts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sport-type/{sportType}")
    @Operation(summary = "Obtener canchas por tipo de deporte")
    public ResponseEntity<List<CourtResponseDTO>> getCourtsBySportType(@PathVariable SportType sportType) {
        List<CourtResponseDTO> response = courtService.getCourtsBySportType(sportType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar canchas por nombre")
    public ResponseEntity<List<CourtResponseDTO>> searchCourtsByName(@RequestParam String name) {
        List<CourtResponseDTO> response = courtService.searchCourtsByName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/price-range")
    @Operation(summary = "Obtener canchas por rango de precio")
    public ResponseEntity<List<CourtResponseDTO>> getCourtsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice
    ) {
        List<CourtResponseDTO> response = courtService.getCourtsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cancha", description = "Actualiza los datos de una cancha existente")
    public ResponseEntity<CourtResponseDTO> updateCourt(
            @PathVariable Long id,
            @Valid @RequestBody CourtRequestDTO requestDTO
    ) {
        CourtResponseDTO response = courtService.updateCourt(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar cancha", description = "Desactiva una cancha (soft delete)")
    public ResponseEntity<Void> deactivateCourt(@PathVariable Long id) {
        courtService.deactivateCourt(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar cancha", description = "Reactiva una cancha previamente desactivada")
    public ResponseEntity<Void> activateCourt(@PathVariable Long id) {
        courtService.activateCourt(id);
        return ResponseEntity.ok().build();
    }
}
