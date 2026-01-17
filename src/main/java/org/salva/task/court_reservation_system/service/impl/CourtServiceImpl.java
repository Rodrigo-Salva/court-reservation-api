package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.dto.request.CourtRequestDTO;
import org.salva.task.court_reservation_system.dto.response.CourtResponseDTO;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.enums.SportType;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.mapper.CourtMapper;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.service.CourtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementación del servicio de Court
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourtServiceImpl implements CourtService {

    private final CourtRepository courtRepository;
    private final CourtMapper courtMapper;

    @Override
    public CourtResponseDTO createCourt(CourtRequestDTO requestDTO) {
        log.info("Creating court with name: {}", requestDTO.getName());

        // Validar que no exista una cancha con el mismo nombre
        if (courtRepository.existsByNameAndActiveTrue(requestDTO.getName())) {
            throw new ValidationException("Ya existe una cancha activa con el nombre: " + requestDTO.getName());
        }

        // Mapper: DTO → Entity
        Court court = courtMapper.toEntity(requestDTO);

        // Guardar
        court = courtRepository.save(court);

        log.info("Court created successfully with id: {}", court.getId());

        // Mapper: Entity → DTO
        return courtMapper.toResponseDTO(court);
    }

    @Override
    @Transactional(readOnly = true)
    public CourtResponseDTO getCourtById(Long id) {
        log.debug("Getting court by id: {}", id);

        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada con id: " + id));

        return courtMapper.toResponseDTO(court);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponseDTO> getAllActiveCourts() {
        log.debug("Getting all active courts");

        List<Court> courts = courtRepository.findByActiveTrue();

        return courtMapper.toResponseDTOList(courts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponseDTO> getCourtsBySportType(SportType sportType) {
        log.debug("Getting courts by sport type: {}", sportType);

        List<Court> courts = courtRepository.findBySportTypeAndActiveTrue(sportType);

        return courtMapper.toResponseDTOList(courts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponseDTO> searchCourtsByName(String name) {
        log.debug("Searching courts by name: {}", name);

        List<Court> courts = courtRepository.findByNameContainingIgnoreCase(name);

        return courtMapper.toResponseDTOList(courts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponseDTO> getCourtsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Getting courts by price range: {} - {}", minPrice, maxPrice);

        if (minPrice.compareTo(maxPrice) > 0) {
            throw new ValidationException("El precio mínimo no puede ser mayor al precio máximo");
        }

        // Aquí podrías usar el query personalizado del repository
        List<Court> courts = courtRepository.findByPriceBaseHourLessThanEqualAndActiveTrue(maxPrice);

        return courtMapper.toResponseDTOList(courts);
    }

    @Override
    public CourtResponseDTO updateCourt(Long id, CourtRequestDTO requestDTO) {
        log.info("Updating court with id: {}", id);

        // Buscar cancha existente
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada con id: " + id));

        // Validar nombre único (si cambió)
        if (!court.getName().equals(requestDTO.getName()) &&
                courtRepository.existsByNameAndActiveTrue(requestDTO.getName())) {
            throw new ValidationException("Ya existe una cancha activa con el nombre: " + requestDTO.getName());
        }

        // Mapper: actualiza solo campos no-null
        courtMapper.updateEntityFromDTO(requestDTO, court);

        // Guardar
        court = courtRepository.save(court);

        log.info("Court updated successfully with id: {}", court.getId());

        return courtMapper.toResponseDTO(court);
    }

    @Override
    public void deactivateCourt(Long id) {
        log.info("Deactivating court with id: {}", id);

        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada con id: " + id));

        court.setActive(false);
        courtRepository.save(court);

        log.info("Court deactivated successfully with id: {}", id);
    }

    @Override
    public void activateCourt(Long id) {
        log.info("Activating court with id: {}", id);

        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada con id: " + id));

        court.setActive(true);
        courtRepository.save(court);

        log.info("Court activated successfully with id: {}", id);
    }
}
