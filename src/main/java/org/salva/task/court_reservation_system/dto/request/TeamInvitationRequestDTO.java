package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamInvitationRequestDTO {
    @NotBlank(message = "El email es obligatorio") @Email(message = "El email debe ser válido")
    private String email;
}
