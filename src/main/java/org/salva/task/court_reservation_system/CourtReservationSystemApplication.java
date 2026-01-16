package org.salva.task.court_reservation_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CourtReservationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourtReservationSystemApplication.class, args);
    }

}
