package org.salva.task.court_reservation_system.dto.response;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de respuesta para paquete
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageResponseDTO {

    private Long id;
    private String name;
    private Integer hoursQuantity;
    private BigDecimal price;
    private BigDecimal discountPercentage;
    private Integer validityDays;
    private Boolean active;

    // Calculados
    private BigDecimal pricePerHour;  // price / hoursQuantity
    private BigDecimal savings;       // Ahorro comparado con precio normal
}
