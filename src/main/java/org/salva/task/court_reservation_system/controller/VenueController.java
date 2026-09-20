package org.salva.task.court_reservation_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.VenueRequestDTO;
import org.salva.task.court_reservation_system.dto.response.VenueResponseDTO;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.repository.VenueRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/venues") @RequiredArgsConstructor
public class VenueController {
    private final VenueRepository venueRepository;

    @GetMapping public List<VenueResponseDTO> list() { return venueRepository.findByActiveTrue().stream().map(this::toDto).toList(); }
    @GetMapping("/all") public List<VenueResponseDTO> listAll() { return venueRepository.findAll().stream().map(this::toDto).toList(); }
    @PostMapping public ResponseEntity<VenueResponseDTO> create(@Valid @RequestBody VenueRequestDTO request) {
        if (venueRepository.existsByNameIgnoreCase(request.getName())) throw new ValidationException("Ya existe una sede con ese nombre");
        Venue venue = venueRepository.save(Venue.builder().name(request.getName()).address(request.getAddress()).phone(request.getPhone()).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(venue));
    }
    @PutMapping("/{id}") public VenueResponseDTO update(@PathVariable Long id, @Valid @RequestBody VenueRequestDTO request) {
        Venue venue = venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        venue.setName(request.getName()); venue.setAddress(request.getAddress()); venue.setPhone(request.getPhone());
        return toDto(venueRepository.save(venue));
    }
    @PatchMapping("/{id}/activate") public void activate(@PathVariable Long id) { setActive(id, true); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deactivate(@PathVariable Long id) { setActive(id, false); }
    private void setActive(Long id, boolean active) { Venue venue = venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada")); venue.setActive(active); venueRepository.save(venue); }
    private VenueResponseDTO toDto(Venue v) { return VenueResponseDTO.builder().id(v.getId()).name(v.getName()).address(v.getAddress()).phone(v.getPhone()).active(v.getActive()).build(); }
}
