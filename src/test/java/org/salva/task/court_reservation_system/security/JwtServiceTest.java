package org.salva.task.court_reservation_system.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.enums.Role;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
    }

    @Test
    void tokenIsValidForTheSameUserAndVersion() {
        CustomUserDetails details = details("ana@test.com", 0);
        String token = jwtService.generateToken(details);

        assertEquals("ana@test.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, details));
    }

    @Test
    void bumpingTheTokenVersionRevokesPreviouslyIssuedTokens() {
        CustomUserDetails details = details("ana@test.com", 0);
        String token = jwtService.generateToken(details);

        details.getUser().setTokenVersion(1);

        assertFalse(jwtService.isTokenValid(token, details));
        assertTrue(jwtService.isTokenValid(jwtService.generateToken(details), details));
    }

    @Test
    void tokenBelongsToItsUserOnly() {
        String token = jwtService.generateToken(details("ana@test.com", 0));

        assertFalse(jwtService.isTokenValid(token, details("otro@test.com", 0)));
    }

    @Test
    void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1_000L);
        CustomUserDetails details = details("ana@test.com", 0);

        String token = jwtService.generateToken(details);

        // Un token vencido lanza al parsear; el filtro lo trata como no autenticado.
        try {
            assertFalse(jwtService.isTokenValid(token, details));
        } catch (io.jsonwebtoken.JwtException expected) {
            assertTrue(true);
        }
    }

    private CustomUserDetails details(String email, int version) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(Role.USER);
        user.setActive(true);
        user.setTokenVersion(version);
        return new CustomUserDetails(user);
    }
}
