package org.salva.task.court_reservation_system.security;

import org.salva.task.court_reservation_system.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos de login fallidos por email + IP: tras {@value #MAX_FAILURES} fallos dentro de la
 * ventana, bloquea nuevos intentos durante {@value #LOCK_MINUTES} minutos. El estado vive en memoria
 * (una instancia); con varias réplicas conviene moverlo a Redis.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final long WINDOW_MINUTES = 15;
    static final long LOCK_MINUTES = 15;
    private static final int PRUNE_THRESHOLD = 10_000;

    private record Attempts(int failures, Instant firstFailure, Instant lockedUntil) {
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public String keyFor(String email, String ip) {
        return (email == null ? "" : email.trim().toLowerCase(Locale.ROOT)) + "|" + (ip == null ? "" : ip);
    }

    public void assertNotBlocked(String key) {
        Attempts current = attempts.get(key);
        if (current == null || current.lockedUntil() == null) {
            return;
        }
        Instant now = clock.instant();
        if (current.lockedUntil().isAfter(now)) {
            long seconds = Math.max(1, Duration.between(now, current.lockedUntil()).getSeconds());
            throw new TooManyRequestsException("Demasiados intentos fallidos. Inténtalo de nuevo en " + ((seconds + 59) / 60) + " minuto(s).", seconds);
        }
        attempts.remove(key, current);
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        prune(now);
        attempts.compute(key, (k, current) -> {
            if (current == null || now.isAfter(current.firstFailure().plus(Duration.ofMinutes(WINDOW_MINUTES)))) {
                return new Attempts(1, now, null);
            }
            int failures = current.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(Duration.ofMinutes(LOCK_MINUTES)) : current.lockedUntil();
            return new Attempts(failures, current.firstFailure(), lockedUntil);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private void prune(Instant now) {
        if (attempts.size() < PRUNE_THRESHOLD) {
            return;
        }
        Instant cutoff = now.minus(Duration.ofMinutes(WINDOW_MINUTES + LOCK_MINUTES));
        attempts.entrySet().removeIf(entry -> entry.getValue().firstFailure().isBefore(cutoff));
    }
}
