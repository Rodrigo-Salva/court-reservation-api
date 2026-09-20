package org.salva.task.court_reservation_system.entity;
import jakarta.persistence.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.JoinRequestStatus;
import java.time.LocalDateTime;
@Entity @Table(name="open_match_join_requests",uniqueConstraints=@UniqueConstraint(columnNames={"open_match_id","user_id"})) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OpenMatchJoinRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="open_match_id",nullable=false) private OpenMatch openMatch;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private JoinRequestStatus status;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
 @PrePersist void created(){createdAt=LocalDateTime.now(); if(status==null)status=JoinRequestStatus.PENDIENTE;}
}
