package org.salva.task.court_reservation_system.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Navegación pública de canchas (ver Explorar sin sesión).
                // Cualquier acción (reservar, comprar paquete, etc.) sigue exigiendo login.
                .requestMatchers(HttpMethod.GET,
                        "/api/courts", "/api/courts/sport-type/**",
                        "/api/courts/search", "/api/courts/price-range"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bookings/court/*/availability").permitAll()

                // Endpoints protegidos de administración (solo ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/courts/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/courts").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/courts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/courts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/courts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/packages/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/packages").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/packages/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/packages/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/packages/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/bookings").hasRole("ADMIN")

                // Cualquier otra petición requiere estar autenticado
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
