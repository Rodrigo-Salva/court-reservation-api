package org.salva.task.court_reservation_system.repository.spec;

import org.salva.task.court_reservation_system.entity.CourtReview;
import org.springframework.data.jpa.domain.Specification;

public final class CourtReviewSpecifications {

    private CourtReviewSpecifications() {
    }

    public static Specification<CourtReview> inVenue(Long venueId) {
        return (root, query, cb) -> venueId == null ? cb.conjunction()
                : cb.equal(root.join("court").join("venue").get("id"), venueId);
    }

    /** filter: "hidden" solo ocultas, "visible" solo visibles, cualquier otro valor todas. */
    public static Specification<CourtReview> byVisibility(String filter) {
        return (root, query, cb) -> "hidden".equals(filter) ? cb.isTrue(root.get("hidden"))
                : "visible".equals(filter) ? cb.isFalse(root.get("hidden")) : cb.conjunction();
    }

    public static Specification<CourtReview> matches(String text) {
        return (root, query, cb) -> {
            if (SpecificationUtils.isBlank(text)) {
                return cb.conjunction();
            }
            String pattern = SpecificationUtils.likePattern(text);
            return cb.or(
                    cb.like(cb.lower(root.join("court").get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.join("user").get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("comment")), pattern, '\\'));
        };
    }
}
