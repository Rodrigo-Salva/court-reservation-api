package org.salva.task.court_reservation_system.service;

import org.salva.task.court_reservation_system.dto.request.CourtRequestDTO;
import org.salva.task.court_reservation_system.dto.response.CourtResponseDTO;
import org.salva.task.court_reservation_system.enums.SportType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface para servicios de Court (Cancha)
 */
public interface CourtService {

    /**
     * Crea una nueva cancha
     */
    CourtResponseDTO createCourt(CourtRequestDTO requestDTO);

    /**
     * Obtiene una cancha por ID
     */
    CourtResponseDTO getCourtById(Long id);

    /**
     * Obtiene todas las canchas activas
     */
    List<CourtResponseDTO> getAllActiveCourts();

    /**
     * Obtiene canchas por tipo de deporte
     */
    List<CourtResponseDTO> getCourtsBySportType(SportType sportType);

    /**
     * Busca canchas por nombre
     */
    List<CourtResponseDTO> searchCourtsByName(String name);

    /**
     * Obtiene canchas por rango de precio
     */
    List<CourtResponseDTO> getCourtsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Actualiza una cancha existente
     */
    CourtResponseDTO updateCourt(Long id, CourtRequestDTO requestDTO);

    /**
     * Desactiva una cancha (soft delete)
     */
    void deactivateCourt(Long id);

    /**
     * Activa una cancha
     */
    void activateCourt(Long id);
}

