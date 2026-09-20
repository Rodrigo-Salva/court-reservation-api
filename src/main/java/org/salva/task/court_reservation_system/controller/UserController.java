package org.salva.task.court_reservation_system.controller;

import org.salva.task.court_reservation_system.dto.request.StaffAssignmentRequestDTO;
import org.salva.task.court_reservation_system.dto.request.StaffUserRequestDTO;
import org.salva.task.court_reservation_system.dto.request.UserRequestDTO;
import org.salva.task.court_reservation_system.dto.response.UserResponseDTO;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.salva.task.court_reservation_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Controller para gestión de usuarios
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API para gestión de usuarios")
public class UserController {

    private final UserService userService;
    private final AccessControlService accessControl;

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO requestDTO) {
        UserResponseDTO response = userService.createUser(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/staff")
    @Operation(summary = "Crear personal de sede", description = "Crea un VENUE_ADMIN o RECEPTIONIST asociado a una sede (solo administración global)")
    public ResponseEntity<UserResponseDTO> createStaffUser(@Valid @RequestBody StaffUserRequestDTO requestDTO) {
        return new ResponseEntity<>(userService.createStaffUser(requestDTO), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/venue")
    @Operation(summary = "Asignar sede a un usuario", description = "Convierte a un usuario existente en personal de una sede (solo administración global)")
    public ResponseEntity<UserResponseDTO> assignStaffVenue(@PathVariable Long id,
            @Valid @RequestBody StaffAssignmentRequestDTO requestDTO) {
        return ResponseEntity.ok(userService.assignStaffVenue(id, requestDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        accessControl.requireOwnerOrAdmin(id, userDetails);
        UserResponseDTO response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Obtener usuario por email")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO response = userService.getUserByEmail(email);
        accessControl.requireOwnerOrAdmin(response.getId(), userDetails);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios activos")
    public ResponseEntity<List<UserResponseDTO>> getAllActiveUsers() {
        List<UserResponseDTO> response = userService.getAllActiveUsers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @Operation(summary = "Listar todos los usuarios (admin)", description = "Incluye usuarios inactivos, solo para administración")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar usuario", description = "Reactiva un usuario previamente desactivado")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar usuarios por nombre")
    public ResponseEntity<List<UserResponseDTO>> searchUsersByName(@RequestParam String name) {
        List<UserResponseDTO> response = userService.searchUsersByName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/membership/{membershipType}")
    @Operation(summary = "Obtener usuarios por tipo de membresía")
    public ResponseEntity<List<UserResponseDTO>> getUsersByMembershipType(
            @PathVariable MembershipType membershipType
    ) {
        List<UserResponseDTO> response = userService.getUsersByMembershipType(membershipType);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        accessControl.requireOwnerOrAdmin(id, userDetails);
        UserResponseDTO response = userService.updateUser(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/membership")
    @Operation(summary = "Actualizar membresía de usuario")
    public ResponseEntity<UserResponseDTO> updateMembership(
            @PathVariable Long id,
            @RequestParam MembershipType membershipType
    ) {
        UserResponseDTO response = userService.updateMembership(id, membershipType);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar usuario")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/email")
    @Operation(summary = "Verificar si existe email")
    public ResponseEntity<Boolean> existsByEmail(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exists/phone")
    @Operation(summary = "Verificar si existe teléfono")
    public ResponseEntity<Boolean> existsByPhone(@RequestParam String phone) {
        boolean exists = userService.existsByPhone(phone);
        return ResponseEntity.ok(exists);
    }
}
