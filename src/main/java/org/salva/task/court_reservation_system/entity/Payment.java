package org.salva.task.court_reservation_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.salva.task.court_reservation_system.enums.PaymentMethod;
import org.salva.task.court_reservation_system.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "booking_id", nullable = false, unique = true) private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(nullable = false, unique = true, length = 40) private String operationCode;
    @Column(length = 200) private String rejectionReason;
    @Column(nullable = false) private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
