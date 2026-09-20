package org.salva.task.court_reservation_system.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class CourtReviewResponseDTO {
    private Long id; private Long courtId; private String courtName; private String venueName;
    private String userName; private Integer rating; private String comment; private Boolean hidden; private LocalDateTime createdAt;
}
