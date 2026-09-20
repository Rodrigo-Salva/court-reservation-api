package org.salva.task.court_reservation_system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.CourtReview;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.repository.CourtReviewRepository;
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
class CourtReviewModerationServiceTest {

    @Mock private CourtReviewRepository reviewRepository;

    private CourtReviewModerationService service;
    private final Venue venueA = Venue.builder().id(1L).name("A").build();
    private final Venue venueB = Venue.builder().id(2L).name("B").build();

    @BeforeEach
    void setUp() {
        service = new CourtReviewModerationService(reviewRepository, new AccessControlService());
    }

    @Test
    void globalAdminSeesAllReviewsAndStaffOnlyTheirVenue() {
        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(review(1L, venueA), review(2L, venueB)));
        when(reviewRepository.findByCourtVenueIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(review(1L, venueA)));

        assertEquals(2, service.list(user(Role.ADMIN, null)).size());
        assertEquals(1, service.list(user(Role.VENUE_ADMIN, venueA)).size());
    }

    @Test
    void staffCanHideAndShowReviewOfTheirVenue() {
        CourtReview review = review(1L, venueA);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);

        assertTrue(service.setHidden(1L, true, user(Role.VENUE_ADMIN, venueA)).getHidden());
        assertFalse(service.setHidden(1L, false, user(Role.VENUE_ADMIN, venueA)).getHidden());
    }

    @Test
    void staffCannotModerateReviewOfAnotherVenue() {
        CourtReview review = review(1L, venueB);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        CustomUserDetails staff = user(Role.VENUE_ADMIN, venueA);

        assertThrows(AccessDeniedException.class, () -> service.setHidden(1L, true, staff));
        assertThrows(AccessDeniedException.class, () -> service.delete(1L, staff));
        verify(reviewRepository, never()).save(any());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesReviewAndMissingReviewIsNotFound() {
        CourtReview review = review(1L, venueA);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        service.delete(1L, user(Role.ADMIN, null));
        verify(reviewRepository).delete(review);
        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L, user(Role.ADMIN, null)));
    }

    private CourtReview review(Long id, Venue venue) {
        Court court = new Court();
        court.setId(id.intValue());
        court.setName("Cancha " + id);
        court.setVenue(venue);
        User author = new User();
        author.setId(100L);
        author.setName("Autor");
        return CourtReview.builder().id(id).court(court).user(author).rating(4).comment("ok").build();
    }

    private CustomUserDetails user(Role role, Venue venue) {
        User user = new User();
        user.setId(50L);
        user.setRole(role);
        user.setVenue(venue);
        return new CustomUserDetails(user);
    }
}
