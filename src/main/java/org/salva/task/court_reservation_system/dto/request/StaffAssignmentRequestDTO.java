package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.salva.task.court_reservation_system.enums.Role;

/**
 * DTO para asignar (o reasignar) un usuario existente como personal de una sede
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAssignmentRequestDTO {

    @NotNull(message = "El rol es obligatorio")
    private Role role;

    @NotNull(message = "La sede es obligatoria")
    private Long venueId;
}
