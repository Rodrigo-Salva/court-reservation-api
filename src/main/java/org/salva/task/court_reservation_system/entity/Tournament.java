package org.salva.task.court_reservation_system.entity;
import jakarta.persistence.*; import lombok.*; import org.salva.task.court_reservation_system.enums.*; import java.time.*;
@Entity @Table(name="tournaments") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder public class Tournament {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,length=100) private String name;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private SportType sportType; @Column(nullable=false) private LocalDate startDate;
 @Column(nullable=false) private Integer maxParticipants; @Enumerated(EnumType.STRING) @Column(nullable=false) private TournamentStatus status;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="venue_id") @com.fasterxml.jackson.annotation.JsonIgnore private Venue venue;
 @com.fasterxml.jackson.annotation.JsonProperty("venueId") public Long venueId(){return venue==null?null:venue.getId();}
 @com.fasterxml.jackson.annotation.JsonProperty("venueName") public String venueName(){return venue==null?null:venue.getName();}
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt; @PrePersist void created(){createdAt=LocalDateTime.now();if(status==null)status=TournamentStatus.INSCRIPCION;}
}
