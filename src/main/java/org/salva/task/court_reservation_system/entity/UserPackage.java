package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa un paquete de horas comprado por un usuario
 */
@Entity
@Table(name = "user_packages",
        indexes = {
                @Index(name = "idx_user_package_user", columnList = "user_id"),
                @Index(name = "idx_user_package_active", columnList = "active"),
                @Index(name = "idx_user_package_expiration", columnList = "expiration_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== RELACIONES ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_package_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_package_package"))
    private Package packageDetails;

    // ========== DATOS DEL PAQUETE COMPRADO ==========

    @NotNull(message = "Las horas iniciales son obligatorias")
    @Min(value = 1)
    @Column(name = "initial_hours", nullable = false)
    private Integer initialHours;

    @NotNull(message = "Las horas restantes son obligatorias")
    @Min(value = 0)
    @Column(name = "remaining_hours", nullable = false)
    private Integer remainingHours;

    @NotNull(message = "La fecha de compra es obligatoria")
    @Column(name = "purchase_date", nullable = false, updatable = false)
    private LocalDateTime purchaseDate;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        purchaseDate = LocalDateTime.now();
        remainingHours = initialHours;
    }

    /**
     * Descuenta horas del paquete
     * @param hours Cantidad de horas a descontar
     * @return true si se descontó exitosamente, false si no hay suficientes horas
     */
    public boolean deductHours(int hours) {
        if (remainingHours >= hours && active && !isExpired()) {
            remainingHours -= hours;
            if (remainingHours == 0) {
                active = false;
            }
            return true;
        }
        return false;
    }

    /**
     * Verifica si el paquete está vencido
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationDate);
    }

    /**
     * Marca el paquete como vencido si pasó la fecha
     */
    public void checkExpiration() {
        if (isExpired() && active) {
            active = false;
        }
    }
}
