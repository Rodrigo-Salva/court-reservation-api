package org.salva.task.court_reservation_system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para cancelación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationResponseDTO {

    private Long bookingId;
    private String status;  // "CANCELLED"

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    private Long hoursInAdvance;

    // Penalización
    private BigDecimal penaltyPercentage;
    private BigDecimal penaltyAmount;
    private BigDecimal refundAmount;

    private String message;  // "Cancelación exitosa sin penalización"

    // Si usó paquete
    private Boolean usedPackage;
    private Integer hoursRefunded;  // Horas devueltas al paquete (si aplica)

    // Si era recurrente
    private Boolean wasRecurrent;
    private Integer totalCancelled;  // Cantidad de reservas canceladas
}
