package ua.vbielskyi.bmf.core.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.bot.WebAppSettingsEntity;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.bot.WebAppSettingsRepository;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebAppService {

    private final WebAppSettingsRepository webAppSettingsRepository;
    private final CustomerRepository customerRepository;

    @Value("${webapp.base-url}")
    private String webAppBaseUrl;

    @Value("${webapp.jwt.secret}")
    private String jwtSecret;

    @Value("${webapp.jwt.expiration-minutes:60}")
    private int jwtExpirationMinutes;

    /**
     * Generate WebApp URL for a specific tenant
     */
    public String generateWebAppUrl(UUID tenantId, Long telegramId) {
        WebAppSettingsEntity settings = webAppSettingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WebApp settings", "tenantId", tenantId));

        CustomerEntity customer = customerRepository.findByTenantIdAndTelegramId(tenantId, telegramId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "telegramId", telegramId));

        // Generate JWT token for authentication
        String token = generateAuthToken(customer);

        // Build WebApp URL with parameters
        return String.format("%s?tenantId=%s&token=%s",
                webAppBaseUrl, tenantId, token);
    }

    /**
     * Generate WebApp URL with deep linking to a specific product
     */
    public String generateProductDeepLink(UUID tenantId, Long telegramId, UUID productId) {
        String baseUrl = generateWebAppUrl(tenantId, telegramId);
        return baseUrl + "&view=product&productId=" + productId;
    }

    /**
     * Generate WebApp URL with deep linking to a category
     */
    public String generateCategoryDeepLink(UUID tenantId, Long telegramId, UUID categoryId) {
        String baseUrl = generateWebAppUrl(tenantId, telegramId);
        return baseUrl + "&view=category&categoryId=" + categoryId;
    }

    /**
     * Generate WebApp URL with deep linking to cart
     */
    public String generateCartDeepLink(UUID tenantId, Long telegramId) {
        String baseUrl = generateWebAppUrl(tenantId, telegramId);
        return baseUrl + "&view=cart";
    }

    /**
     * Generate authentication token for WebApp
     */
    private String generateAuthToken(CustomerEntity customer) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        Date issuedAt = new Date();
        Date expiresAt = Date.from(LocalDateTime.now()
                .plusMinutes(jwtExpirationMinutes)
                .toInstant(ZoneOffset.UTC));

        return JWT.create()
                .withIssuer("bmf-api")
                .withSubject(customer.getId().toString())
                .withClaim("tenantId", customer.getTenantId().toString())
                .withClaim("telegramId", customer.getTelegramId())
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    /**
     * Track WebApp event
     */
    public void trackWebAppEvent(UUID tenantId, Long telegramId, String eventType, Map<String, Object> eventData) {
        Map<String, Object> eventRecord = new HashMap<>(eventData);
        eventRecord.put("tenantId", tenantId);
        eventRecord.put("telegramId", telegramId);
        eventRecord.put("eventType", eventType);
        eventRecord.put("timestamp", System.currentTimeMillis());

        // Store event in analytics system
        log.info("WebApp event: {}", eventRecord);
        // analyticsService.recordEvent(eventRecord);
    }
}