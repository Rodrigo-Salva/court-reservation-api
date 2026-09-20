package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.TeamMemberRole;
import java.time.LocalDateTime;

@Entity @Table(name = "team_members", uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id", nullable = false) private Team team;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TeamMemberRole role;
    @Column(nullable = false, updatable = false) private LocalDateTime joinedAt;
    @PrePersist void onCreate() { joinedAt = LocalDateTime.now(); }
}
