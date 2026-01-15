package org.salva.task.court_reservation_system.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.salva.task.court_reservation_system.enums.RecurrenceFrequency;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para crear reservas recurrentes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurrentBookingRequestDTO {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotNull(message = "El ID de la cancha es obligatorio")
    private Long courtId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o futura")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "La hora de inicio es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotNull(message = "La frecuencia es obligatoria")
    private RecurrenceFrequency frequency;

    @NotNull(message = "La cantidad de semanas es obligatoria")
    @Min(value = 2, message = "Mínimo 2 semanas para reservas recurrentes")
    @Max(value = 12, message = "Máximo 12 semanas para reservas recurrentes")
    private Integer numberOfWeeks;

    private Boolean usesPackage = false;

    private Long userPackageId;
}
