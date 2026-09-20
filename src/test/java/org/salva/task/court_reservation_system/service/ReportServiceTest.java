package org.salva.task.court_reservation_system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salva.task.court_reservation_system.dto.request.ReportFilterDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO;
import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.enums.PaymentStatus;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.enums.SportType;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.repository.BookingRepository;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.repository.PaymentRepository;
import org.salva.task.court_reservation_system.repository.VenueRepository;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private VenueRepository venueRepository;

    private ReportService service;
    private final LocalDate start = LocalDate.of(2026, 3, 1);
    private final LocalDate end = LocalDate.of(2026, 3, 3);
    private final Venue venueA = Venue.builder().id(1L).name("Central").build();
    private final Venue venueB = Venue.builder().id(2L).name("Norte").build();
    private final Court padel = court(1, "Pádel 1", SportType.PADEL, venueA);
    private final Court tenis = court(2, "Tenis 1", SportType.TENIS, venueA);

    @BeforeEach
    void setUp() {
        service = new ReportService(bookingRepository, paymentRepository, courtRepository, venueRepository, new AccessControlService());
    }

    @Test
    void aggregatesTotalsRevenueAndBreakdowns() {
        List<Booking> bookings = List.of(
                booking(10L, padel, start, 18, BookingStatus.COMPLETADA, "100"),
                booking(11L, padel, start, 18, BookingStatus.CANCELADA, "100"),
                booking(12L, tenis, end, 20, BookingStatus.NO_SHOW, "50"));
        when(bookingRepository.findByBookingDateBetween(start, end)).thenReturn(bookings);
        when(paymentRepository.findBookingIdsByStatus(any(), org.mockito.ArgumentMatchers.eq(PaymentStatus.APROBADO))).thenReturn(List.of(10L, 12L));

        OperationalReportResponseDTO report = service.build(new ReportFilterDTO(start, end, null, null, null), admin());

        assertEquals(3, report.getTotalBookings());
        assertEquals(1, report.getCompletedBookings());
        assertEquals(1, report.getCancelledBookings());
        assertEquals(1, report.getNoShows());
        assertEquals(0, new BigDecimal("150").compareTo(report.getRevenue()));
        assertEquals(0, new BigDecimal("33.33").compareTo(report.getCancellationRate()));
        assertEquals(3, report.getDaily().size());
        assertEquals(2, report.getDaily().get(0).getBookings());
        assertEquals("Pádel 1", report.getByCourt().get(0).getCourtName());
        assertEquals(2, report.getBySport().size());
        assertEquals("18:00", report.getPeakHours().get(0).getHour());
        assertTrue(report.getScope().contains("Sede: todas"));
    }

    @Test
    void filtersByCourtAndSport() {
        List<Booking> bookings = List.of(
                booking(10L, padel, start, 18, BookingStatus.COMPLETADA, "100"),
                booking(12L, tenis, end, 20, BookingStatus.COMPLETADA, "50"));
        when(bookingRepository.findByBookingDateBetween(start, end)).thenReturn(bookings);
        when(paymentRepository.findBookingIdsByStatus(any(), any())).thenReturn(List.of());

        assertEquals(1, service.build(new ReportFilterDTO(start, end, null, null, SportType.TENIS), admin()).getTotalBookings());

        when(courtRepository.findById(1L)).thenReturn(Optional.of(padel));
        OperationalReportResponseDTO byCourt = service.build(new ReportFilterDTO(start, end, null, 1L, null), admin());
        assertEquals(1, byCourt.getTotalBookings());
        assertTrue(byCourt.getScope().contains("Cancha: Pádel 1"));
    }

    @Test
    void venueStaffIsForcedToTheirVenue() {
        when(bookingRepository.findByCourtVenueIdAndBookingDateBetween(1L, start, end)).thenReturn(List.of());
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venueA));

        OperationalReportResponseDTO report = service.build(new ReportFilterDTO(start, end, null, null, null), staff(venueA));

        assertEquals(0, report.getTotalBookings());
        assertTrue(report.getScope().contains("Sede: Central"));
        verify(bookingRepository, never()).findByBookingDateBetween(any(), any());
    }

    @Test
    void venueStaffCannotRequestAnotherVenueOrItsCourts() {
        CustomUserDetails staff = staff(venueA);
        assertThrows(AccessDeniedException.class, () -> service.build(new ReportFilterDTO(start, end, 2L, null, null), staff));

        Court northCourt = court(9, "Norte 1", SportType.PADEL, venueB);
        when(courtRepository.findById(9L)).thenReturn(Optional.of(northCourt));
        assertThrows(AccessDeniedException.class, () -> service.build(new ReportFilterDTO(start, end, null, 9L, null), staff));
    }

    @Test
    void rejectsInvalidRanges() {
        assertThrows(ValidationException.class, () -> service.build(new ReportFilterDTO(end, start, null, null, null), admin()));
        assertThrows(ValidationException.class, () -> service.build(new ReportFilterDTO(start, start.plusDays(400), null, null, null), admin()));
        assertThrows(ValidationException.class, () -> service.build(new ReportFilterDTO(null, end, null, null, null), admin()));
    }

    private Court court(int id, String name, SportType sport, Venue venue) {
        Court court = new Court();
        court.setId(id);
        court.setName(name);
        court.setSportType(sport);
        court.setVenue(venue);
        return court;
    }

    private Booking booking(Long id, Court court, LocalDate date, int hour, BookingStatus status, String price) {
        return Booking.builder().id(id).court(court).bookingDate(date).startTime(LocalTime.of(hour, 0)).endTime(LocalTime.of(hour + 1, 0))
                .status(status).totalPrice(new BigDecimal(price)).build();
    }

    private CustomUserDetails admin() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.ADMIN);
        return new CustomUserDetails(user);
    }

    private CustomUserDetails staff(Venue venue) {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.VENUE_ADMIN);
        user.setVenue(venue);
        return new CustomUserDetails(user);
    }
}
