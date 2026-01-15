package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del paquete es obligatorio")
    @Size(min = 3, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "La cantidad de horas es obligatoria")
    @Min(value = 1, message = "El paquete debe tener al menos 1 hora")
    @Max(value = 100, message = "El paquete no puede exceder 100 horas")
    @Column(nullable = false)
    private Integer amountHours;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "El descuento es obligatorio")
    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "1.0", message = "El descuento no puede ser mayor a 100%")
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal discountPercent;

    @NotNull(message = "La vigencia es obligatoria")
    @Min(value = 1, message = "La vigencia mínima es 1 día")
    @Max(value = 365, message = "La vigencia máxima es 365 días")
    @Column(nullable = false)
    private Integer validityDays;

    @Column(nullable = false)
    private Boolean active = true;
}
