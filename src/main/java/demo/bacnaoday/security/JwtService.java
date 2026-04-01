package demo.bacnaoday.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    public static final String USER_ID_CLAIM = "uid";

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 UTF-8 bytes for HS256");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(AppUserDetails user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(USER_ID_CLAIM, user.getUserId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    public AppUserDetails parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = claims.get(USER_ID_CLAIM, Long.class);
        String username = claims.getSubject();
        if (userId == null || username == null) {
            throw new JwtException("missing claims");
        }
        return AppUserDetails.fromJwt(userId, username);
    }
}
