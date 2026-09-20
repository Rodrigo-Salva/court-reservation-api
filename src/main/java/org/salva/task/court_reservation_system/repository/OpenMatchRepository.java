package org.salva.task.court_reservation_system.repository;
import org.salva.task.court_reservation_system.entity.OpenMatch;
import org.salva.task.court_reservation_system.enums.OpenMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OpenMatchRepository extends JpaRepository<OpenMatch,Long>{ List<OpenMatch> findByStatus(OpenMatchStatus status); boolean existsByBookingId(Long bookingId); List<OpenMatch> findByCreatorIdOrderByIdDesc(Long creatorId); }
