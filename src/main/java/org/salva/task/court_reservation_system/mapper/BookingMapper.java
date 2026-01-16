package org.salva.task.court_reservation_system.mapper;

import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.dto.response.BookingDetailResponseDTO;
import org.salva.task.court_reservation_system.dto.response.BookingResponseDTO;
import org.salva.task.court_reservation_system.entity.Booking;
import org.mapstruct.*;

import java.time.Duration;
import java.util.List;

/**
 * Mapper para convertir entre Booking entity y DTOs
 */
@Mapper(componentModel = "spring")
public interface BookingMapper {

    /**
     * Convierte a DTO simple (para listas)
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "court.id", target = "courtId")
    @Mapping(source = "court.name", target = "courtName")
    BookingResponseDTO toResponseDTO(Booking booking);

    List<BookingResponseDTO> toResponseDTOList(List<Booking> bookings);

    /**
     * Convierte a DTO detallado (para vista individual)
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "court.id", target = "courtId")
    @Mapping(source = "court.name", target = "courtName")
    @Mapping(source = "court.sportType", target = "courtSportType")
    @Mapping(target = "durationHours", expression = "java(calculateDuration(booking))")
    BookingDetailResponseDTO toDetailResponseDTO(Booking booking);

    /**
     * Convierte DTO de request a entidad parcial
     * Las relaciones (user, court) se setean en el service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "court", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "basePrice", ignore = true)
    @Mapping(target = "dynamicSurcharges", ignore = true)
    @Mapping(target = "appliedDiscount", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "isRecurrent", constant = "false")
    @Mapping(target = "recurrenceFrequency", constant = "NONE")
    @Mapping(target = "parentBookingId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "penaltyPercentage", ignore = true)
    @Mapping(target = "penaltyAmount", ignore = true)
    Booking toEntity(BookingRequestDTO requestDTO);

    /**
     * Calcula la duración en horas de una reserva
     */
    default Integer calculateDuration(Booking booking) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return 0;
        }
        Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
        return (int) duration.toHours();
    }
}
