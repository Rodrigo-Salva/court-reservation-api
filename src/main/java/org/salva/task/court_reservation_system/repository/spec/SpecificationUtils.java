package org.salva.task.court_reservation_system.repository.spec;

import java.util.Locale;

final class SpecificationUtils {

    private SpecificationUtils() {
    }

    /** Patrón LIKE en minúsculas y con los comodines del usuario escapados (usar con escape '\\'). */
    static String likePattern(String text) {
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
