package org.salva.task.court_reservation_system.security;

import org.salva.task.court_reservation_system.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Centraliza la autorización de recursos que pertenecen a un usuario o a una sede. */
@Service
public class AccessControlService {

    public void requireOwnerOrAdmin(Long ownerId, CustomUserDetails currentUser) {
        if (currentUser == null || ownerId == null) {
            throw new AccessDeniedException("No tiene permiso para realizar esta operación");
        }

        if (currentUser.getUser().getRole() != Role.USER || ownerId.equals(currentUser.getId())) {
            return;
        }

        throw new AccessDeniedException("No tiene permiso para acceder a este recurso");
    }

    /**
     * Devuelve la sede a la que se deben limitar las consultas del usuario:
     * null para administradores globales (sin filtro), el id de su sede para
     * VENUE_ADMIN/RECEPTIONIST. Falla cerrado si el personal no tiene sede.
     */
    public Long resolveVenueFilter(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("No tiene permiso para realizar esta operación");
        }
        Role role = currentUser.getUser().getRole();
        if (isGlobalAdmin(role)) {
            return null;
        }
        if (isVenueStaff(role)) {
            Long venueId = currentUser.getVenueId();
            if (venueId == null) {
                throw new AccessDeniedException("Su usuario no tiene una sede asignada");
            }
            return venueId;
        }
        throw new AccessDeniedException("No tiene permiso para acceder a este recurso");
    }

    /** Exige que el recurso pertenezca a la sede del usuario (los administradores globales pasan siempre). */
    public void requireSameVenueOrAdmin(Long resourceVenueId, CustomUserDetails currentUser) {
        Long allowedVenueId = resolveVenueFilter(currentUser);
        if (allowedVenueId == null) {
            return;
        }
        if (resourceVenueId == null || !allowedVenueId.equals(resourceVenueId)) {
            throw new AccessDeniedException("No tiene permiso para acceder a recursos de otra sede");
        }
    }

    public boolean isVenueStaff(CustomUserDetails currentUser) {
        return currentUser != null && isVenueStaff(currentUser.getUser().getRole());
    }

    private boolean isGlobalAdmin(Role role) {
        return role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    private boolean isVenueStaff(Role role) {
        return role == Role.VENUE_ADMIN || role == Role.RECEPTIONIST;
    }
}
