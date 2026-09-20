package org.salva.task.court_reservation_system.service;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.ReportFilterDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO.CourtStatDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO.DayStatDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO.PeakHourDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO.SportStatDTO;
import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.enums.PaymentStatus;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.repository.BookingRepository;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.repository.PaymentRepository;
import org.salva.task.court_reservation_system.repository.VenueRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Reporte operativo con filtros por sede, cancha y deporte. El personal de sede queda limitado a su sede. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    static final long MAX_RANGE_DAYS = 366;

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final CourtRepository courtRepository;
    private final VenueRepository venueRepository;
    private final AccessControlService accessControl;

    public OperationalReportResponseDTO build(ReportFilterDTO filter, CustomUserDetails currentUser) {
        LocalDate start = filter.startDate();
        LocalDate end = filter.endDate();
        if (start == null || end == null) throw new ValidationException("Indica el rango de fechas");
        if (end.isBefore(start)) throw new ValidationException("La fecha final no puede ser anterior a la inicial");
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) throw new ValidationException("El rango máximo es de un año");

        Long venueId = effectiveVenueId(filter.venueId(), currentUser);
        Court court = null;
        if (filter.courtId() != null) {
            court = courtRepository.findById(filter.courtId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
            Long courtVenueId = court.getVenue() != null ? court.getVenue().getId() : null;
            accessControl.requireSameVenueOrAdmin(courtVenueId, currentUser);
            if (venueId != null && !venueId.equals(courtVenueId)) throw new ValidationException("La cancha no pertenece a la sede seleccionada");
        }

        List<Booking> bookings = (venueId != null
                ? bookingRepository.findByCourtVenueIdAndBookingDateBetween(venueId, start, end)
                : bookingRepository.findByBookingDateBetween(start, end)).stream()
                .filter(b -> filter.courtId() == null || filter.courtId().equals((long) b.getCourt().getId()))
                .filter(b -> filter.sportType() == null || b.getCourt().getSportType() == filter.sportType())
                .toList();

        Set<Long> paidBookingIds = bookings.isEmpty() ? Set.of()
                : new HashSet<>(paymentRepository.findBookingIdsByStatus(bookings.stream().map(Booking::getId).toList(), PaymentStatus.APROBADO));

        long cancelled = count(bookings, BookingStatus.CANCELADA);
        BigDecimal revenue = revenueOf(bookings, paidBookingIds);
        BigDecimal cancellationRate = bookings.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(cancelled).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(bookings.size()), 2, RoundingMode.HALF_UP);

        return OperationalReportResponseDTO.builder()
                .startDate(start.toString()).endDate(end.toString()).scope(describeScope(venueId, court, filter))
                .totalBookings(bookings.size()).completedBookings(count(bookings, BookingStatus.COMPLETADA))
                .cancelledBookings(cancelled).noShows(count(bookings, BookingStatus.NO_SHOW))
                .revenue(revenue).cancellationRate(cancellationRate)
                .peakHours(peakHours(bookings)).daily(daily(bookings, paidBookingIds, start, end))
                .byCourt(byCourt(bookings, paidBookingIds)).bySport(bySport(bookings, paidBookingIds))
                .build();
    }

    private Long effectiveVenueId(Long requestedVenueId, CustomUserDetails currentUser) {
        Long scope = accessControl.resolveVenueFilter(currentUser);
        if (scope == null) return requestedVenueId;
        if (requestedVenueId != null && !requestedVenueId.equals(scope)) {
            throw new AccessDeniedException("No tiene permiso para acceder a recursos de otra sede");
        }
        return scope;
    }

    private String describeScope(Long venueId, Court court, ReportFilterDTO filter) {
        List<String> parts = new ArrayList<>();
        parts.add("Sede: " + (venueId == null ? "todas" : venueRepository.findById(venueId).map(Venue::getName).orElse("#" + venueId)));
        parts.add("Cancha: " + (court == null ? "todas" : court.getName()));
        parts.add("Deporte: " + (filter.sportType() == null ? "todos" : filter.sportType().name()));
        return String.join(" · ", parts);
    }

    private long count(List<Booking> bookings, BookingStatus status) {
        return bookings.stream().filter(b -> b.getStatus() == status).count();
    }

    private BigDecimal revenueOf(Collection<Booking> bookings, Set<Long> paidBookingIds) {
        return bookings.stream().filter(b -> paidBookingIds.contains(b.getId()))
                .map(Booking::getTotalPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PeakHourDTO> peakHours(List<Booking> bookings) {
        return bookings.stream().filter(b -> b.getStatus() != BookingStatus.CANCELADA)
                .collect(Collectors.groupingBy(b -> String.format("%02d:00", b.getStartTime().getHour()), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(5).map(e -> PeakHourDTO.builder().hour(e.getKey()).bookings(e.getValue()).build()).toList();
    }

    private List<DayStatDTO> daily(List<Booking> bookings, Set<Long> paid, LocalDate start, LocalDate end) {
        Map<LocalDate, List<Booking>> byDate = bookings.stream().collect(Collectors.groupingBy(Booking::getBookingDate));
        List<DayStatDTO> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            List<Booking> day = byDate.getOrDefault(date, List.of());
            days.add(DayStatDTO.builder().date(date.toString()).bookings(day.size()).revenue(revenueOf(day, paid)).build());
        }
        return days;
    }

    private List<CourtStatDTO> byCourt(List<Booking> bookings, Set<Long> paid) {
        return bookings.stream().collect(Collectors.groupingBy(b -> b.getCourt().getId())).values().stream()
                .map(group -> {
                    Court court = group.get(0).getCourt();
                    return CourtStatDTO.builder().courtId((long) court.getId()).courtName(court.getName())
                            .sportType(court.getSportType().name()).venueName(court.getVenue() != null ? court.getVenue().getName() : null)
                            .bookings(group.size()).revenue(revenueOf(group, paid)).build();
                })
                .sorted(Comparator.comparing(CourtStatDTO::getRevenue).reversed().thenComparing(Comparator.comparingLong(CourtStatDTO::getBookings).reversed()))
                .toList();
    }

    private List<SportStatDTO> bySport(List<Booking> bookings, Set<Long> paid) {
        return bookings.stream().collect(Collectors.groupingBy(b -> b.getCourt().getSportType().name())).entrySet().stream()
                .map(e -> SportStatDTO.builder().sportType(e.getKey()).bookings(e.getValue().size()).revenue(revenueOf(e.getValue(), paid)).build())
                .sorted(Comparator.comparingLong(SportStatDTO::getBookings).reversed().thenComparing(SportStatDTO::getSportType))
                .toList();
    }
}
