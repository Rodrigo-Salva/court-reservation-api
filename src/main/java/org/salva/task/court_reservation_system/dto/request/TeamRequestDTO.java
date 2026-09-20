package org.salva.task.court_reservation_system.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data public class TeamRequestDTO { @NotBlank @Size(max = 80) private String name; @Size(max = 300) private String description; }
