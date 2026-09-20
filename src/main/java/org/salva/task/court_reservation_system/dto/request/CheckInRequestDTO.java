package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckInRequestDTO {
    @NotBlank(message = "El código de check-in es obligatorio")
    private String code;
}
