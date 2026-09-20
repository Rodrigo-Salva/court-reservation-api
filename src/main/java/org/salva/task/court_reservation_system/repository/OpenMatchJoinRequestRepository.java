package org.salva.task.court_reservation_system.repository;
import org.salva.task.court_reservation_system.entity.OpenMatchJoinRequest;
import org.salva.task.court_reservation_system.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OpenMatchJoinRequestRepository extends JpaRepository<OpenMatchJoinRequest,Long>{ boolean existsByOpenMatchIdAndUserId(Long matchId,Long userId); long countByOpenMatchIdAndStatus(Long matchId, JoinRequestStatus status); List<OpenMatchJoinRequest> findByOpenMatchId(Long matchId); }
