package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Collections;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public String generateToken(String subject, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(subject)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.warn("JWT validation failed", e);
            return false;
        }
    }

    public record JwtValidationResult(boolean valid, String subject, List<String> roles) {
    }

    public JwtValidationResult validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (claims.getExpiration().before(new Date())) {
                log.warn("JWT expired for subject {}", claims.getSubject());
                return new JwtValidationResult(false, null, Collections.emptyList());
            }
            String subject = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) {
                roles = Collections.emptyList();
            }
            return new JwtValidationResult(true, subject, roles);
        } catch (Exception e) {
            log.warn("JWT validation failed", e);
            return new JwtValidationResult(false, null, Collections.emptyList());
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) { // 256 bits for HS256
            throw new IllegalStateException("jwt.secret must be at least 256 bits (32 ASCII characters) long");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
