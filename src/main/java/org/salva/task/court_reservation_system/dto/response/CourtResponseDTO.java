package org.salva.task.court_reservation_system.dto.response;

import org.salva.task.court_reservation_system.enums.SportType;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de respuesta para cancha
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtResponseDTO {

    private Long id;
    private String name;
    private SportType sportType;
    private Integer capacity;
    private BigDecimal basePricePerHour;
    private Boolean active;
    private String description;
}
