package org.salva.task.court_reservation_system.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para agregar a lista de espera
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingListRequestDTO {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotNull(message = "El ID de la cancha es obligatorio")
    private Long courtId;

    @NotNull(message = "La fecha deseada es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o futura")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate desiredDate;

    @NotNull(message = "La hora de inicio deseada es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime desiredStartTime;

    @NotNull(message = "La hora de fin deseada es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime desiredEndTime;
}
