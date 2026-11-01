package com.fielddispatch.auth;

import com.fielddispatch.domain.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final String issuer;
    private final long ttlSeconds;
    private final SecretKey key;

    public JwtService(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.ttlSeconds}") long ttlSeconds,
            @Value("${app.jwt.secret}") String secret
    ) {
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
        this.key = buildKey(secret);
    }

    public TokenInfo issueToken(UserAccount user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        String token = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setSubject(user.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("uid", user.getId())
                .claim("role", user.getRole())
                .claim("name", user.getFullName())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return new TokenInfo(token, exp.getEpochSecond());
    }

    public Claims parseAndValidate(String token) {
        Jws<Claims> parsed = Jwts.parserBuilder()
                .requireIssuer(issuer)
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        return parsed.getBody();
    }

    public String extractUsername(String token) {
        Claims claims = parseAndValidate(token);
        return claims.getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null) {
            throw new IllegalArgumentException("JWT secret is required");
        }

        String trimmed = secret.trim();
        if (trimmed.startsWith("base64:")) {
            byte[] raw = Decoders.BASE64.decode(trimmed.substring("base64:".length()));
            return Keys.hmacShaKeyFor(raw);
        }

        byte[] raw = trimmed.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(raw, 0, padded, 0, raw.length);
            for (int i = raw.length; i < padded.length; i++) {
                padded[i] = (byte) '0';
            }
            raw = padded;
        }
        return Keys.hmacShaKeyFor(raw);
    }

    public record TokenInfo(String token, long expiresAtEpochSeconds) {
    }
}
