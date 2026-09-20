package org.salva.task.court_reservation_system.config;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.Package;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.enums.SportType;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.repository.PackageRepository;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.repository.VenueRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("dev")
@Order(1)
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final PackageRepository packageRepository;
    private final PasswordEncoder passwordEncoder;
    private final VenueRepository venueRepository;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail("admin@sportsbooking.com")) {
            User admin = User.builder()
                    .name("Administrador")
                    .email("admin@sportsbooking.com")
                    .phone("999999999")
                    .membershipType(MembershipType.VIP)
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Administrador creado: admin@sportsbooking.com / admin123");
        }

        Venue defaultVenue = venueRepository.findAll().stream().findFirst()
                .orElseGet(() -> venueRepository.save(Venue.builder()
                        .name("Sede Central").address("Av. Principal 123").phone("999999999").build()));

        if (courtRepository.count() == 0) {
            courtRepository.save(Court.builder()
                    .name("Pádel Central 01").sportType(SportType.PADEL)
                    .capacity(4).priceBaseHour(new BigDecimal("68"))
                    .description("Cancha de pádel con cristal panorámico").venue(defaultVenue).build());
            courtRepository.save(Court.builder()
                    .name("Arena Fútbol 7").sportType(SportType.FULBOL)
                    .capacity(14).priceBaseHour(new BigDecimal("120"))
                    .description("Grass sintético, iluminación nocturna").venue(defaultVenue).build());
            courtRepository.save(Court.builder()
                    .name("Court One").sportType(SportType.TENIS)
                    .capacity(4).priceBaseHour(new BigDecimal("54"))
                    .description("Superficie dura, red profesional").venue(defaultVenue).build());
            courtRepository.save(Court.builder()
                    .name("Básquet Norte").sportType(SportType.BASQUET)
                    .capacity(10).priceBaseHour(new BigDecimal("90"))
                    .description("Cancha techada con tribuna").venue(defaultVenue).build());
            courtRepository.save(Court.builder()
                    .name("Vóley Playa").sportType(SportType.VOLEY)
                    .capacity(8).priceBaseHour(new BigDecimal("70"))
                    .description("Arena importada, red reglamentaria").venue(defaultVenue).build());
            courtRepository.save(Court.builder()
                    .name("Squash Room 3").sportType(SportType.SQUASH)
                    .capacity(2).priceBaseHour(new BigDecimal("45"))
                    .description("Sala con pared de vidrio").venue(defaultVenue).build());
            System.out.println("✅ 6 canchas de prueba creadas");
        }

        Venue northVenue = venueRepository.findByName("Sede Norte")
                .orElseGet(() -> venueRepository.save(Venue.builder()
                        .name("Sede Norte").address("Av. Norte 456").phone("999999990").build()));
        if (courtRepository.findByVenueId(northVenue.getId()).isEmpty()) {
            courtRepository.save(Court.builder()
                    .name("Pádel Norte 01").sportType(SportType.PADEL)
                    .capacity(4).priceBaseHour(new BigDecimal("60"))
                    .description("Cancha de pádel de la sede norte").venue(northVenue).build());
        }

        seedStaff("venueadmin@sportsbooking.com", "Admin Sede Central", "999999998", "venue123", Role.VENUE_ADMIN, defaultVenue);
        seedStaff("recepcion@sportsbooking.com", "Recepción Sede Central", "999999997", "recep123", Role.RECEPTIONIST, defaultVenue);
        seedStaff("norteadmin@sportsbooking.com", "Admin Sede Norte", "999999996", "norte123", Role.VENUE_ADMIN, northVenue);

        if (packageRepository.count() == 0) {
            packageRepository.save(Package.builder()
                    .name("Pack 5 horas").amountHours(5)
                    .price(new BigDecimal("280")).discountPercent(new BigDecimal("0.10"))
                    .validityDays(30).build());
            packageRepository.save(Package.builder()
                    .name("Pack 10 horas").amountHours(10)
                    .price(new BigDecimal("520")).discountPercent(new BigDecimal("0.15"))
                    .validityDays(60).build());
            packageRepository.save(Package.builder()
                    .name("Pack 20 horas").amountHours(20)
                    .price(new BigDecimal("960")).discountPercent(new BigDecimal("0.20"))
                    .validityDays(90).build());
            System.out.println("✅ 3 paquetes de prueba creados");
        }
    }

    private void seedStaff(String email, String name, String phone, String password, Role role, Venue venue) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.save(User.builder()
                .name(name).email(email).phone(phone)
                .membershipType(MembershipType.NINGUNA)
                .password(passwordEncoder.encode(password))
                .role(role).venue(venue).active(true)
                .build());
        System.out.println("✅ " + role + " de " + venue.getName() + " creado: " + email + " / " + password);
    }
}
