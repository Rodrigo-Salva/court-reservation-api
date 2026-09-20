package org.salva.task.court_reservation_system.repository;
import org.salva.task.court_reservation_system.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TeamRepository extends JpaRepository<Team, Long> { List<Team> findByOwnerId(Long ownerId); }
