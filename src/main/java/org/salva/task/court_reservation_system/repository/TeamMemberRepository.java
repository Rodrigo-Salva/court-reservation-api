package org.salva.task.court_reservation_system.repository;
import org.salva.task.court_reservation_system.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
 List<TeamMember> findByTeamId(Long teamId); List<TeamMember> findByUserId(Long userId); long countByTeamId(Long teamId); boolean existsByTeamIdAndUserId(Long teamId, Long userId); void deleteByTeamIdAndUserId(Long teamId, Long userId);
}
