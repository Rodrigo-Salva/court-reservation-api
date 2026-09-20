package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.CourtReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourtReviewRepository extends JpaRepository<CourtReview, Long> {
    List<CourtReview> findByCourtIdAndHiddenFalseOrderByCreatedAtDesc(Long courtId);
    List<CourtReview> findAllByOrderByCreatedAtDesc();
    List<CourtReview> findByCourtVenueIdOrderByCreatedAtDesc(Long venueId);
    boolean existsByCourtIdAndUserId(Long courtId, Long userId);
}
