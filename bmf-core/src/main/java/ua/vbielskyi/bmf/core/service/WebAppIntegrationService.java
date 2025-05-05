package ua.vbielskyi.bmf.core.service.webapp;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.bot.WebAppSettingsEntity;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.exception.InvalidTokenException;
import ua.vbielskyi.bmf.core.exception.ResourceNotFoundException;
import ua.vbielskyi.bmf.core.repository.bot.WebAppSettingsRepository;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebAppIntegrationService {

    private final WebAppSettingsRepository webAppSettingsRepository;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    @Value("${webapp.jwt.secret}")
    private String jwtSecret;

    @Value("${webapp.jwt.expiration-minutes:60}")
    private int jwtExpirationMinutes;

    /**
     * Generate authentication token for WebApp
     */
    public String generateAuthToken(UUID tenantId, Long telegramId) {
        // Verify tenant exists and is active
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active");
        }

        // Get or create customer
        CustomerEntity customer = customerRepository.findByTenantIdAndTelegramId(tenantId, telegramId)
                .orElseGet(() -> {
                    CustomerEntity newCustomer = new CustomerEntity();
                    newCustomer.setTenantId(tenantId);
                    newCustomer.setTelegramId(telegramId);
                    newCustomer.setActive(true);
                    newCustomer.setCreatedAt(LocalDateTime.now());
                    newCustomer.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(newCustomer);
                });

        // Generate JWT token
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        Date issuedAt = new Date();
        Date expiresAt = Date.from(
                LocalDateTime.now()
                        .plusMinutes(jwtExpirationMinutes)
                        .atZone(ZoneId.systemDefault())
                        .toInstant());

        return JWT.create()
                .withIssuer("bmf-api")
                .withSubject(customer.getId().toString())
                .withClaim("tenantId", tenantId.toString())
                .withClaim("telegramId", telegramId)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    /**
     * Validate auth token
     */
    public Map<String, Object> validateAuthToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer("bmf-api")
                    .build()
                    .verify(token);

            // Extract claims
            UUID tenantId = UUID.fromString(jwt.getClaim("tenantId").asString());
            UUID customerId = UUID.fromString(jwt.getSubject());
            Long telegramId = jwt.getClaim("telegramId").asLong();

            // Verify tenant exists and is active
            TenantEntity tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

            if (!tenant.isActive()) {
                throw new IllegalStateException("Tenant is not active");
            }

            // Verify customer exists
            customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

            // Return validated claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("customerId", customerId);
            claims.put("tenantId", tenantId);
            claims.put("telegramId", telegramId);
            claims.put("exp", Instant.ofEpochSecond(jwt.getExpiresAt().getTime() / 1000));

            return claims;
        } catch (JWTVerificationException e) {
            log.error("JWT verification failed", e);
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    /**
     * Get WebApp settings for tenant
     */
    public WebAppSettingsEntity getWebAppSettings(UUID tenantId) {
        return webAppSettingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WebApp settings", "tenantId", tenantId));
    }

    /**
     * Generate WebApp initialization data
     */
    public Map<String, Object> generateWebAppInitData(UUID tenantId) {
        WebAppSettingsEntity settings = getWebAppSettings(tenantId);
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        Map<String, Object> initData = new HashMap<>();
        initData.put("tenantId", tenantId.toString());
        initData.put("shopName", tenant.getShopName());
        initData.put("logoUrl", tenant.getLogoUrl());
        initData.put("primaryColor", settings.getPrimaryColor());
        initData.put("secondaryColor", settings.getSecondaryColor());
        initData.put("accentColor", settings.getAccentColor());
        initData.put("fontFamily", settings.getFontFamily());
        initData.put("enableCart", settings.isEnableCart());
        initData.put("enableFavorites", settings.isEnableFavorites());
        initData.put("enableSearch", settings.isEnableSearch());
        initData.put("enableFilters", settings.isEnableFilters());
        initData.put("productsPerPage", settings.getProductsPerPage());

        return initData;
    }

    /**
     * Process data received from WebApp
     */
    public void processWebAppData(UUID tenantId, Long telegramId, String dataType, Map<String, Object> data) {
        log.info("Processing WebApp data: type={}, tenant={}, user={}", dataType, tenantId, telegramId);

        switch (dataType) {
            case "order":
                processOrderData(tenantId, telegramId, data);
                break;
            case "cart":
                processCartData(tenantId, telegramId, data);
                break;
            case "profile":
                processProfileData(tenantId, telegramId, data);
                break;
            case "feedback":
                processFeedbackData(tenantId, telegramId, data);
                break;
            default:
                log.warn("Unknown WebApp data type: {}", dataType);
                break;
        }
    }

    /**
     * Process order data from WebApp
     */
    private void processOrderData(UUID tenantId, Long telegramId, Map<String, Object> data) {
        // Implementation would extract order details and create an order
        log.info("Processing order data from WebApp: tenant={}, user={}", tenantId, telegramId);
    }

    /**
     * Process cart data from WebApp
     */
    private void processCartData(UUID tenantId, Long telegramId, Map<String, Object> data) {
        // Implementation would update shopping cart
        log.info("Processing cart data from WebApp: tenant={}, user={}", tenantId, telegramId);
    }

    /**
     * Process profile data from WebApp
     */
    private void processProfileData(UUID tenantId, Long telegramId, Map<String, Object> data) {
        // Implementation would update customer profile
        log.info("Processing profile data from WebApp: tenant={}, user={}", tenantId, telegramId);
    }

    /**
     * Process feedback data from WebApp
     */
    private void processFeedbackData(UUID tenantId, Long telegramId, Map<String, Object> data) {
        // Implementation would save customer feedback
        log.info("Processing feedback data from WebApp: tenant={}, user={}", tenantId, telegramId);
    }
}