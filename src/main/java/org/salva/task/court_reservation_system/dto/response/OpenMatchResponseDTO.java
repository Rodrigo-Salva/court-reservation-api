package org.salva.task.court_reservation_system.dto.response;
import lombok.Builder;
import lombok.Data;
import org.salva.task.court_reservation_system.enums.OpenMatchStatus;
import java.time.*;
@Data @Builder public class OpenMatchResponseDTO { private Long id; private Long bookingId; private Long courtId; private String courtName; private LocalDate date; private LocalTime startTime; private LocalTime endTime; private String creatorName; private Integer maxPlayers; private Long confirmedPlayers; private String note; private OpenMatchStatus status; }
