package org.salva.task.court_reservation_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.TeamInvitationRequestDTO;
import org.salva.task.court_reservation_system.dto.request.TeamRequestDTO;
import org.salva.task.court_reservation_system.dto.response.TeamInvitationResponseDTO;
import org.salva.task.court_reservation_system.dto.response.TeamMemberResponseDTO;
import org.salva.task.court_reservation_system.dto.response.TeamResponseDTO;
import org.salva.task.court_reservation_system.entity.*;
import org.salva.task.court_reservation_system.enums.TeamMemberRole;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.repository.*;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.salva.task.court_reservation_system.service.TeamInvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/teams") @RequiredArgsConstructor
public class TeamController {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamInvitationService invitationService;

    /** Equipos de los que el usuario es propietario o miembro. */
    @GetMapping
    public List<TeamResponseDTO> myTeams(@AuthenticationPrincipal CustomUserDetails user) {
        return memberRepository.findByUserId(user.getId()).stream().map(member -> toDto(member.getTeam())).toList();
    }

    @PostMapping
    public ResponseEntity<TeamResponseDTO> create(@Valid @RequestBody TeamRequestDTO request, @AuthenticationPrincipal CustomUserDetails user) {
        Team team = teamRepository.save(Team.builder().name(request.getName()).description(request.getDescription()).owner(user.getUser()).build());
        memberRepository.save(TeamMember.builder().team(team).user(user.getUser()).role(TeamMemberRole.OWNER).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(team));
    }

    @GetMapping("/{id}/members")
    public List<TeamMemberResponseDTO> members(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        requireMember(id, user);
        return memberRepository.findByTeamId(id).stream()
                .map(m -> TeamMemberResponseDTO.builder().userId(m.getUser().getId()).name(m.getUser().getName()).role(m.getRole()).build()).toList();
    }

    @DeleteMapping("/{id}/members/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void removeMember(@PathVariable Long id, @PathVariable Long userId, @AuthenticationPrincipal CustomUserDetails user) {
        Team team = requireOwner(id, user);
        if (team.getOwner().getId().equals(userId)) throw new ValidationException("No puede eliminar al propietario del equipo");
        memberRepository.deleteByTeamIdAndUserId(id, userId);
    }

    @DeleteMapping("/{id}/leave") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void leave(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        Team team = requireMember(id, user);
        if (team.getOwner().getId().equals(user.getId())) throw new ValidationException("El propietario no puede abandonar su equipo");
        memberRepository.deleteByTeamIdAndUserId(id, user.getId());
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<TeamInvitationResponseDTO> invite(@PathVariable Long id, @Valid @RequestBody TeamInvitationRequestDTO request, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.invite(id, request.getEmail(), user.getUser()));
    }

    @GetMapping("/{id}/invitations")
    public List<TeamInvitationResponseDTO> teamInvitations(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return invitationService.pendingForTeam(id, user.getUser());
    }

    @DeleteMapping("/{id}/invitations/{invitationId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvitation(@PathVariable Long id, @PathVariable Long invitationId, @AuthenticationPrincipal CustomUserDetails user) {
        invitationService.cancel(id, invitationId, user.getUser());
    }

    @GetMapping("/invitations/mine")
    public List<TeamInvitationResponseDTO> myInvitations(@AuthenticationPrincipal CustomUserDetails user) {
        return invitationService.pendingForUser(user.getUser());
    }

    @PostMapping("/invitations/{invitationId}/accept") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@PathVariable Long invitationId, @AuthenticationPrincipal CustomUserDetails user) {
        invitationService.accept(invitationId, user.getUser());
    }

    @PostMapping("/invitations/{invitationId}/decline") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable Long invitationId, @AuthenticationPrincipal CustomUserDetails user) {
        invitationService.decline(invitationId, user.getUser());
    }

    private Team requireOwner(Long id, CustomUserDetails user) {
        Team team = findTeam(id);
        if (!team.getOwner().getId().equals(user.getId())) throw new AccessDeniedException("No tiene permiso para administrar este equipo");
        return team;
    }

    private Team requireMember(Long id, CustomUserDetails user) {
        Team team = findTeam(id);
        if (!memberRepository.existsByTeamIdAndUserId(id, user.getId())) throw new AccessDeniedException("No pertenece a este equipo");
        return team;
    }

    private Team findTeam(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
    }

    private TeamResponseDTO toDto(Team t) {
        return TeamResponseDTO.builder().id(t.getId()).name(t.getName()).description(t.getDescription()).ownerId(t.getOwner().getId())
                .ownerName(t.getOwner().getName()).memberCount((int) memberRepository.countByTeamId(t.getId())).build();
    }
}
