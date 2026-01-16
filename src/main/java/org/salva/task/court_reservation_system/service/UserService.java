package org.salva.task.court_reservation_system.service;

import org.salva.task.court_reservation_system.dto.request.UserRequestDTO;
import org.salva.task.court_reservation_system.dto.response.UserResponseDTO;
import org.salva.task.court_reservation_system.enums.MembershipType;

import java.util.List;

/**
 * Interface para servicios de User (Usuario)
 */
public interface UserService {

    /**
     * Crea un nuevo usuario
     */
    UserResponseDTO createUser(UserRequestDTO requestDTO);

    /**
     * Obtiene un usuario por ID
     */
    UserResponseDTO getUserById(Long id);

    /**
     * Obtiene un usuario por email
     */
    UserResponseDTO getUserByEmail(String email);

    /**
     * Obtiene todos los usuarios activos
     */
    List<UserResponseDTO> getAllActiveUsers();

    /**
     * Busca usuarios por nombre
     */
    List<UserResponseDTO> searchUsersByName(String name);

    /**
     * Obtiene usuarios por tipo de membresía
     */
    List<UserResponseDTO> getUsersByMembershipType(MembershipType membershipType);

    /**
     * Actualiza un usuario existente
     */
    UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO);

    /**
     * Actualiza la membresía de un usuario
     */
    UserResponseDTO updateMembership(Long id, MembershipType newMembership);

    /**
     * Desactiva un usuario (soft delete)
     */
    void deactivateUser(Long id);

    /**
     * Verifica si un email ya está registrado
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si un teléfono ya está registrado
     */
    boolean existsByPhone(String phone);
}
