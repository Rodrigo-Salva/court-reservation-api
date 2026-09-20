package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.CourtBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CourtBlockRepository extends JpaRepository<CourtBlock, Long> {
    List<CourtBlock> findByCourtIdAndBlockDateAndActiveTrue(Long courtId, LocalDate blockDate);
    @Query("SELECT COUNT(b) > 0 FROM CourtBlock b WHERE b.court.id = :courtId AND b.blockDate = :date " +
            "AND b.active = true AND b.startTime < :endTime AND b.endTime > :startTime")
    boolean existsOverlappingBlock(@Param("courtId") Long courtId, @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);
}
