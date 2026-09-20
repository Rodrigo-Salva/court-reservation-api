package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.salva.task.court_reservation_system.enums.PaymentMethod;

@Getter @Setter
public class PaymentRequestDTO {
    @NotNull private Long bookingId;
    @NotNull private PaymentMethod method;
    @Size(max = 19) private String simulatedCardNumber;
}
