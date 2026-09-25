package org.salva.task.court_reservation_system.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salva.task.court_reservation_system.dto.request.MatchResultRequestDTO;
import org.salva.task.court_reservation_system.dto.request.TournamentRequestDTO;
import org.salva.task.court_reservation_system.entity.Tournament;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.enums.SportType;
import org.salva.task.court_reservation_system.enums.TournamentStatus;
import org.salva.task.court_reservation_system.repository.TournamentMatchRepository;
import org.salva.task.court_reservation_system.repository.TournamentParticipantRepository;
import org.salva.task.court_reservation_system.repository.TournamentRepository;
import org.salva.task.court_reservation_system.repository.VenueRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentControllerTest {

    @Mock private TournamentRepository tournamentRepository;
    @Mock private TournamentParticipantRepository participantRepository;
    @Mock private TournamentMatchRepository matchRepository;
    @Mock private VenueRepository venueRepository;

    private TournamentController controller;
    private final Venue venueA = Venue.builder().id(1L).name("A").build();
    private final Venue venueB = Venue.builder().id(2L).name("B").build();

    @BeforeEach
    void setUp() {
        controller = new TournamentController(tournamentRepository, participantRepository, matchRepository, venueRepository, new AccessControlService());
    }

    @Test
    void venueStaffCreatesTournamentsInTheirOwnVenueOnly() {
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venueA));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(call -> call.getArgument(0));

        Tournament created = controller.create(request(null), staff(venueA)).getBody();
        assertEquals(1L, created.venueId());

        assertThrows(AccessDeniedException.class, () -> controller.create(request(2L), staff(venueA)));
    }

    @Test
    void globalAdminChoosesTheVenueOrLeavesTheTournamentGlobal() {
        when(venueRepository.findById(2L)).thenReturn(Optional.of(venueB));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(2L, controller.create(request(2L), admin()).getBody().venueId());
        assertNull(controller.create(request(null), admin()).getBody().venueId());
    }

    @Test
    void staffCannotManageTournamentsOfAnotherVenueOrGlobalOnes() {
        when(tournamentRepository.findById(10L)).thenReturn(Optional.of(tournament(venueB)));
        when(tournamentRepository.findById(11L)).thenReturn(Optional.of(tournament(null)));
        CustomUserDetails staff = staff(venueA);

        assertThrows(AccessDeniedException.class, () -> controller.generateFixtures(10L, staff));
        assertThrows(AccessDeniedException.class, () -> controller.generateFixtures(11L, staff));
        MatchResultRequestDTO result = new MatchResultRequestDTO();
        result.setScoreOne(6);
        result.setScoreTwo(3);
        assertThrows(AccessDeniedException.class, () -> controller.result(10L, 1L, result, staff));
        verify(matchRepository, never()).save(any());
    }

    private TournamentRequestDTO request(Long venueId) {
        TournamentRequestDTO dto = new TournamentRequestDTO();
        dto.setName("Copa");
        dto.setSportType(SportType.PADEL);
        dto.setStartDate(LocalDate.now().plusDays(5));
        dto.setMaxParticipants(8);
        dto.setVenueId(venueId);
        return dto;
    }

    private Tournament tournament(Venue venue) {
        return Tournament.builder().id(10L).name("Copa").sportType(SportType.PADEL).startDate(LocalDate.now().plusDays(5))
                .maxParticipants(8).status(TournamentStatus.INSCRIPCION).venue(venue).build();
    }

    private CustomUserDetails staff(Venue venue) {
        User user = new User();
        user.setId(50L);
        user.setRole(Role.VENUE_ADMIN);
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
