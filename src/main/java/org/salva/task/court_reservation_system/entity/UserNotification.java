package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.NotificationType;
import java.time.LocalDateTime;

@Entity @Table(name = "user_notifications") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserNotification {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
 @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private NotificationType type;
 @Column(nullable = false, length = 120) private String title;
 @Column(nullable = false, length = 500) private String message;
 @Column(name = "is_read", nullable = false) @Builder.Default private Boolean read = false;
 @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
 @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
