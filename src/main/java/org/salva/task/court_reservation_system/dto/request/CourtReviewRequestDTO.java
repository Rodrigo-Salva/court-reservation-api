package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CourtReviewRequestDTO {
    @NotNull private Long courtId;
    @NotNull @Min(1) @Max(5) private Integer rating;
    @Size(max = 500) private String comment;
}
