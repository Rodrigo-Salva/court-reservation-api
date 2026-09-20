package org.salva.task.court_reservation_system.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RescheduleBookingRequestDTO {
    @NotNull @FutureOrPresent @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;
    @NotNull @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @NotNull @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
}
