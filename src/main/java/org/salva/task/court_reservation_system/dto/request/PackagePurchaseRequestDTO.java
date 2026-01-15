package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para que un usuario compre un paquete
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePurchaseRequestDTO {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotNull(message = "El ID del paquete es obligatorio")
    private Long packageId;
}
