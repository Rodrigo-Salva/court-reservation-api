package org.salva.task.court_reservation_system.dto.request;

import org.salva.task.court_reservation_system.enums.SportType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para crear o actualizar una cancha
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;

    @NotNull(message = "El tipo de deporte es obligatorio")
    private SportType sportType;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 2, message = "La capacidad mínima es 2 personas")
    @Max(value = 50, message = "La capacidad máxima es 50 personas")
    private Integer capacity;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal basePricePerHour;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;
}
