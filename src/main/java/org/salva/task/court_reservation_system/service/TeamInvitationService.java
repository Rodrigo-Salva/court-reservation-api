package org.salva.task.court_reservation_system.service;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.response.TeamInvitationResponseDTO;
import org.salva.task.court_reservation_system.entity.Team;
import org.salva.task.court_reservation_system.entity.TeamInvitation;
import org.salva.task.court_reservation_system.entity.TeamMember;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.enums.NotificationType;
import org.salva.task.court_reservation_system.enums.TeamInvitationStatus;
import org.salva.task.court_reservation_system.enums.TeamMemberRole;
import org.salva.task.court_reservation_system.exception.BusinessException;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.repository.TeamInvitationRepository;
import org.salva.task.court_reservation_system.repository.TeamMemberRepository;
import org.salva.task.court_reservation_system.repository.TeamRepository;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Invitaciones a equipos: el propietario invita por email y el invitado acepta o rechaza. */
@Service
@RequiredArgsConstructor
@Transactional
public class TeamInvitationService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TeamInvitationResponseDTO invite(Long teamId, String email, User owner) {
        Team team = requireOwner(teamId, owner);
        User invited = userRepository.findByEmail(email.trim())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario activo con ese email"));
        if (invited.getId().equals(owner.getId()) || memberRepository.existsByTeamIdAndUserId(teamId, invited.getId())) {
            throw new ValidationException("El usuario ya pertenece al equipo");
        }
        if (invitationRepository.existsByTeamIdAndInvitedUserIdAndStatus(teamId, invited.getId(), TeamInvitationStatus.PENDIENTE)) {
            throw new ValidationException("Ya existe una invitación pendiente para este usuario");
        }
        TeamInvitation invitation = invitationRepository.save(
                TeamInvitation.builder().team(team).invitedUser(invited).invitedBy(owner).build());
        notificationService.notify(invited, NotificationType.EQUIPO_INVITACION, "Invitación a un equipo",
                owner.getName() + " te invitó a unirte al equipo " + team.getName() + ". Respóndela desde Equipos.");
        return toDto(invitation);
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponseDTO> pendingForTeam(Long teamId, User owner) {
        requireOwner(teamId, owner);
        return invitationRepository.findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, TeamInvitationStatus.PENDIENTE)
                .stream().map(this::toDto).toList();
    }

    public void cancel(Long teamId, Long invitationId, User owner) {
        requireOwner(teamId, owner);
        TeamInvitation invitation = pending(invitationId);
        if (!invitation.getTeam().getId().equals(teamId)) {
            throw new ValidationException("La invitación no pertenece al equipo");
        }
        close(invitation, TeamInvitationStatus.CANCELADA);
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponseDTO> pendingForUser(User user) {
        return invitationRepository.findByInvitedUserIdAndStatusOrderByCreatedAtDesc(user.getId(), TeamInvitationStatus.PENDIENTE)
                .stream().map(this::toDto).toList();
    }

    public void accept(Long invitationId, User user) {
        TeamInvitation invitation = pendingFor(invitationId, user);
        Team team = invitation.getTeam();
        if (!memberRepository.existsByTeamIdAndUserId(team.getId(), user.getId())) {
            memberRepository.save(TeamMember.builder().team(team).user(user).role(TeamMemberRole.MEMBER).build());
        }
        close(invitation, TeamInvitationStatus.ACEPTADA);
        notificationService.notify(team.getOwner(), NotificationType.EQUIPO_INVITACION, "Invitación aceptada",
                user.getName() + " se unió a tu equipo " + team.getName() + ".");
    }

    public void decline(Long invitationId, User user) {
        TeamInvitation invitation = pendingFor(invitationId, user);
        close(invitation, TeamInvitationStatus.RECHAZADA);
        notificationService.notify(invitation.getTeam().getOwner(), NotificationType.EQUIPO_INVITACION, "Invitación rechazada",
                user.getName() + " rechazó unirse a tu equipo " + invitation.getTeam().getName() + ".");
    }

    private Team requireOwner(Long teamId, User user) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
        if (!team.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("No tiene permiso para administrar este equipo");
        }
        return team;
    }

    private TeamInvitation pending(Long invitationId) {
        TeamInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitación no encontrada"));
        if (invitation.getStatus() != TeamInvitationStatus.PENDIENTE) {
            throw new BusinessException("La invitación ya fue respondida o cancelada");
        }
        return invitation;
    }

    private TeamInvitation pendingFor(Long invitationId, User user) {
        TeamInvitation invitation = pending(invitationId);
        if (!invitation.getInvitedUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Esta invitación no es para usted");
        }
        return invitation;
    }

    private void close(TeamInvitation invitation, TeamInvitationStatus status) {
        invitation.setStatus(status);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    private TeamInvitationResponseDTO toDto(TeamInvitation i) {
        return TeamInvitationResponseDTO.builder().id(i.getId()).teamId(i.getTeam().getId()).teamName(i.getTeam().getName())
                .invitedName(i.getInvitedUser().getName()).invitedEmail(i.getInvitedUser().getEmail())
                .invitedByName(i.getInvitedBy().getName()).status(i.getStatus()).createdAt(i.getCreatedAt()).build();
    }
}
