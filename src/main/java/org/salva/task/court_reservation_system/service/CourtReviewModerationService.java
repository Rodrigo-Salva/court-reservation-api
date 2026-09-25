package org.salva.task.court_reservation_system.service;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.response.CourtReviewResponseDTO;
import org.salva.task.court_reservation_system.dto.response.PageResponseDTO;
import org.salva.task.court_reservation_system.repository.spec.CourtReviewSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public PageResponseDTO<CourtReviewResponseDTO> list(CustomUserDetails currentUser, String filter, String text, int page, int size) {
        Long venueId = accessControl.resolveVenueFilter(currentUser);
        Specification<CourtReview> spec = Specification.where(CourtReviewSpecifications.inVenue(venueId))
                .and(CourtReviewSpecifications.byVisibility(filter)).and(CourtReviewSpecifications.matches(text));
        return PageResponseDTO.of(reviewRepository
                .findAll(spec, PageResponseDTO.pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toDto));
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
