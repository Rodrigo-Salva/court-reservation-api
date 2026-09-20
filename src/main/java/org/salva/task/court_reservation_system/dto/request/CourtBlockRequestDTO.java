package org.salva.task.court_reservation_system.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.salva.task.court_reservation_system.enums.CourtBlockType;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CourtBlockRequestDTO {
    @NotNull private Long courtId;
    @NotNull @FutureOrPresent @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate blockDate;
    @NotNull @JsonFormat(pattern = "HH:mm") private LocalTime startTime;
    @NotNull @JsonFormat(pattern = "HH:mm") private LocalTime endTime;
    @NotNull private CourtBlockType type;
    @NotBlank @Size(max = 300) private String reason;
}
