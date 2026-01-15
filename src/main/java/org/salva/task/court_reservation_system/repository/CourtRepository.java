package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.enums.SportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operaciones de base de datos de Court
 */
@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    /**
     * Busca todas las canchas activas
     * Query generado: SELECT * FROM courts WHERE activa = true
     */
    List<Court> findByActiveTrue();

    /**
     * Busca canchas por tipo de deporte
     * Query: SELECT * FROM courts WHERE tipo_deporte = ? AND activa = true
     */
    List<Court> findBySportTypeAndActiveTrue(SportType sportType);

    /**
     * Busca canchas por nombre (búsqueda parcial, case insensitive)
     * Query: SELECT * FROM courts WHERE LOWER(nombre) LIKE LOWER(?)
     */
    List<Court> findByNameContainingIgnoreCase(String name);

    /**
     * Busca canchas con precio base menor o igual al especificado
     */
    List<Court> findByBasePricePerHourLessThanEqualAndActiveTrue(BigDecimal maxPrice);

    /**
     * Busca canchas por capacidad mínima
     */
    List<Court> findByCapacityGreaterThanEqualAndActiveTrue(Integer minCapacity);

    /**
     * Query personalizada: Busca canchas disponibles por deporte y rango de precio
     */
    @Query("SELECT c FROM Court c WHERE c.sportType = :sportType " +
            "AND c.priceBaseHour BETWEEN :minPrice AND :maxPrice " +
            "AND c.active = true " +
            "ORDER BY c.priceBaseHour ASC")
    List<Court> findAvailableCourtsBySportAndPriceRange(
            @Param("sportType") SportType sportType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * Verifica si existe una cancha activa con ese nombre
     */
    boolean existsByNameAndActiveTrue(String name);
}
