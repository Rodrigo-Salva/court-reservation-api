package org.salva.task.court_reservation_system.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO para mostrar el desglose del cálculo de precio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceCalculationResponseDTO {

    private BigDecimal basePricePerHour;
    private Integer durationHours;
    private BigDecimal baseTotal;

    // Factores aplicados
    private Map<String, BigDecimal> appliedFactors;  // {"weekend": 1.3, "peakHours": 1.5}

    private BigDecimal subtotalAfterSurcharges;

    // Descuentos
    private BigDecimal membershipDiscount;
    private BigDecimal recurrentDiscount;
    private BigDecimal totalDiscount;

    private BigDecimal finalPrice;

    // Desglose legible
    private String breakdown;  // "Base: S/. 100.00 + Weekend (30%): S/. 30.00 - Membership (20%): -S/. 26.00 = S/. 104.00"
}
