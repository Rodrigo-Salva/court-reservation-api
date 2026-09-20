package org.salva.task.court_reservation_system.service;

public interface AuditService {
    void record(String action, String resourceType, Long resourceId, String detail);
    void record(String action, String resourceType, Long resourceId, String detail, Long venueId);
}
