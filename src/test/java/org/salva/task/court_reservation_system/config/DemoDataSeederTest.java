package org.salva.task.court_reservation_system.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class DemoDataSeederTest {

    private static final int MINIMUM_ROWS = 10;

    @Autowired private EntityManager entityManager;

    @Test
    void everyTableHasAtLeastTenRowsInDevProfile() {
        Map<String, Long> counts = new TreeMap<>();
        for (EntityType<?> entity : entityManager.getMetamodel().getEntities()) {
            counts.put(entity.getName(), entityManager.createQuery("select count(e) from " + entity.getName() + " e", Long.class).getSingleResult());
        }
        counts.forEach((name, total) -> System.out.println("DEMO-COUNT " + name + " = " + total));
        counts.forEach((name, total) -> assertTrue(total >= MINIMUM_ROWS, name + " tiene solo " + total + " registros"));
    }
}
