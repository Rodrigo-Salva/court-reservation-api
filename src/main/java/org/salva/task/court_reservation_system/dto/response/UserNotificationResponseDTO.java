package org.salva.task.court_reservation_system.dto.response;
import lombok.Builder;
import lombok.Getter;
import org.salva.task.court_reservation_system.enums.NotificationType;
import java.time.LocalDateTime;
@Getter @Builder public class UserNotificationResponseDTO { private Long id; private NotificationType type; private String title; private String message; private Boolean read; private LocalDateTime createdAt; }
