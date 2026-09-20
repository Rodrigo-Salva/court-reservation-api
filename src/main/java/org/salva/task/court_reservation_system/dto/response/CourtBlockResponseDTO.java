package org.salva.task.court_reservation_system.dto.response;

import lombok.Builder;
import lombok.Data;
import org.salva.task.court_reservation_system.enums.CourtBlockType;
import java.time.LocalDate;
import java.time.LocalTime;

@Data @Builder
public class CourtBlockResponseDTO {
    private Long id; private Long courtId; private LocalDate blockDate; private LocalTime startTime; private LocalTime endTime;
    private CourtBlockType type; private String reason; private Boolean active;
}
