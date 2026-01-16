package org.salva.task.court_reservation_system.mapper;

import org.salva.task.court_reservation_system.dto.request.CourtRequestDTO;
import org.salva.task.court_reservation_system.dto.response.CourtResponseDTO;
import org.salva.task.court_reservation_system.entity.Court;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para convertir entre Court entity y DTOs
 */
@Mapper(componentModel = "spring")
public interface CourtMapper {

    /**
     * Convierte entidad a DTO de respuesta
     */
    CourtResponseDTO toResponseDTO(Court court);

    /**
     * Convierte lista de entidades a lista de DTOs
     */
    List<CourtResponseDTO> toResponseDTOList(List<Court> courts);

    /**
     * Convierte DTO de request a entidad (para crear)
     */
    Court toEntity(CourtRequestDTO requestDTO);

    /**
     * Actualiza una entidad existente con datos del DTO (para update)
     * @param requestDTO DTO con nuevos datos
     * @param court Entidad existente a actualizar
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(CourtRequestDTO requestDTO, @MappingTarget Court court);
}
