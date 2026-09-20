package org.salva.task.court_reservation_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VenueRequestDTO {
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Size(max = 200) private String address;
    @Size(max = 30) private String phone;
}
