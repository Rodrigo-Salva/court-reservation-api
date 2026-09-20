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
    @Mapping(target = "basePricePerHour", source = "priceBaseHour")
    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "venueName", source = "venue.name")
    CourtResponseDTO toResponseDTO(Court court);

    /**
     * Convierte lista de entidades a lista de DTOs
     */
    List<CourtResponseDTO> toResponseDTOList(List<Court> courts);

    /**
     * Convierte DTO de request a entidad (para crear)
     */
    @Mapping(target = "priceBaseHour", source = "basePricePerHour")
    @Mapping(target = "venue", ignore = true)
    Court toEntity(CourtRequestDTO requestDTO);

    /**
     * Actualiza una entidad existente con datos del DTO (para update)
     * @param requestDTO DTO con nuevos datos
     * @param court Entidad existente a actualizar
     */
    @Mapping(target = "priceBaseHour", source = "basePricePerHour")
    @Mapping(target = "venue", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(CourtRequestDTO requestDTO, @MappingTarget Court court);
}
