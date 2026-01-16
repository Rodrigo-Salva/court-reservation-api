package org.salva.task.court_reservation_system.service;
/**
 * Interface para tareas programadas (scheduled jobs)
 */
public interface ScheduledTaskService {

    /**
     * Marca reservas pasadas como completadas
     * Ejecuta cada hora
     */
    void markPastBookingsAsCompleted();

    /**
     * Marca paquetes vencidos como inactivos
     * Ejecuta una vez al día
     */
    void markExpiredPackagesAsInactive();

    /**
     * Procesa notificaciones expiradas de lista de espera
     * Ejecuta cada 5 minutos
     */
    void processExpiredWaitingListNotifications();

    /**
     * Limpia solicitudes antiguas de lista de espera
     * Ejecuta una vez al día
     */
    void cleanOldWaitingListRequests();

    /**
     * Envía recordatorios de reservas próximas
     * Ejecuta cada hora
     */
    void sendUpcomingBookingReminders();
}
