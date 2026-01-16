package org.salva.task.court_reservation_system.service.impl;

import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.repository.BookingRepository;
import org.salva.task.court_reservation_system.service.BookingService;
import org.salva.task.court_reservation_system.service.ScheduledTaskService;
import org.salva.task.court_reservation_system.service.UserPackageService;
import org.salva.task.court_reservation_system.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Implementación de tareas programadas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    private final BookingService bookingService;
    private final UserPackageService userPackageService;
    private final WaitingListService waitingListService;
    private final BookingRepository bookingRepository;

    @Override
    @Scheduled(cron = "0 0 * * * *") // Cada hora en punto
    public void markPastBookingsAsCompleted() {
        log.info("Running scheduled task: markPastBookingsAsCompleted");

        try {
            bookingService.markPastBookingsAsCompleted();
            log.info("Completed scheduled task: markPastBookingsAsCompleted");
        } catch (Exception e) {
            log.error("Error in scheduled task markPastBookingsAsCompleted", e);
        }
    }

    @Override
    @Scheduled(cron = "0 0 2 * * *") // Todos los días a las 2 AM
    public void markExpiredPackagesAsInactive() {
        log.info("Running scheduled task: markExpiredPackagesAsInactive");

        try {
            userPackageService.markExpiredPackagesAsInactive();
            log.info("Completed scheduled task: markExpiredPackagesAsInactive");
        } catch (Exception e) {
            log.error("Error in scheduled task markExpiredPackagesAsInactive", e);
        }
    }

    @Override
    @Scheduled(fixedRate = 300000) // Cada 5 minutos (300,000 ms)
    public void processExpiredWaitingListNotifications() {
        log.debug("Running scheduled task: processExpiredWaitingListNotifications");

        try {
            waitingListService.processExpiredNotifications();
        } catch (Exception e) {
            log.error("Error in scheduled task processExpiredWaitingListNotifications", e);
        }
    }

    @Override
    @Scheduled(cron = "0 0 3 * * *") // Todos los días a las 3 AM
    public void cleanOldWaitingListRequests() {
        log.info("Running scheduled task: cleanOldWaitingListRequests");

        try {
            // Limpiar solicitudes mayores a 30 días
            waitingListService.cleanOldRequests(30);
            log.info("Completed scheduled task: cleanOldWaitingListRequests");
        } catch (Exception e) {
            log.error("Error in scheduled task cleanOldWaitingListRequests", e);
        }
    }

    @Override
    @Scheduled(cron = "0 0 * * * *") // Cada hora en punto
    public void sendUpcomingBookingReminders() {
        log.debug("Running scheduled task: sendUpcomingBookingReminders");

        try {
            // Buscar reservas que empiezan en 2 horas
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            LocalTime twoHoursLater = now.plusHours(2);

            List<Booking> upcomingBookings = bookingRepository
                    .findBookingsStartingSoon(today, now, twoHoursLater);

            for (Booking booking : upcomingBookings) {
                sendReminder(booking);
            }

            log.debug("Sent {} booking reminders", upcomingBookings.size());
        } catch (Exception e) {
            log.error("Error in scheduled task sendUpcomingBookingReminders", e);
        }
    }

    private void sendReminder(Booking booking) {
        // Aquí enviarías email/SMS/push notification
        log.info("REMINDER: User {} has booking at {} on court {}",
                booking.getUser().getEmail(),
                booking.getStartTime(),
                booking.getCourt().getName());

        // Ejemplo de implementación futura:
        // emailService.sendEmail(
        //     booking.getUser().getEmail(),
        //     "Recordatorio de reserva",
        //     "Tu reserva empieza en 2 horas..."
        // );
    }
}
