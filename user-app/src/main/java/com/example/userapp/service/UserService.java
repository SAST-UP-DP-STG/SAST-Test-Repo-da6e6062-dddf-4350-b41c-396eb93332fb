package com.example.userapp.service;

import com.example.userapp.entity.User;
import com.example.userapp.entity.UserRole;
import com.example.userapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User createUser(String email, String rawPassword, UserRole role, boolean enabled) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> updateUser(Long id, String email, UserRole role, Boolean enabled) {
        return userRepository.findById(id).map(user -> {
            if (email != null && !email.isBlank()) {
                user.setEmail(email);
            }
            if (role != null) {
                user.setRole(role);
            }
            if (enabled != null) {
                user.setEnabled(enabled);
            }
            return userRepository.save(user);
        });
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean setPassword(Long userId, String newPassword) {
        return userRepository.findById(userId).map(user -> {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }).orElse(false);
    }
}
