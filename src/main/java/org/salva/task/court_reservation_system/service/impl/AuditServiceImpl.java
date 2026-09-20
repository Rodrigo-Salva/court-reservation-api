package org.salva.task.court_reservation_system.service.impl;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.entity.AuditLog;
import org.salva.task.court_reservation_system.repository.AuditLogRepository;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.salva.task.court_reservation_system.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final AuditLogRepository auditLogRepository;
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String resourceType, Long resourceId, String detail) {
        record(action, resourceType, resourceId, detail, null);
    }

    @Override @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String resourceType, Long resourceId, String detail, Long venueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long actorId = null; String email = "sistema";
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            actorId = user.getId(); email = user.getUsername();
        }
        auditLogRepository.save(AuditLog.builder().actorUserId(actorId).actorEmail(email).action(action)
                .resourceType(resourceType).resourceId(resourceId).detail(detail).venueId(venueId).build());
    }
}
