package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.enums.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad que representa una reserva de cancha
 */
@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "idx_booking_date_time", columnList = "booking_date, start_time, end_time"),
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== RELACIONES ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_court"))
    private Court court;

    // ========== DATOS DE LA RESERVA ==========

    @NotNull(message = "La fecha de reserva es obligatoria")
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDIENTE;

    // ========== DATOS DE PRECIO ==========

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "dynamic_surcharges", nullable = false, precision = 10, scale = 2)
    private BigDecimal dynamicSurcharges;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "applied_discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal appliedDiscount;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // ========== RECURRENCIA ==========

    @Column(name = "is_recurrent", nullable = false)
    private Boolean isRecurrent = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", length = 20)
    private RecurrenceFrequency recurrenceFrequency = RecurrenceFrequency.NINGUNA;

    @Column(name = "parent_booking_id")
    private Long parentBookingId;

    // ========== USO DE PAQUETE ==========

    @Column(name = "uses_package", nullable = false)
    private Boolean usesPackage = false;

    @Column(name = "user_package_id")
    private Long userPackageId;

    @Column(name = "hours_deducted", precision = 4, scale = 2)
    private BigDecimal hoursDeducted;

    // ========== AUDITORÍA ==========

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Size(max = 500)
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Column(name = "penalty_percentage", precision = 3, scale = 2)
    private BigDecimal penaltyPercentage;

    @DecimalMin(value = "0.0")
    @Column(name = "penalty_amount", precision = 10, scale = 2)
    private BigDecimal penaltyAmount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = BookingStatus.PENDIENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (status == BookingStatus.CANCELADA && cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        }
    }
}
