package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.UserPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operaciones de base de datos de UserPackage
 */
@Repository
public interface UserPackageRepository extends JpaRepository<UserPackage, Long> {

    /**
     * Busca paquetes activos de un usuario
     */
    List<UserPackage> findByUserIdAndActiveTrue(Long userId);

    /**
     * Busca todos los paquetes de un usuario (activos e inactivos)
     */
    List<UserPackage> findByUserIdOrderByPurchaseDateDesc(Long userId);

    /**
     * Busca el paquete activo con más horas disponibles de un usuario
     */
    @Query("SELECT up FROM UserPackage up WHERE up.user.id = :userId " +
            "AND up.active = true " +
            "AND up.expirationDate > :now " +
            "AND up.remainingHours > 0 " +
            "ORDER BY up.remainingHours DESC")
    Optional<UserPackage> findBestAvailablePackageForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    /**
     * Busca paquetes que vencen pronto (para notificaciones)
     */
    @Query("SELECT up FROM UserPackage up WHERE up.active = true " +
            "AND up.expirationDate BETWEEN :now AND :expirationLimit " +
            "AND up.remainingHours > 0")
    List<UserPackage> findPackagesExpiringSoon(
            @Param("now") LocalDateTime now,
            @Param("expirationLimit") LocalDateTime expirationLimit
    );

    /**
     * Busca paquetes vencidos que aún están marcados como activos
     * (para job de limpieza)
     */
    @Query("SELECT up FROM UserPackage up WHERE up.active = true " +
            "AND up.expirationDate < :now")
    List<UserPackage> findExpiredPackagesStillActive(@Param("now") LocalDateTime now);

    /**
     * Cuenta paquetes activos de un usuario
     */
    long countByUserIdAndActiveTrue(Long userId);
}
