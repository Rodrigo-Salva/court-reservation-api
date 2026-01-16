package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.dto.request.PackagePurchaseRequestDTO;
import org.salva.task.court_reservation_system.dto.response.UserPackageResponseDTO;
import org.salva.task.court_reservation_system.entity.Package;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.UserPackage;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.mapper.UserPackageMapper;
import org.salva.task.court_reservation_system.repository.PackageRepository;
import org.salva.task.court_reservation_system.repository.UserPackageRepository;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.service.UserPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserPackageServiceImpl implements UserPackageService {

    private final UserPackageRepository userPackageRepository;
    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final UserPackageMapper userPackageMapper;

    @Override
    public UserPackageResponseDTO purchasePackage(PackagePurchaseRequestDTO requestDTO) {
        log.info("User {} purchasing package {}", requestDTO.getUserId(), requestDTO.getPackageId());

        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Package pkg = packageRepository.findById(requestDTO.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado"));

        // Crear UserPackage
        UserPackage userPackage = UserPackage.builder()
                .user(user)
                .packageDetails(pkg)
                .initialHours(pkg.getAmountHours())
                .remainingHours(pkg.getAmountHours())
                .expirationDate(LocalDateTime.now().plusDays(pkg.getValidityDays()))
                .active(true)
                .build();

        userPackage = userPackageRepository.save(userPackage);

        log.info("Package purchased successfully with id: {}", userPackage.getId());

        return userPackageMapper.toResponseDTO(userPackage);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPackageResponseDTO getUserPackageById(Long id) {
        UserPackage userPackage = userPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete de usuario no encontrado"));

        return userPackageMapper.toResponseDTO(userPackage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPackageResponseDTO> getActivePackagesByUser(Long userId) {
        List<UserPackage> packages = userPackageRepository.findByUserIdAndActiveTrue(userId);

        return userPackageMapper.toResponseDTOList(packages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPackageResponseDTO> getAllPackagesByUser(Long userId) {
        List<UserPackage> packages = userPackageRepository.findByUserIdOrderByPurchaseDateDesc(userId);

        return userPackageMapper.toResponseDTOList(packages);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPackageResponseDTO getBestAvailablePackage(Long userId) {
        UserPackage userPackage = userPackageRepository
                .findBestAvailablePackageForUser(userId, LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("No hay paquetes disponibles"));

        return userPackageMapper.toResponseDTO(userPackage);
    }

    @Override
    public void markExpiredPackagesAsInactive() {
        log.info("Marking expired packages as inactive");

        List<UserPackage> expiredPackages = userPackageRepository
                .findExpiredPackagesStillActive(LocalDateTime.now());

        for (UserPackage pkg : expiredPackages) {
            pkg.checkExpiration();
        }

        userPackageRepository.saveAll(expiredPackages);

        log.info("Marked {} packages as inactive", expiredPackages.size());
    }
}
