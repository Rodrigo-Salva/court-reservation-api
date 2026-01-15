package org.salva.task.court_reservation_system.repository;

import org.apache.catalina.User;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operaciones de base de datos de User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca usuario por email (único)
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca usuario por teléfono
     */
    Optional<User> findByPhone(String phone);

    /**
     * Busca usuarios por tipo de membresía
     */
    List<User> findByMembershipTypeAndActiveTrue(MembershipType membershipType);

    /**
     * Busca usuarios activos
     */
    List<User> findByActiveTrue();

    /**
     * Busca usuarios registrados después de una fecha
     */
    List<User> findByRegistrationDateAfter(LocalDateTime date);

    /**
     * Busca usuarios por nombre (búsqueda parcial)
     */
    List<User> findByNameContainingIgnoreCase(String name);

    /**
     * Verifica si existe un usuario con ese email
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si existe un usuario con ese teléfono
     */
    boolean existsByPhone(String phone);

    /**
     * Query personalizada: Usuarios con membresía activa y sin reservas recientes
     */
    @Query("SELECT u FROM User u WHERE u.membershipType != 'NONE' " +
            "AND u.active = true " +
            "AND u.id NOT IN (" +
            "  SELECT DISTINCT b.user.id FROM Booking b " +
            "  WHERE b.createdAt > :sinceDate AND b.status = 'CONFIRMED'" +
            ")")
    List<User> findInactiveUsersWithMembership(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Cuenta usuarios por tipo de membresía
     */
    @Query("SELECT u.membershipType, COUNT(u) FROM User u " +
            "WHERE u.active = true " +
            "GROUP BY u.membershipType")
    List<Object[]> countUsersByMembershipType();
}
