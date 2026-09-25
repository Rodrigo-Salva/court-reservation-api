package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.Payment;
import org.salva.task.court_reservation_system.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByBookingId(Long bookingId);
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    java.math.BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.booking.court.venue.id = :venueId")
    java.math.BigDecimal sumAmountByStatusAndVenue(@Param("status") PaymentStatus status, @Param("venueId") Long venueId);

    @Query("SELECT p.booking.id FROM Payment p WHERE p.status = :status AND p.booking.id IN :bookingIds")
    List<Long> findBookingIdsByStatus(@Param("bookingIds") Collection<Long> bookingIds, @Param("status") PaymentStatus status);
}
