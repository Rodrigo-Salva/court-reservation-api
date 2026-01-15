package org.salva.task.court_reservation_system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de respuesta simple para reserva
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private Long id;

    // IDs de relaciones
    private Long userId;
    private Long courtId;

    // Nombres para mostrar
    private String userName;
    private String courtName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private BookingStatus status;
    private BigDecimal totalPrice;

    private Boolean isRecurrent;
    private Boolean usesPackage;
}

