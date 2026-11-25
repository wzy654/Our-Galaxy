package com.ourgalaxy.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenMinutes;
    private final String issuer;

    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = properties.accessTokenMinutes();
        this.issuer = properties.issuer();
    }

    public IssuedToken issue(Long userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenMinutes * 60);
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, tokenId, expiresAt);
    }

    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new ParsedToken(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.getId(),
                    claims.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException(exception);
        }
    }

    public record IssuedToken(String token, String tokenId, Instant expiresAt) {
    }

    public record ParsedToken(Long userId, String email, String tokenId, Instant expiresAt) {
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(Throwable cause) {
            super(cause);
        }
    }
}
