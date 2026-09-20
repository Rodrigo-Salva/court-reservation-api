package org.salva.task.court_reservation_system.repository;
import org.salva.task.court_reservation_system.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> { List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId); }
