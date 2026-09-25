package org.salva.task.court_reservation_system.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

/** Página de transacciones junto con los totales cobrado y reembolsado de todo el alcance del usuario. */
@Getter @Builder
public class PaymentPageResponseDTO {
    private List<PaymentResponseDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private BigDecimal approvedTotal;
    private BigDecimal refundedTotal;
}
