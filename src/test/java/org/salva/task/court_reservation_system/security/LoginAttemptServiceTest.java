package org.salva.task.court_reservation_system.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salva.task.court_reservation_system.exception.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        service = new LoginAttemptService(clock);
    }

    @Test
    void blocksAfterMaxFailuresAndReportsRetryAfter() {
        String key = service.keyFor("User@Test.com", "10.0.0.1");
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            service.recordFailure(key);
            assertDoesNotThrow(() -> service.assertNotBlocked(key));
        }
        service.recordFailure(key);

        TooManyRequestsException blocked = assertThrows(TooManyRequestsException.class, () -> service.assertNotBlocked(key));
        assertTrue(blocked.getRetryAfterSeconds() > 0 && blocked.getRetryAfterSeconds() <= LoginAttemptService.LOCK_MINUTES * 60);
    }

    @Test
    void keyIgnoresEmailCaseAndSurroundingSpaces() {
        assertTrue(service.keyFor(" USER@test.com ", "1.1.1.1").equals(service.keyFor("user@test.com", "1.1.1.1")));
    }

    @Test
    void unblocksAfterLockDuration() {
        String key = service.keyFor("user@test.com", "10.0.0.1");
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) service.recordFailure(key);
        assertThrows(TooManyRequestsException.class, () -> service.assertNotBlocked(key));

        clock.advance(Duration.ofMinutes(LoginAttemptService.LOCK_MINUTES + 1));
        assertDoesNotThrow(() -> service.assertNotBlocked(key));
    }

    @Test
    void successResetsTheCounter() {
        String key = service.keyFor("user@test.com", "10.0.0.1");
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) service.recordFailure(key);
        service.recordSuccess(key);
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) service.recordFailure(key);

        assertDoesNotThrow(() -> service.assertNotBlocked(key));
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        String key = service.keyFor("user@test.com", "10.0.0.1");
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) service.recordFailure(key);
        clock.advance(Duration.ofMinutes(LoginAttemptService.WINDOW_MINUTES + 1));
        service.recordFailure(key);

        assertDoesNotThrow(() -> service.assertNotBlocked(key));
    }

    @Test
    void differentClientsAreIndependent() {
        String attacker = service.keyFor("user@test.com", "6.6.6.6");
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) service.recordFailure(attacker);

        assertThrows(TooManyRequestsException.class, () -> service.assertNotBlocked(attacker));
        assertDoesNotThrow(() -> service.assertNotBlocked(service.keyFor("user@test.com", "7.7.7.7")));
    }

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
