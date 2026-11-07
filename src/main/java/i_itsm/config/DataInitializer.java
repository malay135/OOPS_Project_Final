package i_itsm.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import i_itsm.model.User;
import i_itsm.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {

                User admin = new User(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        "ADMINISTRATOR"
                );

                userRepository.save(admin);
            }

            if (userRepository.findByUsername("technician").isEmpty()) {

                User technician = new User(
                        "technician",
                        passwordEncoder.encode("tech123"),
                        "TECHNICIAN"
                );

                userRepository.save(technician);
            }

            if (userRepository.findByUsername("employee").isEmpty()) {

                User employee = new User(
                        "employee",
                        passwordEncoder.encode("employee123"),
                        "EMPLOYEE"
                );

                userRepository.save(employee);
            }
        };
    }
}