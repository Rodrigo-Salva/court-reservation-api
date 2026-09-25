package org.salva.task.court_reservation_system.repository.spec;

import org.salva.task.court_reservation_system.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    /** Busca por nombre, email o teléfono. */
    public static Specification<User> matches(String text) {
        return (root, query, cb) -> {
            if (SpecificationUtils.isBlank(text)) {
                return cb.conjunction();
            }
            String pattern = SpecificationUtils.likePattern(text);
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("email")), pattern, '\\'),
                    cb.like(root.get("phone"), pattern, '\\'));
        };
    }

    public static Specification<User> hasActive(Boolean active) {
        return (root, query, cb) -> active == null ? cb.conjunction() : cb.equal(root.get("active"), active);
    }
}
