package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.dto.request.UserRequestDTO;
import org.salva.task.court_reservation_system.dto.response.UserResponseDTO;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.mapper.UserMapper;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de User
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        log.info("Creating user with email: {}", requestDTO.getEmail());

        // Validar email único
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new ValidationException("Ya existe un usuario con el email: " + requestDTO.getEmail());
        }

        // Validar teléfono único
        if (userRepository.existsByPhone(requestDTO.getPhone())) {
            throw new ValidationException("Ya existe un usuario con el teléfono: " + requestDTO.getPhone());
        }

        // Mapper: DTO → Entity
        User user = userMapper.toEntity(requestDTO);

        // Guardar
        user = userRepository.save(user);

        log.info("User created successfully with id: {}", user.getId());

        return userMapper.toResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        log.debug("Getting user by id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        return userMapper.toResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        return userMapper.toResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllActiveUsers() {
        log.debug("Getting all active users");

        List<User> users = userRepository.findByActiveTrue();

        return userMapper.toResponseDTOList(users);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> searchUsersByName(String name) {
        log.debug("Searching users by name: {}", name);

        List<User> users = userRepository.findByNameContainingIgnoreCase(name);

        return userMapper.toResponseDTOList(users);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUsersByMembershipType(MembershipType membershipType) {
        log.debug("Getting users by membership type: {}", membershipType);

        List<User> users = userRepository.findByMembershipTypeAndActiveTrue(membershipType);

        return userMapper.toResponseDTOList(users);
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO) {
        log.info("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        // Validar email único (si cambió)
        if (!user.getEmail().equals(requestDTO.getEmail()) &&
                userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new ValidationException("Ya existe un usuario con el email: " + requestDTO.getEmail());
        }

        // Validar teléfono único (si cambió)
        if (!user.getPhone().equals(requestDTO.getPhone()) &&
                userRepository.existsByPhone(requestDTO.getPhone())) {
            throw new ValidationException("Ya existe un usuario con el teléfono: " + requestDTO.getPhone());
        }

        // Mapper: actualiza solo campos no-null
        userMapper.updateEntityFromDTO(requestDTO, user);

        user = userRepository.save(user);

        log.info("User updated successfully with id: {}", user.getId());

        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateMembership(Long id, MembershipType newMembership) {
        log.info("Updating membership for user id: {} to {}", id, newMembership);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        user.setMembershipType(newMembership);
        user = userRepository.save(user);

        log.info("Membership updated successfully for user id: {}", id);

        return userMapper.toResponseDTO(user);
    }

    @Override
    public void deactivateUser(Long id) {
        log.info("Deactivating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        user.setActive(false);
        userRepository.save(user);

        log.info("User deactivated successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }
}
