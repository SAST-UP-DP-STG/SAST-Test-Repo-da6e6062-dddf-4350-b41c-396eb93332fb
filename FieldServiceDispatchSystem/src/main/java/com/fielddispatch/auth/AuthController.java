package com.fielddispatch.auth;

import com.fielddispatch.domain.UserAccount;
import com.fielddispatch.repo.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<UserAccount> found = userAccountRepository.findByUsername(request.username());
        if (found.isEmpty() || !passwordEncoder.matches(request.password(), found.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_credentials"));
        }

        UserAccount user = found.get();
        JwtService.TokenInfo token = jwtService.issueToken(user);
        return ResponseEntity.ok(new AuthResponse(
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                token.token(),
                token.expiresAtEpochSeconds()
        ));
    }

    public record LoginRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Size(min = 1, max = 200) String password
    ) {
    }

    public record AuthResponse(String username, String fullName, String role, String token, long expiresAtEpochSeconds) {
    }
}
