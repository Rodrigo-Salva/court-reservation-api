package org.salva.task.court_reservation_system.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.salva.task.court_reservation_system.enums.PaymentMethod;
import org.salva.task.court_reservation_system.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class PaymentResponseDTO {
    private Long id; private Long bookingId; private String courtName; private BigDecimal amount;
    private PaymentMethod method; private PaymentStatus status; private String operationCode;
    private String rejectionReason; private LocalDateTime paidAt; private LocalDateTime refundedAt;
    private String userName; private LocalDateTime createdAt;
}
