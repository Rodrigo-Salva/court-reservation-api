package org.salva.task.court_reservation_system.entity;
import jakarta.persistence.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.OpenMatchStatus;
import java.time.LocalDateTime;
@Entity @Table(name="open_matches") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OpenMatch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="booking_id", nullable=false, unique=true) private Booking booking;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="creator_id", nullable=false) private User creator;
 @Column(nullable=false) private Integer maxPlayers;
 @Column(length=300) private String note;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private OpenMatchStatus status;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
 @PrePersist void created(){createdAt=LocalDateTime.now(); if(status==null)status=OpenMatchStatus.ABIERTO;}
}
