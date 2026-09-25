package org.salva.task.court_reservation_system.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salva.task.court_reservation_system.dto.response.PageResponseDTO;
import org.salva.task.court_reservation_system.entity.*;
import org.salva.task.court_reservation_system.enums.*;
import org.salva.task.court_reservation_system.repository.spec.BookingSpecifications;
import org.salva.task.court_reservation_system.repository.spec.CourtReviewSpecifications;
import org.salva.task.court_reservation_system.repository.spec.PaymentSpecifications;
import org.salva.task.court_reservation_system.repository.spec.UserSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class PaginationQueriesTest {

    @Autowired private TestEntityManager em;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CourtReviewRepository reviewRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;

    private Venue venueA;
    private Venue venueB;

    @BeforeEach
    void setUp() {
        venueA = em.persist(Venue.builder().name("Central").address("Av 1").build());
        venueB = em.persist(Venue.builder().name("Norte").address("Av 2").build());
        Court padel = em.persist(court("Pádel Uno", SportType.PADEL, venueA));
        Court tenis = em.persist(court("Tenis Dos", SportType.TENIS, venueB));
        User ana = em.persist(user("Ana Pérez", "ana@test.com", "900000001"));
        User beto = em.persist(user("Beto Ríos", "beto@test.com", "900000002"));

        Booking b1 = em.persist(booking(ana, padel, BookingStatus.CONFIRMADA));
        Booking b2 = em.persist(booking(beto, tenis, BookingStatus.CANCELADA));
        Booking b3 = em.persist(booking(beto, padel, BookingStatus.COMPLETADA));
        em.persist(payment(b1, ana, PaymentStatus.APROBADO, "SIM-A1", "100"));
        em.persist(payment(b2, beto, PaymentStatus.RECHAZADO, "SIM-B2", "60"));
        em.persist(payment(b3, beto, PaymentStatus.REEMBOLSADO, "SIM-C3", "80"));

        em.persist(review(padel, ana, "Excelente cancha", false));
        em.persist(review(tenis, beto, "Mala experiencia", true));
        em.persist(review(padel, beto, "Regular", false));
        for (int i = 0; i < 3; i++) {
            em.persist(AuditLog.builder().action("CREAR").resourceType("RESERVA").resourceId((long) i).actorEmail("a@test.com").venueId(i == 2 ? 2L : 1L).build());
        }
        em.flush();
    }

    @Test
    void paymentsAreFilteredByVenueStatusAndText() {
        assertEquals(3, count(Specification.where(PaymentSpecifications.inVenue(null))));
        assertEquals(2, count(Specification.where(PaymentSpecifications.inVenue(venueA.getId()))));
        assertEquals(1, count(Specification.where(PaymentSpecifications.inVenue(venueA.getId())).and(PaymentSpecifications.hasStatus(PaymentStatus.APROBADO))));
        assertEquals(2, count(Specification.where(PaymentSpecifications.matches("beto"))));
        assertEquals(1, count(Specification.where(PaymentSpecifications.matches("sim-a1"))));
        assertEquals(1, count(Specification.where(PaymentSpecifications.matches("tenis"))));
        assertEquals(3, count(Specification.where(PaymentSpecifications.matches("   "))));
    }

    @Test
    void searchWildcardsAreTreatedAsLiteralText() {
        assertEquals(0, count(Specification.where(PaymentSpecifications.matches("%"))));
        assertEquals(0, count(Specification.where(PaymentSpecifications.matches("_"))));
    }

    @Test
    void paymentsArePagedWithTotals() {
        Page<Payment> first = paymentRepository.findAll(Specification.where(PaymentSpecifications.inVenue(null)),
                PageResponseDTO.pageable(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(2, first.getContent().size());
        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(1, paymentRepository.findAll(Specification.where(PaymentSpecifications.inVenue(null)),
                PageResponseDTO.pageable(1, 2, Sort.by("createdAt"))).getContent().size());
    }

    @Test
    void paymentTotalsPerStatusRespectTheVenue() {
        assertEquals(0, new BigDecimal("100").compareTo(paymentRepository.sumAmountByStatus(PaymentStatus.APROBADO)));
        assertEquals(0, new BigDecimal("80").compareTo(paymentRepository.sumAmountByStatusAndVenue(PaymentStatus.REEMBOLSADO, venueA.getId())));
        assertEquals(0, BigDecimal.ZERO.compareTo(paymentRepository.sumAmountByStatusAndVenue(PaymentStatus.APROBADO, venueB.getId())));
    }

    @Test
    void reviewsAreFilteredByVenueVisibilityAndText() {
        assertEquals(3, reviewRepository.count(Specification.where(CourtReviewSpecifications.inVenue(null))));
        assertEquals(2, reviewRepository.count(Specification.where(CourtReviewSpecifications.inVenue(venueA.getId()))));
        assertEquals(1, reviewRepository.count(Specification.where(CourtReviewSpecifications.byVisibility("hidden"))));
        assertEquals(2, reviewRepository.count(Specification.where(CourtReviewSpecifications.byVisibility("visible"))));
        assertEquals(1, reviewRepository.count(Specification.where(CourtReviewSpecifications.matches("excelente"))));
        assertEquals(2, reviewRepository.count(Specification.where(CourtReviewSpecifications.matches("beto"))));
    }

    @Test
    void bookingsAreFilteredByVenueStatusAndText() {
        assertEquals(2, bookingRepository.count(Specification.where(BookingSpecifications.inVenue(venueA.getId()))));
        assertEquals(1, bookingRepository.count(Specification.where(BookingSpecifications.hasStatus(BookingStatus.CANCELADA))));
        assertEquals(2, bookingRepository.count(Specification.where(BookingSpecifications.matches("beto"))));
        assertEquals(1, bookingRepository.count(Specification.where(BookingSpecifications.inVenue(venueA.getId())).and(BookingSpecifications.matches("ana"))));
    }

    @Test
    void auditLogsArePagedNewestFirstAndScopedByVenue() {
        Page<AuditLog> all = auditLogRepository.findAllByOrderByCreatedAtDesc(PageResponseDTO.pageable(0, 2, Sort.unsorted()));
        assertEquals(3, all.getTotalElements());
        assertEquals(2, all.getContent().size());

        Page<AuditLog> venueOne = auditLogRepository.findByVenueIdOrderByCreatedAtDesc(1L, PageResponseDTO.pageable(0, 10, Sort.unsorted()));
        assertEquals(2, venueOne.getTotalElements());
    }

    @Test
    void usersAreSearchedByNameEmailPhoneAndActiveFlag() {
        User inactive = user("Carla Gómez", "carla@test.com", "900000003");
        inactive.setActive(false);
        em.persist(inactive);
        em.flush();

        assertEquals(3, userRepository.count(Specification.where(UserSpecifications.matches(null))));
        assertEquals(1, userRepository.count(Specification.where(UserSpecifications.matches("pérez"))));
        assertEquals(1, userRepository.count(Specification.where(UserSpecifications.matches("BETO@test"))));
        assertEquals(1, userRepository.count(Specification.where(UserSpecifications.matches("900000003"))));
        assertEquals(2, userRepository.count(Specification.where(UserSpecifications.hasActive(true))));
        assertEquals(1, userRepository.count(Specification.where(UserSpecifications.hasActive(false)).and(UserSpecifications.matches("carla"))));
        assertEquals(0, userRepository.count(Specification.where(UserSpecifications.matches("%"))));
    }

    @Test
    void checkInCodeLookupFindsTheBooking() {
        Booking booking = bookingRepository.findAll().get(0);
        assertEquals(booking.getId(), bookingRepository.findByCheckInCode(booking.getCheckInCode()).orElseThrow().getId());
        assertEquals(true, bookingRepository.findByCheckInCode("no-existe").isEmpty());
    }

    @Test
    void pageSizeIsClampedToSafeBounds() {
        assertEquals(PageResponseDTO.DEFAULT_SIZE, PageResponseDTO.pageable(0, 0, Sort.unsorted()).getPageSize());
        assertEquals(PageResponseDTO.MAX_SIZE, PageResponseDTO.pageable(0, 100000, Sort.unsorted()).getPageSize());
        assertEquals(0, PageResponseDTO.pageable(-5, 10, Sort.unsorted()).getPageNumber());
    }

    private long count(Specification<Payment> spec) {
        return paymentRepository.count(spec);
    }

    private Court court(String name, SportType sport, Venue venue) {
        return Court.builder().name(name).sportType(sport).capacity(4).priceBaseHour(new BigDecimal("50")).description("cancha").venue(venue).build();
    }

    private User user(String name, String email, String phone) {
        return User.builder().name(name).email(email).phone(phone).membershipType(MembershipType.NINGUNA).password("x").role(Role.USER).active(true).build();
    }

    private Booking booking(User user, Court court, BookingStatus status) {
        return Booking.builder().user(user).court(court).bookingDate(LocalDate.now().plusDays(1)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .status(status).basePrice(BigDecimal.TEN).dynamicSurcharges(BigDecimal.ZERO).appliedDiscount(BigDecimal.ZERO).totalPrice(BigDecimal.TEN).build();
    }

    private Payment payment(Booking booking, User user, PaymentStatus status, String code, String amount) {
        return Payment.builder().booking(booking).user(user).method(PaymentMethod.EFECTIVO).status(status).amount(new BigDecimal(amount)).operationCode(code).build();
    }

    private CourtReview review(Court court, User user, String comment, boolean hidden) {
        CourtReview review = CourtReview.builder().court(court).user(user).rating(4).comment(comment).build();
        review.setHidden(hidden);
        return review;
    }
}
