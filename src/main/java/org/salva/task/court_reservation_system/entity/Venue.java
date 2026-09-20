package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "venues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Venue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 100) @Column(nullable = false, unique = true, length = 100)
    private String name;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200)
    private String address;
    @Size(max = 30) @Column(length = 30)
    private String phone;
    @Builder.Default @Column(nullable = false)
    private Boolean active = true;
}
