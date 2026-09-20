package org.salva.task.court_reservation_system.controller;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.response.UserNotificationResponseDTO;
import org.salva.task.court_reservation_system.entity.UserNotification;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.repository.UserNotificationRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor public class NotificationController {
 private final UserNotificationRepository repository; private final AccessControlService accessControl;
 @GetMapping("/my") public List<UserNotificationResponseDTO> mine(@AuthenticationPrincipal CustomUserDetails user) { return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::dto).toList(); }
 @PatchMapping("/{id}/read") public UserNotificationResponseDTO markRead(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) { UserNotification n = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada")); accessControl.requireOwnerOrAdmin(n.getUser().getId(), user); n.setRead(true); return dto(repository.save(n)); }
 @PatchMapping("/read-all") public void markAllRead(@AuthenticationPrincipal CustomUserDetails user) { List<UserNotification> notifications = repository.findByUserIdOrderByCreatedAtDesc(user.getId()); notifications.forEach(n -> n.setRead(true)); repository.saveAll(notifications); }
 private UserNotificationResponseDTO dto(UserNotification n) { return UserNotificationResponseDTO.builder().id(n.getId()).type(n.getType()).title(n.getTitle()).message(n.getMessage()).read(n.getRead()).createdAt(n.getCreatedAt()).build(); }
}
