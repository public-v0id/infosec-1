package ru.se.ifmo.infosec.lab1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.se.ifmo.infosec.lab1.entity.User;
import ru.se.ifmo.infosec.lab1.repository.UserRepository;

@SpringBootApplication
public class SecureApiApplication {

    @Bean
    CommandLineRunner initUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {

                User user = new User(
                        "admin",
                        passwordEncoder.encode("password")
                );

                userRepository.save(user);
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(SecureApiApplication.class, args);
    }
}