package org.salva.task.court_reservation_system.repository;

import org.salva.task.court_reservation_system.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository para operaciones de base de datos de Package
 */
@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {

    /**
     * Busca todos los paquetes activos
     */
    List<Package> findByActiveTrue();

    /**
     * Busca paquetes por rango de horas
     */
    List<Package> findByAmountHoursBetweenAndActiveTrue(Integer minHours, Integer maxHours);

    /**
     * Busca paquetes ordenados por mejor descuento
     */
    @Query("SELECT p FROM Package p WHERE p.active = true " +
            "ORDER BY p.discountPercent DESC")
    List<Package> findAllOrderedByBestDiscount();

    /**
     * Busca paquetes ordenados por mejor precio por hora
     */
    @Query("SELECT p FROM Package p WHERE p.active = true " +
            "ORDER BY (p.price / p.amountHours) ASC")
    List<Package> findAllOrderedByBestPricePerHour();

    /**
     * Busca paquetes con descuento mayor o igual al especificado
     */
    List<Package> findByDiscountPercentGreaterThanEqualAndActiveTrue(BigDecimal minDiscount);
}
