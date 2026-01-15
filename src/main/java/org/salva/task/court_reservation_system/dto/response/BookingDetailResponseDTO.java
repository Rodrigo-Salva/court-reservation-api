package org.salva.task.court_reservation_system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.enums.RecurrenceFrequency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO de respuesta detallada para reserva (incluye cálculos de precio)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDetailResponseDTO {

    private Long id;

    // Usuario
    private Long userId;
    private String userName;
    private String userEmail;

    // Cancha
    private Long courtId;
    private String courtName;
    private String courtSportType;

    // Fechas y horarios
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Integer durationHours;

    // Estado
    private BookingStatus status;

    // Desglose de precios
    private BigDecimal basePrice;
    private BigDecimal dynamicSurcharges;
    private BigDecimal appliedDiscount;
    private BigDecimal totalPrice;

    // Recurrencia
    private Boolean isRecurrent;
    private RecurrenceFrequency recurrenceFrequency;
    private Long parentBookingId;

    // Paquete
    private Boolean usesPackage;
    private Long userPackageId;
    private BigDecimal hoursDeducted;

    // Auditoría
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    private String cancellationReason;
    private BigDecimal penaltyPercentage;
    private BigDecimal penaltyAmount;
}
