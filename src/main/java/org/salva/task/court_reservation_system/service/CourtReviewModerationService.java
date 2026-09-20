package org.salva.task.court_reservation_system.service;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.response.CourtReviewResponseDTO;
import org.salva.task.court_reservation_system.entity.CourtReview;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.repository.CourtReviewRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Moderación de reseñas: los administradores globales ven todas; el personal de sede solo las de su sede. */
@Service
@RequiredArgsConstructor
@Transactional
public class CourtReviewModerationService {

    private final CourtReviewRepository reviewRepository;
    private final AccessControlService accessControl;

    @Transactional(readOnly = true)
    public List<CourtReviewResponseDTO> list(CustomUserDetails currentUser) {
        Long venueId = accessControl.resolveVenueFilter(currentUser);
        List<CourtReview> reviews = venueId == null
                ? reviewRepository.findAllByOrderByCreatedAtDesc()
                : reviewRepository.findByCourtVenueIdOrderByCreatedAtDesc(venueId);
        return reviews.stream().map(this::toDto).toList();
    }

    public CourtReviewResponseDTO setHidden(Long id, boolean hidden, CustomUserDetails currentUser) {
        CourtReview review = findAuthorized(id, currentUser);
        review.setHidden(hidden);
        return toDto(reviewRepository.save(review));
    }

    public void delete(Long id, CustomUserDetails currentUser) {
        reviewRepository.delete(findAuthorized(id, currentUser));
    }

    private CourtReview findAuthorized(Long id, CustomUserDetails currentUser) {
        CourtReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada"));
        accessControl.requireSameVenueOrAdmin(review.getCourt().getVenue() != null ? review.getCourt().getVenue().getId() : null, currentUser);
        return review;
    }

    public CourtReviewResponseDTO toDto(CourtReview r) {
        return CourtReviewResponseDTO.builder().id(r.getId()).courtId((long) r.getCourt().getId())
                .courtName(r.getCourt().getName())
                .venueName(r.getCourt().getVenue() != null ? r.getCourt().getVenue().getName() : null)
                .userName(r.getUser().getName()).rating(r.getRating()).comment(r.getComment())
                .hidden(r.getHidden()).createdAt(r.getCreatedAt()).build();
    }
}
