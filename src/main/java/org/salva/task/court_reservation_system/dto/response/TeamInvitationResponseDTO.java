package org.salva.task.court_reservation_system.dto.response;

import lombok.Builder;
import lombok.Data;
import org.salva.task.court_reservation_system.enums.TeamInvitationStatus;
import java.time.LocalDateTime;

@Data @Builder
public class TeamInvitationResponseDTO {
    private Long id; private Long teamId; private String teamName; private String invitedName; private String invitedEmail;
    private String invitedByName; private TeamInvitationStatus status; private LocalDateTime createdAt;
}
