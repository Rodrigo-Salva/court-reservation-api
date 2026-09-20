package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.TeamInvitationStatus;
import java.time.LocalDateTime;

@Entity @Table(name = "team_invitations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamInvitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id", nullable = false) private Team team;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invited_user_id", nullable = false) private User invitedUser;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invited_by_id", nullable = false) private User invitedBy;
    @Builder.Default @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TeamInvitationStatus status = TeamInvitationStatus.PENDIENTE;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
