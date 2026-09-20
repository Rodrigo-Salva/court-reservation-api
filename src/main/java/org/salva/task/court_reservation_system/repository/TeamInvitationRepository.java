package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.TeamInvitation;
import org.salva.task.court_reservation_system.enums.TeamInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    List<TeamInvitation> findByInvitedUserIdAndStatusOrderByCreatedAtDesc(Long userId, TeamInvitationStatus status);
    List<TeamInvitation> findByTeamIdAndStatusOrderByCreatedAtDesc(Long teamId, TeamInvitationStatus status);
    boolean existsByTeamIdAndInvitedUserIdAndStatus(Long teamId, Long userId, TeamInvitationStatus status);
}
