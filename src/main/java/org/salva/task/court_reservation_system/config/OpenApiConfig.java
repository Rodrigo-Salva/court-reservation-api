package org.salva.task.court_reservation_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI (Swagger)
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Court Reservation System API")
                        .version("1.0.0")
                        .description("Sistema de Reservas para Canchas Deportivas con gestión de precios dinámicos, " +
                                "paquetes prepagados, cancelaciones con penalizaciones y lista de espera.")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@sportsbooking.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.sportsbooking.com")
                                .description("Production Server")
                ));
    }
}
