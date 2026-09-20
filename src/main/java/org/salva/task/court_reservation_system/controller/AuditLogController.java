package org.salva.task.court_reservation_system.controller;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.entity.AuditLog;
import org.salva.task.court_reservation_system.repository.AuditLogRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequestMapping("/api/audit-logs") @RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogRepository auditLogRepository;
    private final AccessControlService accessControl;
    @GetMapping public List<AuditLog> listLatest(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Long venueId = accessControl.resolveVenueFilter(currentUser);
        return venueId == null ? auditLogRepository.findTop100ByOrderByCreatedAtDesc()
                : auditLogRepository.findTop100ByVenueIdOrderByCreatedAtDesc(venueId);
    }
}
