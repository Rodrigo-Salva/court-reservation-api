package org.salva.task.court_reservation_system.mapper;

import org.salva.task.court_reservation_system.dto.response.UserPackageResponseDTO;
import org.salva.task.court_reservation_system.entity.UserPackage;
import org.mapstruct.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper para convertir entre UserPackage entity y DTOs
 */
@Mapper(componentModel = "spring")
public interface UserPackageMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "packageDetails.id", target = "packageId")
    @Mapping(source = "packageDetails.name", target = "packageName")
    @Mapping(target = "usedHours", expression = "java(calculateUsedHours(userPackage))")
    @Mapping(target = "daysUntilExpiration", expression = "java(calculateDaysUntilExpiration(userPackage))")
    @Mapping(target = "isExpired", expression = "java(userPackage.isExpired())")
    UserPackageResponseDTO toResponseDTO(UserPackage userPackage);

    List<UserPackageResponseDTO> toResponseDTOList(List<UserPackage> userPackages);

    /**
     * Calcula horas usadas
     */
    default Integer calculateUsedHours(UserPackage userPackage) {
        return userPackage.getInitialHours() - userPackage.getRemainingHours();
    }

    /**
     * Calcula días hasta vencimiento
     */
    default Long calculateDaysUntilExpiration(UserPackage userPackage) {
        if (userPackage.getExpirationDate() == null) {
            return null;
        }
        Duration duration = Duration.between(LocalDateTime.now(), userPackage.getExpirationDate());
        long days = duration.toDays();
        return days < 0 ? 0 : days;
    }
}
