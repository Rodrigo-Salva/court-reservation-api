package org.salva.task.court_reservation_system.dto.response;
import lombok.Builder;
import lombok.Data;
import org.salva.task.court_reservation_system.enums.TeamMemberRole;
@Data @Builder public class TeamMemberResponseDTO { private Long userId; private String name; private TeamMemberRole role; }
