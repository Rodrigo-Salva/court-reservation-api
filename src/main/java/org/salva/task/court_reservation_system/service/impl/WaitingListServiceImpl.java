package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.dto.request.WaitingListRequestDTO;
import org.salva.task.court_reservation_system.dto.response.WaitingListResponseDTO;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.WaitingList;
import org.salva.task.court_reservation_system.exception.BusinessException;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.mapper.WaitingListMapper;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.repository.WaitingListRepository;
import org.salva.task.court_reservation_system.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación del servicio de WaitingList
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WaitingListServiceImpl implements WaitingListService {

    private final WaitingListRepository waitingListRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final WaitingListMapper waitingListMapper;

    // Tiempo en minutos que tiene el usuario para responder
    private static final int NOTIFICATION_TIMEOUT_MINUTES = 30;

    @Override
    public WaitingListResponseDTO addToWaitingList(WaitingListRequestDTO requestDTO) {
        log.info("Adding user {} to waiting list for court {} on date {}",
                requestDTO.getUserId(), requestDTO.getCourtId(), requestDTO.getDesiredDate());

        // Validar que el horario deseado sea futuro
        LocalDateTime desiredDateTime = LocalDateTime.of(
                requestDTO.getDesiredDate(),
                requestDTO.getDesiredStartTime()
        );

        if (desiredDateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("No se puede agregar a lista de espera para horarios pasados");
        }

        // Obtener entidades
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Court court = courtRepository.findById(requestDTO.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));

        // Verificar si ya está en la lista para ese horario
        long count = waitingListRepository.countByCourtIdAndDesiredDateAndDesiredStartTimeAndDesiredEndTime(
                requestDTO.getCourtId(),
                requestDTO.getDesiredDate(),
                requestDTO.getDesiredStartTime(),
                requestDTO.getDesiredEndTime()
        );

        // Crear solicitud
        WaitingList waitingList = waitingListMapper.toEntity(requestDTO);
        waitingList.setUser(user);
        waitingList.setCourt(court);

        waitingList = waitingListRepository.save(waitingList);

        log.info("User added to waiting list. Position in queue: {}", count + 1);

        WaitingListResponseDTO response = waitingListMapper.toResponseDTO(waitingList);
        response.setPositionInQueue((int) count + 1);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public WaitingListResponseDTO getWaitingListById(Long id) {
        WaitingList waitingList = waitingListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de lista de espera no encontrada"));

        WaitingListResponseDTO response = waitingListMapper.toResponseDTO(waitingList);
        response.setPositionInQueue(calculatePosition(waitingList));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitingListResponseDTO> getWaitingListByUser(Long userId) {
        log.debug("Getting waiting list for user: {}", userId);

        List<WaitingList> waitingLists = waitingListRepository.findByUserIdOrderByRequestDateDesc(userId);
        List<WaitingListResponseDTO> responses = waitingListMapper.toResponseDTOList(waitingLists);

        // Calcular posición en la cola para cada uno
        for (int i = 0; i < waitingLists.size(); i++) {
            responses.get(i).setPositionInQueue(calculatePosition(waitingLists.get(i)));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitingListResponseDTO> getPendingByCourtAndTimeSlot(
            Long courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        log.debug("Getting pending waiting list for court: {} on date: {} time: {}-{}",
                courtId, date, startTime, endTime);

        List<WaitingList> waitingLists = waitingListRepository.findPendingByCourtAndTimeSlot(
                courtId, date, startTime, endTime
        );

        return waitingListMapper.toResponseDTOList(waitingLists);
    }

    @Override
    public void processCancellationNotification(
            Long courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        log.info("Processing cancellation notification for court: {} on date: {} time: {}-{}",
                courtId, date, startTime, endTime);

        // RN-031: Obtener usuarios en lista de espera (ordenados por FIFO)
        List<WaitingList> waitingList = waitingListRepository.findPendingByCourtAndTimeSlot(
                courtId, date, startTime, endTime
        );

        if (waitingList.isEmpty()) {
            log.info("No users in waiting list for this time slot");
            return;
        }

        // RN-032: Notificar al primero en la lista
        WaitingList first = waitingList.get(0);
        first.notifyUser(NOTIFICATION_TIMEOUT_MINUTES);
        waitingListRepository.save(first);

        // Aquí normalmente enviarías un email/SMS/push notification
        sendNotification(first);

        log.info("Notification sent to user: {}. They have {} minutes to respond",
                first.getUser().getId(), NOTIFICATION_TIMEOUT_MINUTES);
    }

    @Override
    public void processExpiredNotifications() {
        log.info("Processing expired notifications");

        List<WaitingList> expiredNotifications = waitingListRepository
                .findExpiredNotifications(LocalDateTime.now());

        int processedCount = 0;

        for (WaitingList waiting : expiredNotifications) {
            if (waiting.isNotificationExpired()) {
                log.info("User {} did not respond in time. Moving to next in queue",
                        waiting.getUser().getId());

                // Marcar como no notificado para intentar con el siguiente
                waiting.setNotified(false);
                waiting.setNotificationDate(null);
                waiting.setNotificationExpirationDate(null);
                waitingListRepository.save(waiting);

                // Notificar al siguiente en la lista (RN-034)
                processCancellationNotification(
                        waiting.getCourt().getId(),
                        waiting.getDesiredDate(),
                        waiting.getDesiredStartTime(),
                        waiting.getDesiredEndTime()
                );

                processedCount++;
            }
        }

        log.info("Processed {} expired notifications", processedCount);
    }

    @Override
    public void removeFromWaitingList(Long id) {
        log.info("Removing from waiting list: {}", id);

        WaitingList waitingList = waitingListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de lista de espera no encontrada"));

        waitingListRepository.delete(waitingList);

        log.info("Removed from waiting list successfully");
    }

    @Override
    public void cleanOldRequests(int daysOld) {
        log.info("Cleaning waiting list requests older than {} days", daysOld);

        LocalDate cutoffDate = LocalDate.now().minusDays(daysOld);
        waitingListRepository.deleteOldRequests(cutoffDate);

        log.info("Old waiting list requests cleaned");
    }

    // ========== MÉTODOS PRIVADOS ==========

    private int calculatePosition(WaitingList waitingList) {
        if (waitingList.getNotified()) {
            return 1; // Si ya fue notificado, está primero
        }

        List<WaitingList> allWaiting = waitingListRepository.findPendingByCourtAndTimeSlot(
                waitingList.getCourt().getId(),
                waitingList.getDesiredDate(),
                waitingList.getDesiredStartTime(),
                waitingList.getDesiredEndTime()
        );

        AtomicInteger position = new AtomicInteger(1);
        for (WaitingList w : allWaiting) {
            if (w.getId().equals(waitingList.getId())) {
                return position.get();
            }
            position.incrementAndGet();
        }

        return position.get();
    }

    private void sendNotification(WaitingList waitingList) {
        // Aquí integrarías con un servicio de notificaciones (Email, SMS, Push)
        log.info("NOTIFICATION: User {} has availability for court {} on {} from {}-{}",
                waitingList.getUser().getEmail(),
                waitingList.getCourt().getName(),
                waitingList.getDesiredDate(),
                waitingList.getDesiredStartTime(),
                waitingList.getDesiredEndTime());

        // Ejemplo de implementación futura:
        // emailService.sendEmail(
        //     waitingList.getUser().getEmail(),
        //     "¡Cancha disponible!",
        //     "Tienes 30 minutos para confirmar tu reserva..."
        // );
    }
}
