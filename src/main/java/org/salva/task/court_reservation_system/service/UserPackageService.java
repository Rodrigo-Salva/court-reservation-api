package org.salva.task.court_reservation_system.service;

import org.salva.task.court_reservation_system.dto.request.PackagePurchaseRequestDTO;
import org.salva.task.court_reservation_system.dto.response.UserPackageResponseDTO;

import java.util.List;

/**
 * Interface para servicios de UserPackage (Paquete de Usuario)
 */
public interface UserPackageService {

    /**
     * Compra un paquete para un usuario
     */
    UserPackageResponseDTO purchasePackage(PackagePurchaseRequestDTO requestDTO);

    /**
     * Obtiene un paquete de usuario por ID
     */
    UserPackageResponseDTO getUserPackageById(Long id);

    /**
     * Obtiene todos los paquetes activos de un usuario
     */
    List<UserPackageResponseDTO> getActivePackagesByUser(Long userId);

    /**
     * Obtiene todos los paquetes de un usuario (activos e inactivos)
     */
    List<UserPackageResponseDTO> getAllPackagesByUser(Long userId);

    /**
     * Obtiene el mejor paquete disponible para un usuario
     */
    UserPackageResponseDTO getBestAvailablePackage(Long userId);

    /**
     * Marca paquetes vencidos como inactivos (job automático)
     */
    void markExpiredPackagesAsInactive();
}
