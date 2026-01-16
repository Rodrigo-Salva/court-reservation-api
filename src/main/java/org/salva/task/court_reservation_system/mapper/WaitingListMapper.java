package org.salva.task.court_reservation_system.mapper;

import org.salva.task.court_reservation_system.dto.request.WaitingListRequestDTO;
import org.salva.task.court_reservation_system.dto.response.WaitingListResponseDTO;
import org.salva.task.court_reservation_system.entity.WaitingList;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para convertir entre WaitingList entity y DTOs
 */
@Mapper(componentModel = "spring")
public interface WaitingListMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "court.id", target = "courtId")
    @Mapping(source = "court.name", target = "courtName")
    @Mapping(target = "positionInQueue", ignore = true) // Se calcula en el service
    WaitingListResponseDTO toResponseDTO(WaitingList waitingList);

    List<WaitingListResponseDTO> toResponseDTOList(List<WaitingList> waitingLists);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "court", ignore = true)
    @Mapping(target = "requestDate", ignore = true)
    @Mapping(target = "notified", constant = "false")
    @Mapping(target = "notificationDate", ignore = true)
    @Mapping(target = "notificationExpirationDate", ignore = true)
    WaitingList toEntity(WaitingListRequestDTO requestDTO);
}
