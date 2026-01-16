package org.salva.task.court_reservation_system.service;

import org.salva.task.court_reservation_system.dto.request.PackageRequestDTO;
import org.salva.task.court_reservation_system.dto.response.PackageResponseDTO;

import java.util.List;

/**
 * Interface para servicios de Package (Paquete)
 */
public interface PackageService {

    /**
     * Crea un nuevo paquete
     */
    PackageResponseDTO createPackage(PackageRequestDTO requestDTO);

    /**
     * Obtiene un paquete por ID
     */
    PackageResponseDTO getPackageById(Long id);

    /**
     * Obtiene todos los paquetes activos
     */
    List<PackageResponseDTO> getAllActivePackages();

    /**
     * Obtiene paquetes ordenados por mejor descuento
     */
    List<PackageResponseDTO> getPackagesOrderedByBestDiscount();

    /**
     * Obtiene paquetes ordenados por mejor precio por hora
     */
    List<PackageResponseDTO> getPackagesOrderedByBestPrice();

    /**
     * Actualiza un paquete existente
     */
    PackageResponseDTO updatePackage(Long id, PackageRequestDTO requestDTO);

    /**
     * Desactiva un paquete
     */
    void deactivatePackage(Long id);
}
