package org.salva.task.court_reservation_system.service.impl;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.UserNotification;
import org.salva.task.court_reservation_system.enums.NotificationType;
import org.salva.task.court_reservation_system.repository.UserNotificationRepository;
import org.salva.task.court_reservation_system.service.NotificationService;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor public class NotificationServiceImpl implements NotificationService {
 private final UserNotificationRepository repository;
 public void notify(User user, NotificationType type, String title, String message) { repository.save(UserNotification.builder().user(user).type(type).title(title).message(message).build()); }
}
