package org.salva.task.court_reservation_system.repository.spec;

import org.salva.task.court_reservation_system.entity.Payment;
import org.salva.task.court_reservation_system.enums.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;

public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    public static Specification<Payment> inVenue(Long venueId) {
        return (root, query, cb) -> venueId == null ? cb.conjunction()
                : cb.equal(root.join("booking").join("court").join("venue").get("id"), venueId);
    }

    public static Specification<Payment> hasStatus(PaymentStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    /** Busca por nombre del cliente, nombre de la cancha o código de operación. */
    public static Specification<Payment> matches(String text) {
        return (root, query, cb) -> {
            if (SpecificationUtils.isBlank(text)) {
                return cb.conjunction();
            }
            String pattern = SpecificationUtils.likePattern(text);
            return cb.or(
                    cb.like(cb.lower(root.join("user").get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.join("booking").join("court").get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("operationCode")), pattern, '\\'));
        };
    }
}
