package org.salva.task.court_reservation_system.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para reservas recurrentes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurrentBookingResponseDTO {

    private Integer totalRequested;
    private Integer successfulBookings;
    private Integer failedBookings;

    private List<RecurrentBookingDetail> bookingDetails;

    private BigDecimal estimatedTotalPrice;
    private Boolean recurrentDiscountApplied;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecurrentBookingDetail {
        private Long bookingId;
        private LocalDate bookingDate;
        private String status;  // "CREATED", "FAILED"
        private String reason;  // Si falló: "Court not available", etc.
        private BigDecimal price;
    }
}
