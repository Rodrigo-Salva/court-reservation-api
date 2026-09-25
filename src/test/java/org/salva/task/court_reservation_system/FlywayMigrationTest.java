package org.salva.task.court_reservation_system;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aplica las migraciones de Flyway sobre H2 (modo PostgreSQL) y hace que Hibernate valide el esquema
 * resultante contra las entidades. Falla si alguien cambia una entidad sin agregar su migración.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flywaycheck;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayMigrationTest {

    @Autowired private Flyway flyway;

    @Test
    void migrationsApplyCleanlyAndMatchTheEntityModel() {
        assertTrue(flyway.info().applied().length >= 1, "No se aplicó ninguna migración");
        assertEquals(0, flyway.info().pending().length, "Hay migraciones pendientes");
    }
}
