package org.salva.task.court_reservation_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.CourtReviewRequestDTO;
import org.salva.task.court_reservation_system.dto.response.CourtReviewResponseDTO;
import org.salva.task.court_reservation_system.entity.*;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.exception.*;
import org.salva.task.court_reservation_system.repository.*;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.salva.task.court_reservation_system.service.CourtReviewModerationService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/court-reviews") @RequiredArgsConstructor
public class CourtReviewController {
 private final CourtReviewRepository reviewRepository; private final CourtRepository courtRepository; private final BookingRepository bookingRepository;
 private final CourtReviewModerationService moderationService;
 @GetMapping("/court/{courtId}") public List<CourtReviewResponseDTO> list(@PathVariable Long courtId) { return reviewRepository.findByCourtIdAndHiddenFalseOrderByCreatedAtDesc(courtId).stream().map(this::dto).toList(); }
 @GetMapping("/moderation") public List<CourtReviewResponseDTO> moderation(@AuthenticationPrincipal CustomUserDetails user) { return moderationService.list(user); }
 @PatchMapping("/{id}/hide") public CourtReviewResponseDTO hide(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) { return moderationService.setHidden(id, true, user); }
 @PatchMapping("/{id}/show") public CourtReviewResponseDTO show(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) { return moderationService.setHidden(id, false, user); }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) { moderationService.delete(id, user); }
 @PostMapping public ResponseEntity<CourtReviewResponseDTO> create(@Valid @RequestBody CourtReviewRequestDTO request,@AuthenticationPrincipal CustomUserDetails user) {
  Court court=courtRepository.findById(request.getCourtId()).orElseThrow(()->new ResourceNotFoundException("Cancha no encontrada"));
  if(!bookingRepository.existsByCourtIdAndUserIdAndStatus((long) court.getId(),user.getId(),BookingStatus.COMPLETADA)) throw new BusinessException("Solo puede calificar una cancha después de completar una reserva");
  if(reviewRepository.existsByCourtIdAndUserId((long) court.getId(),user.getId())) throw new ValidationException("Ya calificó esta cancha");
  CourtReview review=reviewRepository.save(CourtReview.builder().court(court).user(user.getUser()).rating(request.getRating()).comment(request.getComment()).build());
  return ResponseEntity.status(HttpStatus.CREATED).body(dto(review)); }
 private CourtReviewResponseDTO dto(CourtReview r){return moderationService.toDto(r);}
}
