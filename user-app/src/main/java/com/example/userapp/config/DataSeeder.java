package com.example.userapp.config;

import com.example.userapp.entity.User;
import com.example.userapp.entity.UserRole;
import com.example.userapp.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
            admin.setRole(UserRole.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        }
        if (userRepository.findByEmail("user@example.com").isEmpty()) {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPasswordHash(passwordEncoder.encode("User123!"));
            user.setRole(UserRole.USER);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }
}
