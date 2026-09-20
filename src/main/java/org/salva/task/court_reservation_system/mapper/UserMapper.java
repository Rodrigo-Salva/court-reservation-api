package org.salva.task.court_reservation_system.mapper;

import org.salva.task.court_reservation_system.dto.request.UserRequestDTO;
import org.salva.task.court_reservation_system.dto.response.UserResponseDTO;
import org.salva.task.court_reservation_system.entity.User;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para convertir entre User entity y DTOs
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Convierte entidad a DTO de respuesta
     * Agrega campos calculados del enum de membresía
     */
    @Mapping(source = "membershipType.discountPercentage", target = "membershipDiscount")
    @Mapping(source = "membershipType.maxDaysAdvance", target = "maxDaysAdvance")
    @Mapping(source = "venue.id", target = "venueId")
    @Mapping(source = "venue.name", target = "venueName")
    UserResponseDTO toResponseDTO(User user);

    List<UserResponseDTO> toResponseDTOList(List<User> users);

    /**
     * Convierte DTO de request a entidad
     * No mapea registrationDate (se genera automáticamente con @PrePersist)
     */
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "venue", ignore = true)
    User toEntity(UserRequestDTO requestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "venue", ignore = true)
    void updateEntityFromDTO(UserRequestDTO requestDTO, @MappingTarget User user);
}
