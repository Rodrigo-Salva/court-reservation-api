package org.salva.task.court_reservation_system.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salva.task.court_reservation_system.dto.request.StaffAssignmentRequestDTO;
import org.salva.task.court_reservation_system.dto.request.StaffUserRequestDTO;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.mapper.UserMapper;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.repository.VenueRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private VenueRepository venueRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;
    private final Venue venue = Venue.builder().id(1L).name("Sede Central").build();

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, userMapper, venueRepository, passwordEncoder);
    }

    @Test
    void createStaffUserStoresEncodedPasswordRoleAndVenue() {
        StaffUserRequestDTO request = staffRequest(Role.RECEPTIONIST, 1L);
        when(userRepository.existsByEmail("staff@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("999888777")).thenReturn(false);
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
        when(passwordEncoder.encode("secret1")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createStaffUser(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(Role.RECEPTIONIST, saved.getValue().getRole());
        assertEquals(venue, saved.getValue().getVenue());
        assertEquals("ENCODED", saved.getValue().getPassword());
    }

    @Test
    void createStaffUserRejectsNonStaffRoles() {
        for (Role role : new Role[]{Role.USER, Role.ADMIN, Role.SUPER_ADMIN}) {
            assertThrows(ValidationException.class, () -> service.createStaffUser(staffRequest(role, 1L)));
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void createStaffUserRequiresExistingVenue() {
        when(userRepository.existsByEmail("staff@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("999888777")).thenReturn(false);
        when(venueRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createStaffUser(staffRequest(Role.VENUE_ADMIN, 9L)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignStaffVenueUpdatesRoleAndVenueOfRegularUser() {
        User user = new User();
        user.setId(5L);
        user.setRole(Role.USER);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
        when(userRepository.save(user)).thenReturn(user);

        service.assignStaffVenue(5L, new StaffAssignmentRequestDTO(Role.VENUE_ADMIN, 1L));

        assertEquals(Role.VENUE_ADMIN, user.getRole());
        assertEquals(venue, user.getVenue());
    }

    @Test
    void assignStaffVenueRejectsGlobalAdministrators() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(ValidationException.class,
                () -> service.assignStaffVenue(1L, new StaffAssignmentRequestDTO(Role.VENUE_ADMIN, 1L)));
        verify(userRepository, never()).save(any());
    }

    private StaffUserRequestDTO staffRequest(Role role, Long venueId) {
        return StaffUserRequestDTO.builder().name("Staff").email("staff@test.com").phone("999888777")
                .password("secret1").role(role).venueId(venueId).build();
    }
}
