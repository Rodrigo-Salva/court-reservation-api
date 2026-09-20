package org.salva.task.court_reservation_system.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salva.task.court_reservation_system.dto.request.CourtRequestDTO;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.mapper.CourtMapper;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.repository.VenueRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtServiceImplTest {

    @Mock private CourtRepository courtRepository;
    @Mock private CourtMapper courtMapper;
    @Mock private VenueRepository venueRepository;

    private CourtServiceImpl service;
    private final Venue venueA = Venue.builder().id(1L).name("A").build();
    private final Venue venueB = Venue.builder().id(2L).name("B").build();

    @BeforeEach
    void setUp() {
        service = new CourtServiceImpl(courtRepository, courtMapper, venueRepository, new AccessControlService());
    }

    @Test
    void createCourtForcesTheStaffVenueWhenNoneIsSent() {
        Court court = new Court();
        CourtRequestDTO request = request(null);
        when(courtMapper.toEntity(request)).thenReturn(court);
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venueA));
        when(courtRepository.save(court)).thenReturn(court);

        service.createCourt(request, staff(Role.VENUE_ADMIN, venueA));

        assertEquals(venueA, court.getVenue());
    }

    @Test
    void createCourtRejectsAnotherVenueForStaff() {
        Court court = new Court();
        CourtRequestDTO request = request(2L);
        when(courtMapper.toEntity(request)).thenReturn(court);

        assertThrows(AccessDeniedException.class, () -> service.createCourt(request, staff(Role.VENUE_ADMIN, venueA)));
        verify(courtRepository, never()).save(any());
    }

    @Test
    void createCourtLetsGlobalAdminPickAnyVenue() {
        Court court = new Court();
        CourtRequestDTO request = request(2L);
        when(courtMapper.toEntity(request)).thenReturn(court);
        when(venueRepository.findById(2L)).thenReturn(Optional.of(venueB));
        when(courtRepository.save(court)).thenReturn(court);

        service.createCourt(request, admin());

        assertEquals(venueB, court.getVenue());
    }

    @Test
    void getAllCourtsFiltersByVenueForStaffOnly() {
        when(courtRepository.findByVenueId(1L)).thenReturn(List.of());
        when(courtRepository.findAll()).thenReturn(List.of());

        service.getAllCourts(staff(Role.VENUE_ADMIN, venueA));
        verify(courtRepository).findByVenueId(1L);
        verify(courtRepository, never()).findAll();

        service.getAllCourts(admin());
        verify(courtRepository).findAll();
    }

    @Test
    void updateCourtDeniesCourtOfAnotherVenue() {
        Court court = court(10, venueB);
        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourt(10L, request(null), staff(Role.VENUE_ADMIN, venueA)));
        verify(courtRepository, never()).save(any());
    }

    @Test
    void updateCourtCannotMoveCourtToAnotherVenue() {
        Court court = court(10, venueA);
        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourt(10L, request(2L), staff(Role.VENUE_ADMIN, venueA)));
        verify(courtRepository, never()).save(any());
    }

    @Test
    void deactivateAndActivateDenyCourtOfAnotherVenue() {
        Court court = court(10, venueB);
        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));
        CustomUserDetails staff = staff(Role.VENUE_ADMIN, venueA);

        assertThrows(AccessDeniedException.class, () -> service.deactivateCourt(10L, staff));
        assertThrows(AccessDeniedException.class, () -> service.activateCourt(10L, staff));
        verify(courtRepository, never()).save(any());
    }

    @Test
    void deactivateAndActivateWorkForCourtOfOwnVenue() {
        Court court = court(10, venueA);
        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));
        CustomUserDetails staff = staff(Role.VENUE_ADMIN, venueA);

        service.deactivateCourt(10L, staff);
        assertFalse(court.getActive());
        service.activateCourt(10L, staff);
        assertTrue(court.getActive());
    }

    private CourtRequestDTO request(Long venueId) {
        CourtRequestDTO dto = new CourtRequestDTO();
        dto.setName("Cancha X");
        dto.setVenueId(venueId);
        return dto;
    }

    private Court court(int id, Venue venue) {
        Court court = new Court();
        court.setId(id);
        court.setName("Cancha " + id);
        court.setVenue(venue);
        court.setActive(true);
        return court;
    }

    private CustomUserDetails staff(Role role, Venue venue) {
        User user = new User();
        user.setId(50L);
        user.setRole(role);
        user.setVenue(venue);
        return new CustomUserDetails(user);
    }

    private CustomUserDetails admin() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.ADMIN);
        return new CustomUserDetails(user);
    }
}
