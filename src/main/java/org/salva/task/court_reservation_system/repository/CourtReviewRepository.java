package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.CourtReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface CourtReviewRepository extends JpaRepository<CourtReview, Long>, JpaSpecificationExecutor<CourtReview> {
    List<CourtReview> findByCourtIdAndHiddenFalseOrderByCreatedAtDesc(Long courtId);
    boolean existsByCourtIdAndUserId(Long courtId, Long userId);
}
