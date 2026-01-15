package org.salva.task.court_reservation_system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para paquete de usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPackageResponseDTO {

    private Long id;

    private Long userId;
    private String userName;

    private Long packageId;
    private String packageName;

    private Integer initialHours;
    private Integer remainingHours;
    private Integer usedHours;  // Calculado: initialHours - remainingHours

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime purchaseDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expirationDate;

    private Long daysUntilExpiration;  // Calculado
    private Boolean active;
    private Boolean isExpired;  // Calculado
}
