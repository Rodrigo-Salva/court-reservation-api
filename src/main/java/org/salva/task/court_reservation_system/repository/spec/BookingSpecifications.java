package org.salva.task.court_reservation_system.repository.spec;

import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> inVenue(Long venueId) {
        return (root, query, cb) -> venueId == null ? cb.conjunction()
                : cb.equal(root.join("court").join("venue").get("id"), venueId);
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    /** Busca por nombre del cliente o de la cancha. */
    public static Specification<Booking> matches(String text) {
        return (root, query, cb) -> {
            if (SpecificationUtils.isBlank(text)) {
                return cb.conjunction();
            }
            String pattern = SpecificationUtils.likePattern(text);
            return cb.or(
                    cb.like(cb.lower(root.join("user").get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.join("court").get("name")), pattern, '\\'));
        };
    }
}
