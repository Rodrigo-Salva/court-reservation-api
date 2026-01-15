package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad que representa una solicitud en lista de espera
 */
@Entity
@Table(name = "waiting_list",
        indexes = {
                @Index(name = "idx_waiting_court_date", columnList = "court_id, desired_date"),
                @Index(name = "idx_waiting_user", columnList = "user_id"),
                @Index(name = "idx_waiting_notified", columnList = "notified")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== RELACIONES ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_waiting_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false, foreignKey = @ForeignKey(name = "fk_waiting_court"))
    private Court court;

    // ========== DATOS DEL HORARIO DESEADO ==========

    @NotNull(message = "La fecha deseada es obligatoria")
    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;

    @NotNull(message = "La hora de inicio deseada es obligatoria")
    @Column(name = "desired_start_time", nullable = false)
    private LocalTime desiredStartTime;

    @NotNull(message = "La hora de fin deseada es obligatoria")
    @Column(name = "desired_end_time", nullable = false)
    private LocalTime desiredEndTime;

    // ========== CONTROL DE NOTIFICACIONES ==========

    @Column(name = "request_date", nullable = false, updatable = false)
    private LocalDateTime requestDate;

    @Column(nullable = false)
    private Boolean notified = false;

    @Column(name = "notification_date")
    private LocalDateTime notificationDate;

    @Column(name = "notification_expiration_date")
    private LocalDateTime notificationExpirationDate;

    @PrePersist
    protected void onCreate() {
        requestDate = LocalDateTime.now();
    }

    /**
     * Marca como notificado y establece tiempo de expiración
     */
    public void notifyUser(int minutesToRespond) {
        this.notified = true;
        this.notificationDate = LocalDateTime.now();
        this.notificationExpirationDate = LocalDateTime.now().plusMinutes(minutesToRespond);
    }

    /**
     * Verifica si la notificación expiró (usuario no respondió a tiempo)
     */
    public boolean isNotificationExpired() {
        return notified &&
                notificationExpirationDate != null &&
                LocalDateTime.now().isAfter(notificationExpirationDate);
    }
}
