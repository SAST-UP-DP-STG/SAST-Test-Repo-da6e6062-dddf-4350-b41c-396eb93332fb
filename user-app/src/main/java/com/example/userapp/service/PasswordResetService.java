package com.example.userapp.service;

import com.example.userapp.entity.PasswordResetToken;
import com.example.userapp.entity.User;
import com.example.userapp.repository.PasswordResetTokenRepository;
import com.example.userapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final long VALIDITY_MINUTES = 60;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean confirmReset(String token, String newPassword) {
        Optional<PasswordResetToken> opt = tokenRepository.findByTokenAndUsedAtIsNull(token.trim());
        if (opt.isEmpty()) return false;
        PasswordResetToken prt = opt.get();
        if (prt.getExpiresAt() == null || !prt.getExpiresAt().isAfter(Instant.now())) return false;
        Long userId = prt.getUser().getId();
        prt.setUsedAt(Instant.now());
        tokenRepository.saveAndFlush(prt);
        String hash = passwordEncoder.encode(newPassword);
        int updated = userRepository.updatePasswordHash(userId, hash);
        return updated > 0;
    }

    @Transactional
    public String createResetToken(User user) {
        String token = generateToken();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setUser(user);
        prt.setExpiresAt(Instant.now().plusSeconds(VALIDITY_MINUTES * 60));
        tokenRepository.save(prt);
        return token;
    }

    public Optional<PasswordResetToken> findValidToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return tokenRepository.findByTokenAndUsedAtIsNull(token.trim())
                .filter(prt -> prt.getExpiresAt() != null && prt.getExpiresAt().isAfter(Instant.now()));
    }

    private static String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
