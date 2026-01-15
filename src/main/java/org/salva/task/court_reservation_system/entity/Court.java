package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.SportType;

import java.math.BigDecimal;

@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Court {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe de tener entre 3 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "El tipo de deporte es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SportType sportType;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 2, message = "La capacidad minima es 2 personas")
    @Max(value = 50, message = "La capacidad maxima es 50 personas")
    @Column(nullable = false)
    private Integer capacity;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe de ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceBaseHour;

    @Column(nullable = false)
    private Boolean active = true;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;
}
