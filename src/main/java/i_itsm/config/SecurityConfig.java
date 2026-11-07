package i_itsm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth

                // Anyone with a valid account can view/search incidents
                .requestMatchers(
                    org.springframework.http.HttpMethod.GET,
                    "/api/incidents/**"
                ).hasAnyRole(
                    "EMPLOYEE",
                    "TECHNICIAN",
                    "ADMINISTRATOR"
                )

                // Any authenticated role can create an incident
                .requestMatchers(
                    org.springframework.http.HttpMethod.POST,
                    "/api/incidents"
                ).hasAnyRole(
                    "EMPLOYEE",
                    "TECHNICIAN",
                    "ADMINISTRATOR"
                )

                // Only technicians and administrators can change status
                .requestMatchers(
                    org.springframework.http.HttpMethod.PUT,
                    "/api/incidents/*/status"
                ).hasAnyRole(
                    "TECHNICIAN",
                    "ADMINISTRATOR"
                )

                // Only technicians and administrators can assign incidents
                .requestMatchers("/api/assignments/**")
                .hasAnyRole(
                    "TECHNICIAN",
                    "ADMINISTRATOR"
                )

                // Only administrators can view audit information
                .requestMatchers("/api/audit/**")
                .hasRole("ADMINISTRATOR")

                // All authenticated roles can access notifications
                .requestMatchers("/api/notifications/**")
                .authenticated()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}