package org.salva.task.court_reservation_system.controller;

import org.salva.task.court_reservation_system.dto.request.PackagePurchaseRequestDTO;
import org.salva.task.court_reservation_system.dto.response.UserPackageResponseDTO;
import org.salva.task.court_reservation_system.service.UserPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestión de paquetes de usuario
 */
@RestController
@RequestMapping("/api/user-packages")
@RequiredArgsConstructor
@Tag(name = "User Packages", description = "API para compra y gestión de paquetes de usuario")
public class UserPackageController {

    private final UserPackageService userPackageService;

    @PostMapping("/purchase")
    @Operation(summary = "Comprar un paquete", description = "Usuario compra un paquete de horas prepagadas")
    public ResponseEntity<UserPackageResponseDTO> purchasePackage(
            @Valid @RequestBody PackagePurchaseRequestDTO requestDTO
    ) {
        UserPackageResponseDTO response = userPackageService.purchasePackage(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener paquete de usuario por ID")
    public ResponseEntity<UserPackageResponseDTO> getUserPackageById(@PathVariable Long id) {
        UserPackageResponseDTO response = userPackageService.getUserPackageById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener todos los paquetes de un usuario")
    public ResponseEntity<List<UserPackageResponseDTO>> getAllPackagesByUser(@PathVariable Long userId) {
        List<UserPackageResponseDTO> response = userPackageService.getAllPackagesByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Obtener paquetes activos de un usuario")
    public ResponseEntity<List<UserPackageResponseDTO>> getActivePackagesByUser(@PathVariable Long userId) {
        List<UserPackageResponseDTO> response = userPackageService.getActivePackagesByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/best-available")
    @Operation(summary = "Obtener el mejor paquete disponible del usuario")
    public ResponseEntity<UserPackageResponseDTO> getBestAvailablePackage(@PathVariable Long userId) {
        UserPackageResponseDTO response = userPackageService.getBestAvailablePackage(userId);
        return ResponseEntity.ok(response);
    }
}
