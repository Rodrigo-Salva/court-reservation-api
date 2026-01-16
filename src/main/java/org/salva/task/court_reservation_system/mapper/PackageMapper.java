package org.salva.task.court_reservation_system.mapper;

import org.salva.task.court_reservation_system.dto.request.PackageRequestDTO;
import org.salva.task.court_reservation_system.dto.response.PackageResponseDTO;
import org.salva.task.court_reservation_system.entity.Package;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Mapper para convertir entre Package entity y DTOs
 */
@Mapper(componentModel = "spring")
public interface PackageMapper {

    /**
     * Convierte entidad a DTO con campos calculados
     */
    @Mapping(target = "pricePerHour", expression = "java(calculatePricePerHour(pkg))")
    @Mapping(target = "savings", expression = "java(calculateSavings(pkg))")
    PackageResponseDTO toResponseDTO(Package pkg);

    List<PackageResponseDTO> toResponseDTOList(List<Package> packages);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    Package toEntity(PackageRequestDTO requestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(PackageRequestDTO requestDTO, @MappingTarget Package pkg);

    /**
     * Calcula el precio por hora del paquete
     */
    default BigDecimal calculatePricePerHour(Package pkg) {
        if (pkg.getAmountHours() == null || pkg.getAmountHours() == 0) {
            return BigDecimal.ZERO;
        }
        return pkg.getPrice()
                .divide(new BigDecimal(pkg.getAmountHours()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el ahorro (ejemplo: comparado con precio base de S/. 50/hora)
     */
    default BigDecimal calculateSavings(Package pkg) {
        BigDecimal regularPrice = new BigDecimal("50.00"); // Precio regular por hora
        BigDecimal totalRegularPrice = regularPrice.multiply(new BigDecimal(pkg.getAmountHours()));
        return totalRegularPrice.subtract(pkg.getPrice());
    }
}
