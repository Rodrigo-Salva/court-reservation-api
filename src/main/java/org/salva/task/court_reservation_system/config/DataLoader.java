package org.salva.task.court_reservation_system.config;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.Package;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.enums.SportType;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.repository.PackageRepository;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final PackageRepository packageRepository;
    private final PasswordEncoder passwordEncoder;

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

        if (courtRepository.count() == 0) {
            courtRepository.save(Court.builder()
                    .name("Pádel Central 01").sportType(SportType.PADEL)
                    .capacity(4).priceBaseHour(new BigDecimal("68"))
                    .description("Cancha de pádel con cristal panorámico").build());
            courtRepository.save(Court.builder()
                    .name("Arena Fútbol 7").sportType(SportType.FULBOL)
                    .capacity(14).priceBaseHour(new BigDecimal("120"))
                    .description("Grass sintético, iluminación nocturna").build());
            courtRepository.save(Court.builder()
                    .name("Court One").sportType(SportType.TENIS)
                    .capacity(4).priceBaseHour(new BigDecimal("54"))
                    .description("Superficie dura, red profesional").build());
            courtRepository.save(Court.builder()
                    .name("Básquet Norte").sportType(SportType.BASQUET)
                    .capacity(10).priceBaseHour(new BigDecimal("90"))
                    .description("Cancha techada con tribuna").build());
            courtRepository.save(Court.builder()
                    .name("Vóley Playa").sportType(SportType.VOLEY)
                    .capacity(8).priceBaseHour(new BigDecimal("70"))
                    .description("Arena importada, red reglamentaria").build());
            courtRepository.save(Court.builder()
                    .name("Squash Room 3").sportType(SportType.SQUASH)
                    .capacity(2).priceBaseHour(new BigDecimal("45"))
                    .description("Sala con pared de vidrio").build());
            System.out.println("✅ 6 canchas de prueba creadas");
        }

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
}
