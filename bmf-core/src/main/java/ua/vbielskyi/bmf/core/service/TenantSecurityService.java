package ua.vbielskyi.bmf.core.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantUserEntity;
import ua.vbielskyi.bmf.core.exception.SecurityException;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantUserRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSecurityService {

    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
  //  private final PasswordEncoder passwordEncoder;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-hours:24}")
    private int jwtExpirationHours;

    @Value("${security.telegram-token.encryption-key}")
    private String tokenEncryptionKey;

    /**
     * Authenticate tenant user
     */
    public String authenticateTenantUser(UUID tenantId, String username, String password) {
        // Verify tenant exists and is active
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SecurityException("Invalid tenant ID"));

        if (!tenant.getActive()) {
            throw new SecurityException("Tenant account is inactive");
        }

        // Find user
        TenantUserEntity user = tenantUserRepository.findByTenantIdAndEmail(tenantId, username)
                .orElseThrow(() -> new SecurityException("Invalid username or password"));

        if (!user.isActive()) {
            throw new SecurityException("User account is inactive");
        }

        // Validate password (mock implementation)
//        if (!isValidPassword(password, user.getPassword())) {
//            throw new SecurityException("Invalid username or password");
//        }

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        tenantUserRepository.save(user);

        // Generate JWT token
        return generateToken(tenantId, user.getId(), user.getUsername(), user.getRole());
    }

    /**
     * Generate JWT token for authenticated user
     */
    private String generateToken(UUID tenantId, UUID userId, String username, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiration = Date.from(
                LocalDateTime.now()
                        .plusHours(jwtExpirationHours)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId.toString());
        claims.put("userId", userId.toString());
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate JWT token
     */
//    public Map<String, Object> validateToken(String token) {
//        try {
//            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
//
//            Claims claims = Jwts.parserBuilder()
//                    .setSigningKey(key)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//
//            // Convert claims to map
//            Map<String, Object> result = new HashMap<>();
//            result.put("tenantId", UUID.fromString(claims.get("tenantId", String.class)));
//            result.put("userId", UUID.fromString(claims.get("userId", String.class)));
//            result.put("username", claims.getSubject());
//            result.put("role", claims.get("role", String.class));
//
//            return result;
//        } catch (Exception e) {
//            log.error("Error validating JWT token", e);
//            throw new SecurityException("Invalid or expired token");
//        }
//    }

    /**
     * Encrypt sensitive data like bot tokens
     */
    public String encryptBotToken(String token) {
        // In a real implementation, use a proper encryption library
        // This is just a placeholder for demonstration
        return "ENCRYPTED_" + token;
    }

    /**
     * Decrypt bot token
     */
    public String decryptBotToken(String encryptedToken) {
        // In a real implementation, use a proper decryption method
        if (encryptedToken.startsWith("ENCRYPTED_")) {
            return encryptedToken.substring("ENCRYPTED_".length());
        }
        throw new SecurityException("Invalid encrypted token format");
    }

    /**
     * Check if password matches stored hash
     */
//    private boolean isValidPassword(String rawPassword, String storedPassword) {
//        // In real implementation, use passwordEncoder.matches()
//        return passwordEncoder.matches(rawPassword, storedPassword);
//    }
}