package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.salva.task.court_reservation_system.enums.CourtBlockType;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "court_blocks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourtBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "court_id", nullable = false) private Court court;
    @NotNull @Column(nullable = false) private LocalDate blockDate;
    @NotNull @Column(nullable = false) private LocalTime startTime;
    @NotNull @Column(nullable = false) private LocalTime endTime;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CourtBlockType type;
    @NotBlank @Column(nullable = false, length = 300) private String reason;
    @Builder.Default @Column(nullable = false) private Boolean active = true;
}
