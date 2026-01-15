package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para cancelar una reserva
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationRequestDTO {

    @NotNull(message = "El ID de la reserva es obligatorio")
    private Long bookingId;

    @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
    private String reason;

    private Boolean cancelAllRecurrent = false;  // Si es recurrente, cancelar todas
}
