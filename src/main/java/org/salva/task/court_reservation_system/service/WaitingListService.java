package org.salva.task.court_reservation_system.service;

import org.salva.task.court_reservation_system.dto.request.WaitingListRequestDTO;
import org.salva.task.court_reservation_system.dto.response.WaitingListResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Interface para servicios de WaitingList (Lista de Espera)
 */
public interface WaitingListService {

    /**
     * Agrega un usuario a la lista de espera
     */
    WaitingListResponseDTO addToWaitingList(WaitingListRequestDTO requestDTO);

    /**
     * Obtiene una solicitud de lista de espera por ID
     */
    WaitingListResponseDTO getWaitingListById(Long id);

    /**
     * Obtiene todas las solicitudes de un usuario
     */
    List<WaitingListResponseDTO> getWaitingListByUser(Long userId);

    /**
     * Obtiene usuarios esperando por una cancha y horario específico
     */
    List<WaitingListResponseDTO> getPendingByCourtAndTimeSlot(
            Long courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    );

    /**
     * Procesa cancelación de reserva y notifica a lista de espera
     */
    void processCancellationNotification(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime);

    /**
     * Verifica y procesa notificaciones expiradas (job automático)
     */
    void processExpiredNotifications();

    /**
     * Elimina una solicitud de lista de espera
     */
    void removeFromWaitingList(Long id);

    /**
     * Limpia solicitudes antiguas (job automático)
     */
    void cleanOldRequests(int daysOld);
}
