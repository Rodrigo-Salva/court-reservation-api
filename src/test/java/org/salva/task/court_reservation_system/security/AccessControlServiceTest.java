package org.salva.task.court_reservation_system.security;

import org.junit.jupiter.api.Test;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.Role;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessControlServiceTest {

    private final AccessControlService accessControl = new AccessControlService();

    @Test
    void allowsResourceOwner() {
        assertDoesNotThrow(() -> accessControl.requireOwnerOrAdmin(7L, user(7L, Role.USER)));
    }

    @Test
    void deniesDifferentRegularUser() {
        assertThrows(AccessDeniedException.class,
                () -> accessControl.requireOwnerOrAdmin(7L, user(8L, Role.USER)));
    }

    @Test
    void allowsAdministrator() {
        assertDoesNotThrow(() -> accessControl.requireOwnerOrAdmin(7L, user(8L, Role.ADMIN)));
    }

    @Test
    void venueStaffCanAccessResourcesOfTheirOwnVenue() {
        assertDoesNotThrow(() -> accessControl.requireSameVenueOrAdmin(3L, staff(Role.VENUE_ADMIN, 3L)));
        assertDoesNotThrow(() -> accessControl.requireSameVenueOrAdmin(3L, staff(Role.RECEPTIONIST, 3L)));
    }

    @Test
    void venueStaffCannotAccessResourcesOfAnotherVenue() {
        assertThrows(AccessDeniedException.class,
                () -> accessControl.requireSameVenueOrAdmin(4L, staff(Role.VENUE_ADMIN, 3L)));
        assertThrows(AccessDeniedException.class,
                () -> accessControl.requireSameVenueOrAdmin(4L, staff(Role.RECEPTIONIST, 3L)));
    }

    @Test
    void venueStaffCannotAccessResourcesWithoutVenue() {
        assertThrows(AccessDeniedException.class,
                () -> accessControl.requireSameVenueOrAdmin(null, staff(Role.VENUE_ADMIN, 3L)));
    }

    @Test
    void venueStaffWithoutAssignedVenueIsDeniedEverywhere() {
        assertThrows(AccessDeniedException.class,
                () -> accessControl.requireSameVenueOrAdmin(3L, staff(Role.VENUE_ADMIN, null)));
        assertThrows(AccessDeniedException.class,
                () -> accessControl.resolveVenueFilter(staff(Role.RECEPTIONIST, null)));
    }

    @Test
    void globalAdministratorsAccessAnyVenue() {
        assertDoesNotThrow(() -> accessControl.requireSameVenueOrAdmin(4L, user(1L, Role.ADMIN)));
        assertDoesNotThrow(() -> accessControl.requireSameVenueOrAdmin(null, user(1L, Role.SUPER_ADMIN)));
    }

    @Test
    void regularUserIsDeniedVenueScopedResources() {
        assertThrows(AccessDeniedException.class,
                () -> accessControl.requireSameVenueOrAdmin(3L, user(1L, Role.USER)));
        assertThrows(AccessDeniedException.class, () -> accessControl.resolveVenueFilter(user(1L, Role.USER)));
    }

    @Test
    void resolveVenueFilterReturnsNullForGlobalAdminsAndVenueIdForStaff() {
        assertNull(accessControl.resolveVenueFilter(user(1L, Role.ADMIN)));
        assertNull(accessControl.resolveVenueFilter(user(1L, Role.SUPER_ADMIN)));
        assertEquals(3L, accessControl.resolveVenueFilter(staff(Role.VENUE_ADMIN, 3L)));
        assertEquals(3L, accessControl.resolveVenueFilter(staff(Role.RECEPTIONIST, 3L)));
    }

    @Test
    void resolveVenueFilterRejectsMissingUser() {
        assertThrows(AccessDeniedException.class, () -> accessControl.resolveVenueFilter(null));
    }

    private CustomUserDetails user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return new CustomUserDetails(user);
    }

    private CustomUserDetails staff(Role role, Long venueId) {
        User user = new User();
        user.setId(50L);
        user.setRole(role);
        if (venueId != null) {
            user.setVenue(Venue.builder().id(venueId).name("Sede " + venueId).build());
        }
        return new CustomUserDetails(user);
    }
}
