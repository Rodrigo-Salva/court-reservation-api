package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_created", columnList = "created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "actor_user_id") private Long actorUserId;
    @Column(name = "actor_email", length = 150) private String actorEmail;
    @Column(nullable = false, length = 80) private String action;
    @Column(name = "resource_type", nullable = false, length = 60) private String resourceType;
    @Column(name = "resource_id") private Long resourceId;
    @Column(length = 500) private String detail;
    @Column(name = "venue_id") private Long venueId;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
