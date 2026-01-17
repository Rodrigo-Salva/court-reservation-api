package org.salva.task.court_reservation_system.controller;

import org.salva.task.court_reservation_system.dto.request.PackageRequestDTO;
import org.salva.task.court_reservation_system.dto.response.PackageResponseDTO;
import org.salva.task.court_reservation_system.service.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestión de paquetes
 */
@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Tag(name = "Packages", description = "API para gestión de paquetes de horas")
public class PackageController {

    private final PackageService packageService;

    @PostMapping
    @Operation(summary = "Crear un nuevo paquete")
    public ResponseEntity<PackageResponseDTO> createPackage(@Valid @RequestBody PackageRequestDTO requestDTO) {
        PackageResponseDTO response = packageService.createPackage(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener paquete por ID")
    public ResponseEntity<PackageResponseDTO> getPackageById(@PathVariable Long id) {
        PackageResponseDTO response = packageService.getPackageById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar todos los paquetes activos")
    public ResponseEntity<List<PackageResponseDTO>> getAllActivePackages() {
        List<PackageResponseDTO> response = packageService.getAllActivePackages();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/best-discount")
    @Operation(summary = "Obtener paquetes ordenados por mejor descuento")
    public ResponseEntity<List<PackageResponseDTO>> getPackagesByBestDiscount() {
        List<PackageResponseDTO> response = packageService.getPackagesOrderedByBestDiscount();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/best-price")
    @Operation(summary = "Obtener paquetes ordenados por mejor precio/hora")
    public ResponseEntity<List<PackageResponseDTO>> getPackagesByBestPrice() {
        List<PackageResponseDTO> response = packageService.getPackagesOrderedByBestPrice();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar paquete")
    public ResponseEntity<PackageResponseDTO> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody PackageRequestDTO requestDTO
    ) {
        PackageResponseDTO response = packageService.updatePackage(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar paquete")
    public ResponseEntity<Void> deactivatePackage(@PathVariable Long id) {
        packageService.deactivatePackage(id);
        return ResponseEntity.noContent().build();
    }
}
