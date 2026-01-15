package org.salva.task.court_reservation_system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO de respuesta para lista de espera
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingListResponseDTO {

    private Long id;

    private Long userId;
    private String userName;

    private Long courtId;
    private String courtName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate desiredDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime desiredStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime desiredEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestDate;

    private Boolean notified;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime notificationDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime notificationExpirationDate;

    private Integer positionInQueue;  // Calculado: su posición en la lista
}
