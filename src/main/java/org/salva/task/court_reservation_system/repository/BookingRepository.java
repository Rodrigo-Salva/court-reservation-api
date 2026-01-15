package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository para operaciones de base de datos de Booking
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Busca reservas por usuario
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Busca reservas por usuario y estado
     */
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    /**
     * Busca reservas por cancha y fecha
     */
    List<Booking> findByCourtIdAndBookingDate(Long courtId, LocalDate date);

    /**
     * Busca reservas confirmadas de una cancha en una fecha específica
     * (útil para verificar disponibilidad)
     */
    @Query("SELECT b FROM Booking b WHERE b.court.id = :courtId " +
            "AND b.bookingDate = :date " +
            "AND b.status = 'CONFIRMED' " +
            "ORDER BY b.startTime ASC")
    List<Booking> findConfirmedBookingsByCourtAndDate(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date
    );

    /**
     * Verifica si existe solapamiento de horarios para una cancha
     * CRÍTICO para la regla de negocio de no solapamiento
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.court.id = :courtId " +
            "AND b.bookingDate = :date " +
            "AND b.status = 'CONFIRMED' " +
            "AND (" +
            "  (b.startTime < :endTime AND b.endTime > :startTime)" +
            ")")
    boolean existsOverlappingBooking(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Busca reservas recurrentes hijas de una reserva padre
     */
    List<Booking> findByParentBookingId(Long parentBookingId);

    /**
     * Busca reservas futuras de un usuario (para validar límites)
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId " +
            "AND b.bookingDate >= :today " +
            "AND b.status = 'CONFIRMED' " +
            "ORDER BY b.bookingDate ASC, b.startTime ASC")
    List<Booking> findFutureBookingsByUser(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    /**
     * Busca reservas que usan un paquete específico
     */
    List<Booking> findByUserPackageId(Long userPackageId);

    /**
     * Busca reservas pendientes de completar (para marcar como COMPLETED automáticamente)
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
            "AND b.bookingDate < :date " +
            "OR (b.bookingDate = :date AND b.endTime < :time)")
    List<Booking> findPastConfirmedBookings(
            @Param("date") LocalDate date,
            @Param("time") LocalTime time
    );

    /**
     * Busca reservas por rango de fechas y cancha (para reportes)
     */
    @Query("SELECT b FROM Booking b WHERE b.court.id = :courtId " +
            "AND b.bookingDate BETWEEN :startDate AND :endDate " +
            "AND b.status IN :statuses " +
            "ORDER BY b.bookingDate DESC")
    List<Booking> findByCourtAndDateRangeAndStatuses(
            @Param("courtId") Long courtId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<BookingStatus> statuses
    );

    /**
     * Estadísticas: Total recaudado por cancha en un periodo
     */
    @Query("SELECT b.court.id, b.court.name, SUM(b.totalPrice) " +
            "FROM Booking b " +
            "WHERE b.bookingDate BETWEEN :startDate AND :endDate " +
            "AND b.status IN ('CONFIRMED', 'COMPLETED') " +
            "GROUP BY b.court.id, b.court.name " +
            "ORDER BY SUM(b.totalPrice) DESC")
    List<Object[]> getTotalRevenueByCourtInPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Busca reservas a punto de iniciar (para notificaciones)
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
            "AND b.bookingDate = :date " +
            "AND b.startTime BETWEEN :startTime AND :endTime")
    List<Booking> findBookingsStartingSoon(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
