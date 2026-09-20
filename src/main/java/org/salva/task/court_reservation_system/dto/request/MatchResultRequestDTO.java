package org.salva.task.court_reservation_system.dto.request; import jakarta.validation.constraints.*; import lombok.Data;
@Data public class MatchResultRequestDTO { @NotNull @Min(0) private Integer scoreOne; @NotNull @Min(0) private Integer scoreTwo; }
