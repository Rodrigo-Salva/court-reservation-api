package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.WaitingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository para operaciones de base de datos de WaitingList
 */
@Repository
public interface WaitingListRepository extends JpaRepository<WaitingList, Long> {

    /**
     * Busca solicitudes en lista de espera de un usuario
     */
    List<WaitingList> findByUserIdOrderByRequestDateDesc(Long userId);

    /**
     * Busca usuarios esperando por una cancha y fecha específicas
     * Ordenados por orden de llegada (FIFO)
     */
    @Query("SELECT wl FROM WaitingList wl WHERE wl.court.id = :courtId " +
            "AND wl.desiredDate = :date " +
            "AND wl.desiredStartTime = :startTime " +
            "AND wl.desiredEndTime = :endTime " +
            "AND wl.notified = false " +
            "ORDER BY wl.requestDate ASC")
    List<WaitingList> findPendingByCourtAndTimeSlot(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Busca usuarios notificados pero que no han respondido
     */
    List<WaitingList> findByNotifiedTrue();

    /**
     * Busca notificaciones expiradas (usuarios no respondieron a tiempo)
     */
    @Query("SELECT wl FROM WaitingList wl WHERE wl.notified = true " +
            "AND wl.notificationExpirationDate < :now")
    List<WaitingList> findExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * Elimina solicitudes antiguas (limpieza de datos)
     */
    @Query("DELETE FROM WaitingList wl WHERE wl.desiredDate < :cutoffDate")
    void deleteOldRequests(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Cuenta cuántas personas están esperando un horario específico
     */
    long countByCourtIdAndDesiredDateAndDesiredStartTimeAndDesiredEndTime(
            Long courtId,
            LocalDate desiredDate,
            LocalTime desiredStartTime,
            LocalTime desiredEndTime
    );
}
