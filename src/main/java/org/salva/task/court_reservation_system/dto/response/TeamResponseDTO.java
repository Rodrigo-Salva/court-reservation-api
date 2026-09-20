package org.salva.task.court_reservation_system.dto.response;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class TeamResponseDTO { private Long id; private String name; private String description; private Long ownerId; private String ownerName; private int memberCount; }
