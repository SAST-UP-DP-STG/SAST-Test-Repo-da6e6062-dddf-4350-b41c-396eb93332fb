package com.example.cwe312.playcookie;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class Secure {

    private final ObjectMapper mapper = new ObjectMapper();

    // Serializes session data into a cookie named PLAY_SESSION
    @PostMapping(path = "/authenticate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authenticate(@RequestBody Map<String, Object> body) throws JsonProcessingException {
        String username = String.valueOf(body.getOrDefault("username", "")).trim();
        String password = String.valueOf(body.getOrDefault("password", "")).trim();
        if (username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Missing credentials"));
        }

        Map<String, Object> session = new HashMap<>();
        session.put("username", username);
        session.put("password", password);
        session.put("role", "user");

        String json = mapper.writeValueAsString(session);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        ResponseCookie cookie = ResponseCookie.from("PLAY_SESSION", encoded)
                .path("/")
                .httpOnly(false)
                .secure(false)
                .maxAge(3600)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("ok", true, "message", "Authenticated", "user", Map.of("username", username)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String cookieHeader = request.getHeader("Cookie");
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "cookie", cookieHeader == null ? "" : cookieHeader
        ));
    }
}

