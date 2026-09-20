package org.salva.task.court_reservation_system.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="tournament_participants",uniqueConstraints=@UniqueConstraint(columnNames={"tournament_id","user_id"})) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder public class TournamentParticipant {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tournament_id",nullable=false) private Tournament tournament; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
 @Builder.Default @Column(nullable=false) private Integer points=0; @Builder.Default @Column(nullable=false) private Integer wins=0; @Builder.Default @Column(nullable=false) private Integer losses=0;
}
