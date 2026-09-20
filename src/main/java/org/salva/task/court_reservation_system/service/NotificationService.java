package org.salva.task.court_reservation_system.service;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.enums.NotificationType;
public interface NotificationService { void notify(User user, NotificationType type, String title, String message); }
