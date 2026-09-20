package org.salva.task.court_reservation_system.dto.request;

import org.salva.task.court_reservation_system.enums.SportType;
import java.time.LocalDate;

/** Filtros del reporte operativo: sede, cancha y deporte son opcionales. */
public record ReportFilterDTO(LocalDate startDate, LocalDate endDate, Long venueId, Long courtId, SportType sportType) {
}
