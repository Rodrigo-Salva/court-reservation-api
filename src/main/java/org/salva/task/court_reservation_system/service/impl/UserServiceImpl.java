package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.dto.request.StaffAssignmentRequestDTO;
import org.salva.task.court_reservation_system.dto.request.StaffUserRequestDTO;
import org.salva.task.court_reservation_system.dto.request.UserRequestDTO;
import org.salva.task.court_reservation_system.dto.response.UserResponseDTO;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.mapper.UserMapper;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.repository.VenueRepository;
import org.salva.task.court_reservation_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final VenueRepository venueRepository;
    private final PasswordEncoder passwordEncoder;

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
    public UserResponseDTO createStaffUser(StaffUserRequestDTO requestDTO) {
        log.info("Creating staff user with email: {}", requestDTO.getEmail());

        requireVenueStaffRole(requestDTO.getRole());
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new ValidationException("Ya existe un usuario con el email: " + requestDTO.getEmail());
        }
        if (userRepository.existsByPhone(requestDTO.getPhone())) {
            throw new ValidationException("Ya existe un usuario con el teléfono: " + requestDTO.getPhone());
        }
        Venue venue = findVenue(requestDTO.getVenueId());

        User user = User.builder()
                .name(requestDTO.getName())
                .email(requestDTO.getEmail())
                .phone(requestDTO.getPhone())
                .membershipType(MembershipType.NINGUNA)
                .password(passwordEncoder.encode(requestDTO.getPassword()))
                .role(requestDTO.getRole())
                .venue(venue)
                .active(true)
                .build();
        user = userRepository.save(user);

        log.info("Staff user created with id: {} for venue: {}", user.getId(), venue.getId());
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO assignStaffVenue(Long id, StaffAssignmentRequestDTO requestDTO) {
        log.info("Assigning user {} to venue {} as {}", id, requestDTO.getVenueId(), requestDTO.getRole());

        requireVenueStaffRole(requestDTO.getRole());
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN) {
            throw new ValidationException("No se puede asignar una sede a un administrador global");
        }

        user.setRole(requestDTO.getRole());
        user.setVenue(findVenue(requestDTO.getVenueId()));
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    private void requireVenueStaffRole(Role role) {
        if (role != Role.VENUE_ADMIN && role != Role.RECEPTIONIST) {
            throw new ValidationException("El rol debe ser VENUE_ADMIN o RECEPTIONIST");
        }
    }

    private Venue findVenue(Long venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con id: " + venueId));
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
    public List<UserResponseDTO> getAllUsers() {
        log.debug("Getting all users (including inactive)");

        List<User> users = userRepository.findAll();

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
    public void activateUser(Long id) {
        log.info("Activating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        user.setActive(true);
        userRepository.save(user);

        log.info("User activated successfully with id: {}", id);
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
