package com.fielddispatch.web;

import com.fielddispatch.domain.UserAccount;
import com.fielddispatch.repo.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/bootstrap")
public class AdminBootstrapController {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapController(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/account")
    public ResponseEntity<?> createAccount(@RequestBody Map<String, Object> body) {
        String username = body.get("username").toString();
        String fullName = body.getOrDefault("fullName", username).toString();
        String role = body.getOrDefault("role", "DISPATCHER").toString();
        String password = body.get("password").toString();

        if (userAccountRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "username_taken"));
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setFullName(fullName);
        account.setRole(role.toUpperCase());
        account.setPasswordHash(passwordEncoder.encode(password));
        userAccountRepository.save(account);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", account.getId(),
                "username", account.getUsername(),
                "role", account.getRole()
        ));
    }
}
