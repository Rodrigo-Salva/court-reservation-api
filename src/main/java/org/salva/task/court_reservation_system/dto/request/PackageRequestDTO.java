package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para crear o actualizar un paquete (solo admin)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageRequestDTO {

    @NotBlank(message = "El nombre del paquete es obligatorio")
    @Size(min = 3, max = 100)
    private String name;

    @NotNull(message = "La cantidad de horas es obligatoria")
    @Min(value = 1, message = "El paquete debe tener al menos 1 hora")
    @Max(value = 100, message = "El paquete no puede exceder 100 horas")
    private Integer hoursQuantity;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    @NotNull(message = "El descuento es obligatorio")
    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "1.0", message = "El descuento no puede ser mayor a 100%")
    private BigDecimal discountPercentage;

    @NotNull(message = "La vigencia es obligatoria")
    @Min(value = 1, message = "La vigencia mínima es 1 día")
    @Max(value = 365, message = "La vigencia máxima es 365 días")
    private Integer validityDays;
}
