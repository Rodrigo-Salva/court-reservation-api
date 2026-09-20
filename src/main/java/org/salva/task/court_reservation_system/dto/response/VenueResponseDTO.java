package org.salva.task.court_reservation_system.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class VenueResponseDTO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private Boolean active;
}
