package i_itsm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            PrintWriter writer = response.getWriter();
            writer.write("{\"error\":\"Unauthorized: " + authException.getMessage().replace("\"", "\\\"") + "\"}");
            writer.flush();
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            PrintWriter writer = response.getWriter();
            writer.write("{\"error\":\"Forbidden: " + accessDeniedException.getMessage().replace("\"", "\\\"") + "\"}");
            writer.flush();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    AuthenticationEntryPoint authenticationEntryPoint,
                                                    AccessDeniedHandler accessDeniedHandler,
                                                    CorsConfigurationSource corsConfigurationSource)
            throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth

                // Public registration endpoint
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()

                // Anyone with a valid account can view/search incidents
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/incidents/**"
                ).hasAnyRole(
                    "EMPLOYEE",
                    "TECHNICIAN",
                    "ADMINISTRATOR"
                )

                // Any authenticated role can create an incident
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/incidents"
                ).hasAnyRole(
                    "EMPLOYEE",
                    "TECHNICIAN",
                    "ADMINISTRATOR"
                )

                // Only technicians and administrators can change status
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/incidents/*/status"
                ).hasAnyRole(
                    "TECHNICIAN",
                    "ADMINISTRATOR"
                )

                // Only administrators can assign incidents
                .requestMatchers("/api/assignments/**")
                .hasRole("ADMINISTRATOR")

                // Only administrators can view audit information
                .requestMatchers("/api/audit/**")
                .hasRole("ADMINISTRATOR")

                // All authenticated roles can access notifications
                .requestMatchers("/api/notifications/**")
                .authenticated()

                // Current user info endpoint
                .requestMatchers("/api/me", "/api/me/**").authenticated()

                // Technicians: technicians and administrators can view; only administrators can add or edit
                .requestMatchers(HttpMethod.GET, "/api/technicians/**").hasAnyRole("TECHNICIAN", "ADMINISTRATOR")
                .requestMatchers("/api/technicians/**").hasRole("ADMINISTRATOR")

                // SLA endpoint
                .requestMatchers("/api/sla/**").authenticated()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic
                .authenticationEntryPoint(authenticationEntryPoint)
            );

        return http.build();
    }
}