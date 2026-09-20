package org.salva.task.court_reservation_system.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="tournament_matches") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder public class TournamentMatch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tournament_id",nullable=false) private Tournament tournament;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="player_one_id",nullable=false) private User playerOne; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="player_two_id",nullable=false) private User playerTwo;
 private Integer scoreOne; private Integer scoreTwo; @Builder.Default @Column(nullable=false) private Boolean completed=false;
}
