package org.salva.task.court_reservation_system.config;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.entity.*;
import org.salva.task.court_reservation_system.entity.Package;
import org.salva.task.court_reservation_system.enums.*;
import org.salva.task.court_reservation_system.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Datos de demostración para desarrollo: deja al menos 10 registros en cada tabla.
 * Se ejecuta después de {@link DataLoader} y solo una vez (se detecta por el usuario demo01).
 * Todos los usuarios demo usan la contraseña "demo123".
 */
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "demo123";

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final PackageRepository packageRepository;
    private final UserPackageRepository userPackageRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final CourtBlockRepository courtBlockRepository;
    private final CourtReviewRepository courtReviewRepository;
    private final WaitingListRepository waitingListRepository;
    private final UserNotificationRepository notificationRepository;
    private final OpenMatchRepository openMatchRepository;
    private final OpenMatchJoinRequestRepository joinRequestRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    private final LocalDate today = LocalDate.now();

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail("demo01@sportsbooking.com")) {
            return;
        }
        List<Venue> venues = seedVenues();
        List<Court> courts = seedCourts(venues);
        List<User> users = seedUsers();
        List<Package> packages = seedPackages();
        seedUserPackages(users, packages);
        List<Booking> bookings = seedBookings(users, courts);
        seedPayments(bookings);
        seedCourtBlocks(courts);
        seedReviews(users, courts);
        seedWaitingList(users, courts);
        seedNotifications(users);
        seedOpenMatches(users, bookings);
        seedTeams(users);
        seedTournaments(users);
        seedAuditLogs(users, bookings, courts);
        System.out.println("✅ Datos de demostración cargados (usuarios demo01..demo12@sportsbooking.com / " + DEMO_PASSWORD + ")");
    }

    private List<Venue> seedVenues() {
        String[][] data = {
                {"Sede Sur", "Av. Sur 210, Chorrillos"}, {"Sede Este", "Jr. Este 88, Ate"},
                {"Sede Oeste", "Av. Oeste 501, San Miguel"}, {"Sede Miraflores", "Calle Los Pinos 340, Miraflores"},
                {"Sede San Isidro", "Av. Javier Prado 1200, San Isidro"}, {"Sede Surco", "Av. Caminos del Inca 780, Surco"},
                {"Sede La Molina", "Av. La Molina 2450, La Molina"}, {"Sede Callao", "Av. Faucett 3100, Callao"},
        };
        for (int i = 0; i < data.length; i++) {
            if (!venueRepository.existsByNameIgnoreCase(data[i][0])) {
                venueRepository.save(Venue.builder().name(data[i][0]).address(data[i][1]).phone("9" + String.format("%08d", 10000000 + i * 137)).build());
            }
        }
        return venueRepository.findAll();
    }

    private List<Court> seedCourts(List<Venue> venues) {
        SportType[] sports = SportType.values();
        int[] capacity = {14, 4, 10, 8, 4, 2, 4, 4};
        int index = 0;
        for (Venue venue : venues) {
            if (!courtRepository.findByVenueId(venue.getId()).isEmpty()) continue;
            SportType sport = sports[index % sports.length];
            courtRepository.save(Court.builder().name("Cancha " + sport.name() + " " + venue.getName().replace("Sede ", ""))
                    .sportType(sport).capacity(capacity[index % capacity.length])
                    .priceBaseHour(new BigDecimal(40 + (index * 9) % 60))
                    .description("Cancha de " + sport.name().toLowerCase() + " de " + venue.getName()).venue(venue).build());
            index++;
        }
        return courtRepository.findAll();
    }

    private List<User> seedUsers() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        String[] names = {"Lucía Fernández", "Carlos Ramírez", "Valeria Torres", "Diego Castillo", "Camila Rojas", "Mateo Vargas",
                "Sofía Delgado", "Andrés Salazar", "Renata Paredes", "Joaquín Medina", "Fernanda Ríos", "Sebastián Cárdenas"};
        MembershipType[] memberships = MembershipType.values();
        // El admin y el personal de sede participan también en los datos, para que esas cuentas vean contenido.
        List<User> users = new ArrayList<>();
        for (String email : new String[]{"admin@sportsbooking.com", "venueadmin@sportsbooking.com", "recepcion@sportsbooking.com", "norteadmin@sportsbooking.com"}) {
            userRepository.findByEmail(email).ifPresent(users::add);
        }
        for (int i = 0; i < names.length; i++) {
            users.add(userRepository.save(User.builder().name(names[i]).email(String.format("demo%02d@sportsbooking.com", i + 1))
                    .phone(String.format("98800%04d", i + 1)).membershipType(memberships[i % memberships.length])
                    .password(hash).role(Role.USER).active(true).build()));
        }
        return users;
    }

    private List<Package> seedPackages() {
        String[][] data = {
                {"Pack 3 horas", "3", "150", "0.05", "20"}, {"Pack 8 horas", "8", "400", "0.12", "45"}, {"Pack 15 horas", "15", "720", "0.18", "75"},
                {"Pack 30 horas", "30", "1350", "0.25", "120"}, {"Pack Familiar 12h", "12", "600", "0.15", "60"}, {"Pack Corporativo 40h", "40", "1700", "0.30", "180"},
                {"Pack Fin de Semana 6h", "6", "300", "0.08", "30"},
        };
        for (String[] p : data) {
            packageRepository.save(Package.builder().name(p[0]).amountHours(Integer.parseInt(p[1])).price(new BigDecimal(p[2]))
                    .discountPercent(new BigDecimal(p[3])).validityDays(Integer.parseInt(p[4])).build());
        }
        return packageRepository.findAll();
    }

    private void seedUserPackages(List<User> users, List<Package> packages) {
        for (int i = 0; i < 10; i++) {
            Package pack = packages.get(i % packages.size());
            boolean expired = i % 5 == 4;
            UserPackage owned = userPackageRepository.save(UserPackage.builder().user(users.get(i)).packageDetails(pack)
                    .initialHours(pack.getAmountHours())
                    .expirationDate(expired ? LocalDateTime.now().minusDays(3) : LocalDateTime.now().plusDays(pack.getValidityDays())).build());
            owned.setRemainingHours(Math.max(0, pack.getAmountHours() - (i % 4) * 2));
            owned.setActive(!expired && owned.getRemainingHours() > 0);
            userPackageRepository.save(owned);
        }
    }

    private List<Booking> seedBookings(List<User> users, List<Court> courts) {
        List<Booking> bookings = new ArrayList<>();
        // 10 confirmadas futuras (base de los partidos abiertos), 6 completadas, 2 canceladas, 2 no-show y 2 confirmadas extra
        BookingStatus[] statuses = new BookingStatus[22];
        for (int i = 0; i < 22; i++) {
            statuses[i] = i < 10 ? BookingStatus.CONFIRMADA : i < 16 ? BookingStatus.COMPLETADA : i < 18 ? BookingStatus.CANCELADA : i < 20 ? BookingStatus.NO_SHOW : BookingStatus.CONFIRMADA;
        }
        for (int i = 0; i < statuses.length; i++) {
            Court court = courts.get(i % courts.size());
            BookingStatus status = statuses[i];
            boolean past = status == BookingStatus.COMPLETADA || status == BookingStatus.NO_SHOW;
            LocalDate date = past ? today.minusDays(1 + i % 12) : today.plusDays(1 + i % 9);
            LocalTime start = LocalTime.of(8 + (i * 3) % 13, 0);
            BigDecimal price = court.getPriceBaseHour();
            Booking booking = Booking.builder().user(users.get(i % users.size())).court(court).bookingDate(date)
                    .startTime(start).endTime(start.plusHours(1)).status(status)
                    .basePrice(price).dynamicSurcharges(BigDecimal.ZERO).appliedDiscount(BigDecimal.ZERO).totalPrice(price).build();
            if (status == BookingStatus.CANCELADA) {
                booking.setCancelledAt(LocalDateTime.now().minusHours(6));
                booking.setCancellationReason("El cliente canceló por cambio de planes");
            }
            if (status == BookingStatus.NO_SHOW) {
                booking.setPenaltyPercentage(BigDecimal.ONE);
                booking.setPenaltyAmount(price);
            }
            if (status == BookingStatus.COMPLETADA) {
                booking.setCheckedInAt(date.atTime(start).minusMinutes(5));
            }
            bookings.add(bookingRepository.save(booking));
        }
        return bookings;
    }

    private void seedPayments(List<Booking> bookings) {
        PaymentMethod[] methods = PaymentMethod.values();
        // reservas 0-1 rechazadas, 2-3 aprobadas, 10-15 (completadas) aprobadas, 16-17 (canceladas) reembolsadas, 18-19 (no-show) aprobadas
        int[] paid = {0, 1, 2, 3, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        int code = 1;
        for (int index : paid) {
            Booking booking = bookings.get(index);
            PaymentStatus status = index < 2 ? PaymentStatus.RECHAZADO : index == 16 || index == 17 ? PaymentStatus.REEMBOLSADO : PaymentStatus.APROBADO;
            Payment payment = Payment.builder().booking(booking).user(booking.getUser()).method(methods[index % methods.length]).status(status)
                    .amount(booking.getTotalPrice()).operationCode(String.format("SIM-DEMO%04d", code++)).build();
            if (status == PaymentStatus.RECHAZADO) payment.setRejectionReason("Pago simulado rechazado: tarjeta terminada en 0000");
            else payment.setPaidAt(LocalDateTime.now().minusDays(1));
            if (status == PaymentStatus.REEMBOLSADO) payment.setRefundedAt(LocalDateTime.now().minusHours(5));
            paymentRepository.save(payment);
        }
    }

    private void seedCourtBlocks(List<Court> courts) {
        CourtBlockType[] types = CourtBlockType.values();
        String[] reasons = {"Mantenimiento del piso", "Feriado nacional", "Torneo interno del club", "Cambio de red", "Pintado de líneas",
                "Evento corporativo", "Reparación de iluminación", "Día del deporte", "Limpieza profunda", "Clínica de entrenamiento"};
        for (int i = 0; i < 10; i++) {
            LocalTime start = LocalTime.of(6 + (i % 4) * 2, 0);
            courtBlockRepository.save(CourtBlock.builder().court(courts.get((i * 2) % courts.size())).blockDate(today.plusDays(2 + i))
                    .startTime(start).endTime(start.plusHours(2)).type(types[i % types.length]).reason(reasons[i]).build());
        }
    }

    private void seedReviews(List<User> users, List<Court> courts) {
        int[] ratings = {5, 4, 3, 5, 2, 4, 5, 1, 4, 5};
        String[] comments = {"Excelente cancha, muy bien cuidada", "Buena iluminación, volveré", "Regular, el piso estaba algo gastado",
                "Todo perfecto, atención de primera", "Muy caro para lo que ofrece", "Lindo lugar para jugar entre amigos",
                "La mejor cancha de la zona", "Pésima experiencia, contenido ofensivo retirado", "Buen ambiente y vestuarios limpios", "Recomendadísima"};
        for (int i = 0; i < 10; i++) {
            CourtReview review = CourtReview.builder().court(courts.get(i % courts.size())).user(users.get(i)).rating(ratings[i]).comment(comments[i]).build();
            review.setHidden(ratings[i] <= 2);
            courtReviewRepository.save(review);
        }
    }

    private void seedWaitingList(List<User> users, List<Court> courts) {
        for (int i = 0; i < 10; i++) {
            LocalTime start = LocalTime.of(17 + i % 4, 0);
            WaitingList entry = WaitingList.builder().user(users.get((i + 3) % users.size())).court(courts.get(i % courts.size()))
                    .desiredDate(today.plusDays(1 + i % 6)).desiredStartTime(start).desiredEndTime(start.plusHours(1)).notified(false).build();
            if (i % 3 == 0) entry.notifyUser(30);
            waitingListRepository.save(entry);
        }
    }

    private void seedNotifications(List<User> users) {
        NotificationType[] types = NotificationType.values();
        String[][] texts = {
                {"Reserva creada", "Tu reserva quedó confirmada."}, {"Pago aprobado", "Recibimos tu pago correctamente."},
                {"Pago no aprobado", "Puedes intentar nuevamente con otro método de pago."}, {"Reserva cancelada", "Tu reserva fue cancelada."},
                {"Reserva reprogramada", "Tu reserva fue actualizada a un nuevo horario."}, {"Check-in registrado", "Tu ingreso a la cancha fue registrado."},
                {"Hay un cupo disponible", "Se liberó un horario que estabas esperando."}, {"Invitación a un equipo", "Te invitaron a unirte a un equipo. Respóndela desde Equipos."},
        };
        for (int i = 0; i < 16; i++) {
            NotificationType type = types[i % types.length];
            String[] text = texts[i % texts.length];
            UserNotification notification = UserNotification.builder().user(users.get(i % users.size())).type(type).title(text[0]).message(text[1]).build();
            notification.setRead(i % 3 == 0);
            notificationRepository.save(notification);
        }
    }

    private void seedOpenMatches(List<User> users, List<Booking> bookings) {
        OpenMatchStatus[] statuses = {OpenMatchStatus.ABIERTO, OpenMatchStatus.ABIERTO, OpenMatchStatus.ABIERTO, OpenMatchStatus.COMPLETO, OpenMatchStatus.ABIERTO,
                OpenMatchStatus.ABIERTO, OpenMatchStatus.COMPLETO, OpenMatchStatus.ABIERTO, OpenMatchStatus.CERRADO, OpenMatchStatus.ABIERTO};
        JoinRequestStatus[] requestStatuses = JoinRequestStatus.values();
        for (int i = 0; i < 10; i++) {
            Booking booking = bookings.get(i);
            OpenMatch match = openMatchRepository.save(OpenMatch.builder().booking(booking).creator(booking.getUser())
                    .maxPlayers(4 + (i % 4) * 2).note("Partido amistoso nivel " + (i % 2 == 0 ? "intermedio" : "principiante") + ", ¡se buscan jugadores!")
                    .status(statuses[i]).build());
            for (int r = 1; r <= 2; r++) {
                User applicant = users.get((users.indexOf(booking.getUser()) + r * 3) % users.size());
                joinRequestRepository.save(OpenMatchJoinRequest.builder().openMatch(match).user(applicant)
                        .status(requestStatuses[(i + r) % requestStatuses.length]).build());
            }
        }
    }

    private void seedTeams(List<User> users) {
        String[] names = {"Los Halcones", "Titanes FC", "Padel Pro", "Raqueta Norte", "Águilas del Sur", "Smashers", "Los Cracks", "Voley Sol", "Bloque Azul", "Furia Roja"};
        TeamInvitationStatus[] statuses = TeamInvitationStatus.values();
        for (int i = 0; i < names.length; i++) {
            User owner = users.get(i);
            Team team = teamRepository.save(Team.builder().name(names[i]).description("Equipo " + names[i] + " · entrenamientos los fines de semana").owner(owner).build());
            teamMemberRepository.save(TeamMember.builder().team(team).user(owner).role(TeamMemberRole.OWNER).build());
            teamMemberRepository.save(TeamMember.builder().team(team).user(users.get((i + 1) % users.size())).role(TeamMemberRole.MEMBER).build());
            teamMemberRepository.save(TeamMember.builder().team(team).user(users.get((i + 2) % users.size())).role(TeamMemberRole.MEMBER).build());

            User invited = users.get((i + 8) % users.size());
            TeamInvitationStatus status = statuses[i % statuses.length];
            TeamInvitation invitation = TeamInvitation.builder().team(team).invitedUser(invited).invitedBy(owner).status(status).build();
            if (status != TeamInvitationStatus.PENDIENTE) invitation.setRespondedAt(LocalDateTime.now().minusHours(2));
            teamInvitationRepository.save(invitation);
            if (status == TeamInvitationStatus.ACEPTADA) {
                teamMemberRepository.save(TeamMember.builder().team(team).user(invited).role(TeamMemberRole.MEMBER).build());
            }
        }
    }

    private void seedTournaments(List<User> users) {
        String[] names = {"Copa Pádel Apertura", "Liga Tenis Clausura", "Torneo Vóley Verano", "Copa Fútbol 7", "Open Squash", "Campeonato Básquet 3x3",
                "Torneo Bádminton Dobles", "Copa Frontenis", "Interclubes Pádel", "Torneo Relámpago Tenis"};
        SportType[] sports = {SportType.PADEL, SportType.TENIS, SportType.VOLEY, SportType.FULBOL, SportType.SQUASH, SportType.BASQUET,
                SportType.BADMINTON, SportType.FRONTENIS, SportType.PADEL, SportType.TENIS};
        List<Tournament> tournaments = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            TournamentStatus status = i == 0 ? TournamentStatus.EN_CURSO : i == 1 ? TournamentStatus.FINALIZADO : TournamentStatus.INSCRIPCION;
            tournaments.add(tournamentRepository.save(Tournament.builder().name(names[i]).sportType(sports[i])
                    .startDate(i == 1 ? today.minusDays(20) : today.plusDays(3 + i * 4)).maxParticipants(i % 2 == 0 ? 8 : 16).status(status).build()));
        }
        // Copa en curso: 5 jugadores, round-robin de 10 partidos (6 ya jugados)
        List<TournamentParticipant> active = enroll(tournaments.get(0), users.subList(0, 5));
        int[][] scores = {{6, 3}, {4, 6}, {6, 2}, {3, 6}, {6, 4}, {5, 7}};
        int played = 0;
        for (int a = 0; a < active.size(); a++) {
            for (int b = a + 1; b < active.size(); b++) {
                TournamentMatch match = TournamentMatch.builder().tournament(tournaments.get(0)).playerOne(active.get(a).getUser()).playerTwo(active.get(b).getUser()).build();
                if (played < scores.length) play(match, active.get(a), active.get(b), scores[played++]);
                tournamentMatchRepository.save(match);
            }
        }
        // Liga finalizada: 4 jugadores, 6 partidos jugados
        List<TournamentParticipant> finished = enroll(tournaments.get(1), users.subList(5, 9));
        int[][] finalScores = {{6, 1}, {6, 4}, {3, 6}, {6, 2}, {7, 5}, {4, 6}};
        int k = 0;
        for (int a = 0; a < finished.size(); a++) {
            for (int b = a + 1; b < finished.size(); b++) {
                TournamentMatch match = TournamentMatch.builder().tournament(tournaments.get(1)).playerOne(finished.get(a).getUser()).playerTwo(finished.get(b).getUser()).build();
                play(match, finished.get(a), finished.get(b), finalScores[k++]);
                tournamentMatchRepository.save(match);
            }
        }
        // Torneos abiertos con inscritos
        enroll(tournaments.get(2), users.subList(9, 12));
        enroll(tournaments.get(3), users.subList(0, 4));
    }

    private List<TournamentParticipant> enroll(Tournament tournament, List<User> players) {
        List<TournamentParticipant> participants = new ArrayList<>();
        for (User player : players) {
            participants.add(participantRepository.save(TournamentParticipant.builder().tournament(tournament).user(player).build()));
        }
        return participants;
    }

    private void play(TournamentMatch match, TournamentParticipant one, TournamentParticipant two, int[] score) {
        match.setScoreOne(score[0]);
        match.setScoreTwo(score[1]);
        match.setCompleted(true);
        TournamentParticipant winner = score[0] > score[1] ? one : two;
        TournamentParticipant loser = winner == one ? two : one;
        winner.setWins(winner.getWins() + 1);
        winner.setPoints(winner.getPoints() + 3);
        loser.setLosses(loser.getLosses() + 1);
        participantRepository.save(winner);
        participantRepository.save(loser);
    }

    private void seedAuditLogs(List<User> users, List<Booking> bookings, List<Court> courts) {
        String[][] entries = {
                {"CREAR", "RESERVA"}, {"CREAR", "RESERVA"}, {"CANCELAR", "RESERVA"}, {"CHECK_IN", "RESERVA"}, {"NO_SHOW", "RESERVA"},
                {"REPROGRAMAR", "RESERVA"}, {"CREAR", "RESERVA"}, {"CHECK_IN", "RESERVA"}, {"CANCELAR", "RESERVA"}, {"CREAR", "RESERVA"},
                {"CHECK_IN", "RESERVA"}, {"NO_SHOW", "RESERVA"},
        };
        for (int i = 0; i < entries.length; i++) {
            Booking booking = bookings.get(i);
            User actor = users.get(i % users.size());
            Venue venue = booking.getCourt().getVenue();
            auditLogRepository.save(AuditLog.builder().actorUserId(actor.getId()).actorEmail(actor.getEmail()).action(entries[i][0])
                    .resourceType(entries[i][1]).resourceId(booking.getId()).detail("Datos de demostración: " + entries[i][0].toLowerCase() + " de reserva en " + booking.getCourt().getName())
                    .venueId(venue != null ? venue.getId() : null).build());
        }
    }
}
