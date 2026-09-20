package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByActiveTrue();
    boolean existsByNameIgnoreCase(String name);
    Optional<Venue> findByName(String name);
}
