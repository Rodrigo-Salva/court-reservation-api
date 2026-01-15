package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.MembershipType;

/**
 * DTO para crear o actualizar un usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "El teléfono debe tener entre 9 y 15 dígitos")
    private String phone;

    @NotNull(message = "El tipo de membresía es obligatorio")
    private MembershipType membershipType;
}
