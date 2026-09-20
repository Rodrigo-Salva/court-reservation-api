package org.salva.task.court_reservation_system.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.OpenMatchRequestDTO;
import org.salva.task.court_reservation_system.dto.response.OpenMatchResponseDTO;
import org.salva.task.court_reservation_system.entity.*;
import org.salva.task.court_reservation_system.enums.*;
import org.salva.task.court_reservation_system.exception.*;
import org.salva.task.court_reservation_system.repository.*;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/open-matches") @RequiredArgsConstructor
public class OpenMatchController {
 private final OpenMatchRepository matchRepository; private final OpenMatchJoinRequestRepository requestRepository; private final BookingRepository bookingRepository;
 @GetMapping public List<OpenMatchResponseDTO> list(){return matchRepository.findByStatus(OpenMatchStatus.ABIERTO).stream().map(this::dto).toList();}
 @GetMapping("/mine") public List<OpenMatchResponseDTO> mine(@AuthenticationPrincipal CustomUserDetails user){return matchRepository.findByCreatorIdOrderByIdDesc(user.getId()).stream().map(this::dto).toList();}
 @PostMapping public ResponseEntity<OpenMatchResponseDTO> create(@Valid @RequestBody OpenMatchRequestDTO req,@AuthenticationPrincipal CustomUserDetails user){
  Booking booking=bookingRepository.findById(req.getBookingId()).orElseThrow(()->new ResourceNotFoundException("Reserva no encontrada"));
  if(!booking.getUser().getId().equals(user.getId()))throw new org.springframework.security.access.AccessDeniedException("Solo el titular puede publicar la reserva");
  if(booking.getStatus()!=BookingStatus.CONFIRMADA)throw new BusinessException("La reserva debe estar confirmada");
  if(matchRepository.existsByBookingId(booking.getId()))throw new ValidationException("Esta reserva ya fue publicada");
  OpenMatch match=matchRepository.save(OpenMatch.builder().booking(booking).creator(booking.getUser()).maxPlayers(req.getMaxPlayers()).note(req.getNote()).build());
  return ResponseEntity.status(HttpStatus.CREATED).body(dto(match)); }
 @PostMapping("/{id}/join") public ResponseEntity<Void> join(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails user){OpenMatch match=open(id); if(match.getCreator().getId().equals(user.getId()))throw new ValidationException("El creador ya participa"); if(match.getStatus()!=OpenMatchStatus.ABIERTO)throw new BusinessException("El partido no acepta jugadores"); if(requestRepository.existsByOpenMatchIdAndUserId(id,user.getId()))throw new ValidationException("Ya envió una solicitud"); requestRepository.save(OpenMatchJoinRequest.builder().openMatch(match).user(user.getUser()).build()); return ResponseEntity.status(HttpStatus.CREATED).build();}
 @GetMapping("/{id}/requests") public List<Map<String,Object>> requests(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails user){OpenMatch match=requireCreator(id,user); return requestRepository.findByOpenMatchId(id).stream().map(r->Map.<String,Object>of("id",r.getId(),"userId",r.getUser().getId(),"name",r.getUser().getName(),"status",r.getStatus())).toList();}
 @PatchMapping("/{id}/requests/{requestId}/accept") public void accept(@PathVariable Long id,@PathVariable Long requestId,@AuthenticationPrincipal CustomUserDetails user){OpenMatch match=requireCreator(id,user); OpenMatchJoinRequest request=requestRepository.findById(requestId).orElseThrow(()->new ResourceNotFoundException("Solicitud no encontrada")); if(!request.getOpenMatch().getId().equals(id))throw new ValidationException("Solicitud inválida"); long players=requestRepository.countByOpenMatchIdAndStatus(id,JoinRequestStatus.ACEPTADA)+1; if(players>=match.getMaxPlayers()-1)match.setStatus(OpenMatchStatus.COMPLETO); request.setStatus(JoinRequestStatus.ACEPTADA); requestRepository.save(request); matchRepository.save(match);}
 @PatchMapping("/{id}/requests/{requestId}/reject") public void reject(@PathVariable Long id,@PathVariable Long requestId,@AuthenticationPrincipal CustomUserDetails user){requireCreator(id,user); OpenMatchJoinRequest request=requestRepository.findById(requestId).orElseThrow(()->new ResourceNotFoundException("Solicitud no encontrada")); if(!request.getOpenMatch().getId().equals(id))throw new ValidationException("Solicitud inválida"); request.setStatus(JoinRequestStatus.RECHAZADA); requestRepository.save(request);}
 private OpenMatch open(Long id){return matchRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Partido no encontrado"));}
 private OpenMatch requireCreator(Long id,CustomUserDetails user){OpenMatch m=open(id);if(!m.getCreator().getId().equals(user.getId()))throw new org.springframework.security.access.AccessDeniedException("Solo el creador puede gestionar el partido");return m;}
 private OpenMatchResponseDTO dto(OpenMatch m){long confirmed=requestRepository.countByOpenMatchIdAndStatus(m.getId(),JoinRequestStatus.ACEPTADA)+1;return OpenMatchResponseDTO.builder().id(m.getId()).bookingId(m.getBooking().getId()).courtId((long)m.getBooking().getCourt().getId()).courtName(m.getBooking().getCourt().getName()).date(m.getBooking().getBookingDate()).startTime(m.getBooking().getStartTime()).endTime(m.getBooking().getEndTime()).creatorName(m.getCreator().getName()).maxPlayers(m.getMaxPlayers()).confirmedPlayers(confirmed).note(m.getNote()).status(m.getStatus()).build();}
}
