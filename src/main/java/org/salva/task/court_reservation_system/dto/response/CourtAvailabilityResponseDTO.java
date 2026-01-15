package org.salva.task.court_reservation_system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO para mostrar disponibilidad de una cancha en un día
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtAvailabilityResponseDTO {

    private Long courtId;
    private String courtName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private List<TimeSlotDTO> availableSlots;
    private List<TimeSlotDTO> occupiedSlots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSlotDTO {
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        private BigDecimal estimatedPrice;  // Solo para available
        private Double priceFactor;         // 0.8 (valle), 1.0 (normal), 1.5 (pico)
    }
}
