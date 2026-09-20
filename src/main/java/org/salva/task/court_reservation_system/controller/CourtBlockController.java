package org.salva.task.court_reservation_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.CourtBlockRequestDTO;
import org.salva.task.court_reservation_system.dto.response.CourtBlockResponseDTO;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.CourtBlock;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.repository.CourtBlockRepository;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/court-blocks") @RequiredArgsConstructor
public class CourtBlockController {
    private final CourtBlockRepository blockRepository;
    private final CourtRepository courtRepository;
    private final AccessControlService accessControl;

    @GetMapping("/court/{courtId}")
    public List<CourtBlockResponseDTO> list(@PathVariable Long courtId, @RequestParam LocalDate date,
                                            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Court court = courtRepository.findById(courtId).orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        accessControl.requireSameVenueOrAdmin(venueIdOf(court), currentUser);
        return blockRepository.findByCourtIdAndBlockDateAndActiveTrue(courtId, date).stream().map(this::toDto).toList();
    }
    @PostMapping
    public ResponseEntity<CourtBlockResponseDTO> create(@Valid @RequestBody CourtBlockRequestDTO request,
                                                        @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (!request.getStartTime().isBefore(request.getEndTime())) throw new ValidationException("La hora de inicio debe ser anterior a la hora de fin");
        Court court = courtRepository.findById(request.getCourtId()).orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        accessControl.requireSameVenueOrAdmin(venueIdOf(court), currentUser);
        CourtBlock block = CourtBlock.builder().court(court).blockDate(request.getBlockDate()).startTime(request.getStartTime())
                .endTime(request.getEndTime()).type(request.getType()).reason(request.getReason()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(blockRepository.save(block)));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        CourtBlock block = blockRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado"));
        accessControl.requireSameVenueOrAdmin(venueIdOf(block.getCourt()), currentUser);
        block.setActive(false); blockRepository.save(block);
    }
    private Long venueIdOf(Court court) { return court.getVenue() != null ? court.getVenue().getId() : null; }
    private CourtBlockResponseDTO toDto(CourtBlock b) { return CourtBlockResponseDTO.builder().id(b.getId()).courtId((long) b.getCourt().getId())
            .blockDate(b.getBlockDate()).startTime(b.getStartTime()).endTime(b.getEndTime()).type(b.getType()).reason(b.getReason()).active(b.getActive()).build(); }
}
