package org.salva.task.court_reservation_system.controller;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.response.PageResponseDTO;
import org.salva.task.court_reservation_system.entity.AuditLog;
import org.salva.task.court_reservation_system.repository.AuditLogRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/audit-logs") @RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogRepository auditLogRepository;
    private final AccessControlService accessControl;

    @GetMapping public PageResponseDTO<AuditLog> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long venueId = accessControl.resolveVenueFilter(currentUser);
        var pageable = PageResponseDTO.pageable(page, size, Sort.unsorted());
        return PageResponseDTO.of(venueId == null ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.findByVenueIdOrderByCreatedAtDesc(venueId, pageable));
    }
}
