package org.salva.task.court_reservation_system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.salva.task.court_reservation_system.enums.MembershipType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private MembershipType membershipType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registrationDate;

    private Boolean active;

    // Información adicional calculada
    private Double membershipDiscount;  // En formato decimal (0.20 = 20%)
    private Integer maxDaysAdvance;
}

