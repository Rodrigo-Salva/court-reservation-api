package org.salva.task.court_reservation_system.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.dto.request.CancellationRequestDTO;
import org.salva.task.court_reservation_system.dto.response.CancellationResponseDTO;
import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.salva.task.court_reservation_system.enums.NotificationType;
import org.salva.task.court_reservation_system.mapper.BookingMapper;
import org.salva.task.court_reservation_system.repository.BookingRepository;
import org.salva.task.court_reservation_system.repository.CourtBlockRepository;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.repository.UserPackageRepository;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.service.AuditService;
import org.salva.task.court_reservation_system.service.NotificationService;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPaymentFlowTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private UserPackageRepository userPackageRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private CourtBlockRepository courtBlockRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @InjectMocks private BookingServiceImpl bookingService;

    private User user;
    private Court court;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Juan");
        user.setMembershipType(MembershipType.VIP);
        user.setActive(true);

        court = new Court();
        court.setId(1);
        court.setName("Cancha 1");
        court.setPriceBaseHour(BigDecimal.valueOf(50));
        court.setActive(true);
    }

    private Booking createBookingWithFlag(boolean requirePayment) {
        ReflectionTestUtils.setField(bookingService, "requirePayment", requirePayment);
        ReflectionTestUtils.setField(bookingService, "paymentWindowMinutes", 15);
        BookingRequestDTO request = new BookingRequestDTO();
        request.setUserId(1L);
        request.setCourtId(1L);
        request.setBookingDate(LocalDate.now().plusDays(5));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(11, 0));
        request.setUsesPackage(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courtRepository.findByIdForBooking(1L)).thenReturn(Optional.of(court));
        Booking mapped = new Booking();
        mapped.setBookingDate(request.getBookingDate());
        mapped.setStartTime(request.getStartTime());
        mapped.setEndTime(request.getEndTime());
        when(bookingMapper.toEntity(any(BookingRequestDTO.class))).thenReturn(mapped);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> call.getArgument(0));

        bookingService.createBooking(request);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void newBookingIsConfirmedImmediatelyWhenPaymentIsNotRequired() {
        Booking booking = createBookingWithFlag(false);

        assertEquals(BookingStatus.CONFIRMADA, booking.getStatus());
        assertNull(booking.getPaymentDeadline());
    }

    @Test
    void newBookingWaitsForPaymentWithADeadlineWhenPaymentIsRequired() {
        LocalDateTime before = LocalDateTime.now();

        Booking booking = createBookingWithFlag(true);

        assertEquals(BookingStatus.PENDIENTE, booking.getStatus());
        assertNotNull(booking.getPaymentDeadline());
        assertTrue(booking.getPaymentDeadline().isAfter(before.plusMinutes(14)));
        assertTrue(booking.getPaymentDeadline().isBefore(before.plusMinutes(16)));
        verify(notificationService).notify(eq(user), eq(NotificationType.RESERVA_CREADA), eq("Reserva pendiente de pago"), anyString());
    }

    @Test
    void expiredUnpaidBookingsAreCancelledAndTheirOwnersNotified() {
        Booking expired = pendingBooking(10L);
        when(bookingRepository.findByStatusAndPaymentDeadlineBefore(eq(BookingStatus.PENDIENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        int cancelled = bookingService.cancelExpiredUnpaidBookings();

        assertEquals(1, cancelled);
        assertEquals(BookingStatus.CANCELADA, expired.getStatus());
        assertNotNull(expired.getCancelledAt());
        assertEquals("Pago no realizado dentro del plazo", expired.getCancellationReason());
        verify(bookingRepository).save(expired);
        verify(auditService).record(eq("EXPIRAR"), eq("RESERVA"), eq(10L), anyString(), any());
        verify(notificationService).notify(eq(user), eq(NotificationType.CANCELACION), anyString(), anyString());
    }

    @Test
    void noBookingsAreTouchedWhenNothingExpired() {
        when(bookingRepository.findByStatusAndPaymentDeadlineBefore(eq(BookingStatus.PENDIENTE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertEquals(0, bookingService.cancelExpiredUnpaidBookings());
    }

    @Test
    void cancellingAnUnpaidBookingHasNoPenalty() {
        Booking pending = pendingBooking(11L);
        pending.setBookingDate(LocalDate.now().plusDays(3));
        pending.setStartTime(LocalTime.of(10, 0));
        pending.setEndTime(LocalTime.of(11, 0));
        pending.setTotalPrice(BigDecimal.valueOf(50));
        pending.setUsesPackage(false);
        when(bookingRepository.findById(11L)).thenReturn(Optional.of(pending));
        CancellationRequestDTO request = new CancellationRequestDTO();
        request.setBookingId(11L);
        request.setReason("Ya no puedo");

        CancellationResponseDTO response = bookingService.cancelBooking(request);

        assertEquals(BookingStatus.CANCELADA, pending.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(pending.getPenaltyAmount()));
        assertNotNull(response);
    }

    private Booking pendingBooking(Long id) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setCourt(court);
        booking.setStatus(BookingStatus.PENDIENTE);
        booking.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));
        return booking;
    }
}
