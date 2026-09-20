package org.salva.task.court_reservation_system.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckInCodeResponseDTO {
    private Long bookingId;
    private String code;
}
