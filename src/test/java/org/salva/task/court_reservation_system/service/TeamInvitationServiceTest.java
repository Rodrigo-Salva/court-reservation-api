package org.salva.task.court_reservation_system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamInvitationServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository memberRepository;
    @Mock private TeamInvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    private TeamInvitationService service;
    private final User owner = user(1L, "Capitana", "cap@test.com");
    private final User guest = user(2L, "Invitado", "guest@test.com");
    private final Team team = Team.builder().id(10L).name("Los Cracks").owner(owner).build();

    @BeforeEach
    void setUp() {
        service = new TeamInvitationService(teamRepository, memberRepository, invitationRepository, userRepository, notificationService);
    }

    @Test
    void ownerInvitesExistingUserByEmailAndUserIsNotified() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findByEmail("guest@test.com")).thenReturn(Optional.of(guest));
        when(memberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(false);
        when(invitationRepository.existsByTeamIdAndInvitedUserIdAndStatus(10L, 2L, TeamInvitationStatus.PENDIENTE)).thenReturn(false);
        when(invitationRepository.save(any(TeamInvitation.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.invite(10L, " guest@test.com ", owner);

        assertEquals(TeamInvitationStatus.PENDIENTE, result.getStatus());
        assertEquals("guest@test.com", result.getInvitedEmail());
        verify(notificationService).notify(eq(guest), eq(NotificationType.EQUIPO_INVITACION), any(), any());
    }

    @Test
    void inviteRejectsNonOwnerUnknownEmailExistingMemberAndDuplicates() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThrows(AccessDeniedException.class, () -> service.invite(10L, "guest@test.com", guest));

        when(userRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.invite(10L, "nadie@test.com", owner));

        when(userRepository.findByEmail("guest@test.com")).thenReturn(Optional.of(guest));
        when(memberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(true);
        assertThrows(ValidationException.class, () -> service.invite(10L, "guest@test.com", owner));

        when(memberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(false);
        when(invitationRepository.existsByTeamIdAndInvitedUserIdAndStatus(10L, 2L, TeamInvitationStatus.PENDIENTE)).thenReturn(true);
        assertThrows(ValidationException.class, () -> service.invite(10L, "guest@test.com", owner));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void invitedUserAcceptingBecomesMemberAndOwnerIsNotified() {
        TeamInvitation invitation = pendingInvitation();
        when(invitationRepository.findById(5L)).thenReturn(Optional.of(invitation));
        when(memberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(false);

        service.accept(5L, guest);

        ArgumentCaptor<TeamMember> member = ArgumentCaptor.forClass(TeamMember.class);
        verify(memberRepository).save(member.capture());
        assertEquals(TeamMemberRole.MEMBER, member.getValue().getRole());
        assertEquals(guest, member.getValue().getUser());
        assertEquals(TeamInvitationStatus.ACEPTADA, invitation.getStatus());
        assertNotNull(invitation.getRespondedAt());
        verify(notificationService).notify(eq(owner), eq(NotificationType.EQUIPO_INVITACION), any(), any());
    }

    @Test
    void decliningDoesNotAddMember() {
        TeamInvitation invitation = pendingInvitation();
        when(invitationRepository.findById(5L)).thenReturn(Optional.of(invitation));

        service.decline(5L, guest);

        assertEquals(TeamInvitationStatus.RECHAZADA, invitation.getStatus());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void onlyTheInvitedUserCanRespondAndOnlyWhilePending() {
        TeamInvitation invitation = pendingInvitation();
        when(invitationRepository.findById(5L)).thenReturn(Optional.of(invitation));

        assertThrows(AccessDeniedException.class, () -> service.accept(5L, user(3L, "Otro", "otro@test.com")));
        assertThrows(AccessDeniedException.class, () -> service.decline(5L, owner));

        invitation.setStatus(TeamInvitationStatus.RECHAZADA);
        assertThrows(BusinessException.class, () -> service.accept(5L, guest));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void ownerCancelsPendingInvitationOfHisTeamOnly() {
        TeamInvitation invitation = pendingInvitation();
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(invitationRepository.findById(5L)).thenReturn(Optional.of(invitation));

        assertThrows(AccessDeniedException.class, () -> service.cancel(10L, 5L, guest));
        service.cancel(10L, 5L, owner);
        assertEquals(TeamInvitationStatus.CANCELADA, invitation.getStatus());

        Team other = Team.builder().id(11L).name("Otro").owner(owner).build();
        when(teamRepository.findById(11L)).thenReturn(Optional.of(other));
        TeamInvitation fresh = pendingInvitation();
        when(invitationRepository.findById(6L)).thenReturn(Optional.of(fresh));
        assertThrows(ValidationException.class, () -> service.cancel(11L, 6L, owner));
    }

    private TeamInvitation pendingInvitation() {
        return TeamInvitation.builder().id(5L).team(team).invitedUser(guest).invitedBy(owner).build();
    }

    private User user(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setActive(true);
        return user;
    }
}
