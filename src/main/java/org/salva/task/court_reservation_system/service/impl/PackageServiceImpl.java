package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.dto.request.PackageRequestDTO;
import org.salva.task.court_reservation_system.dto.response.PackageResponseDTO;
import org.salva.task.court_reservation_system.entity.Package;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.mapper.PackageMapper;
import org.salva.task.court_reservation_system.repository.PackageRepository;
import org.salva.task.court_reservation_system.service.PackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PackageServiceImpl implements PackageService {

    private final PackageRepository packageRepository;
    private final PackageMapper packageMapper;

    @Override
    public PackageResponseDTO createPackage(PackageRequestDTO requestDTO) {
        log.info("Creating package with name: {}", requestDTO.getName());

        Package pkg = packageMapper.toEntity(requestDTO);
        pkg = packageRepository.save(pkg);

        log.info("Package created successfully with id: {}", pkg.getId());

        return packageMapper.toResponseDTO(pkg);
    }

    @Override
    @Transactional(readOnly = true)
    public PackageResponseDTO getPackageById(Long id) {
        log.debug("Getting package by id: {}", id);

        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con id: " + id));

        return packageMapper.toResponseDTO(pkg);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponseDTO> getAllActivePackages() {
        log.debug("Getting all active packages");

        List<Package> packages = packageRepository.findByActiveTrue();

        return packageMapper.toResponseDTOList(packages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponseDTO> getPackagesOrderedByBestDiscount() {
        log.debug("Getting packages ordered by best discount");

        List<Package> packages = packageRepository.findAllOrderedByBestDiscount();

        return packageMapper.toResponseDTOList(packages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponseDTO> getPackagesOrderedByBestPrice() {
        log.debug("Getting packages ordered by best price per hour");

        List<Package> packages = packageRepository.findAllOrderedByBestPricePerHour();

        return packageMapper.toResponseDTOList(packages);
    }

    @Override
    public PackageResponseDTO updatePackage(Long id, PackageRequestDTO requestDTO) {
        log.info("Updating package with id: {}", id);

        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con id: " + id));

        packageMapper.updateEntityFromDTO(requestDTO, pkg);
        pkg = packageRepository.save(pkg);

        log.info("Package updated successfully with id: {}", pkg.getId());

        return packageMapper.toResponseDTO(pkg);
    }

    @Override
    public void deactivatePackage(Long id) {
        log.info("Deactivating package with id: {}", id);

        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con id: " + id));

        pkg.setActive(false);
        packageRepository.save(pkg);

        log.info("Package deactivated successfully with id: {}", id);
    }
}
