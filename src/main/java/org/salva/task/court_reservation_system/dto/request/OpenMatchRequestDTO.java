package org.salva.task.court_reservation_system.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class OpenMatchRequestDTO { @NotNull private Long bookingId; @NotNull @Min(2) @Max(50) private Integer maxPlayers; @Size(max=300) private String note; }
