package ua.vbielskyi.bmf.core.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ua.vbielskyi.bmf.common.context.TenantContext;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Interceptor that extracts tenant ID from request headers
 * and sets it in the TenantContext. Also validates that the tenant exists.
 */
@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final TenantRepository tenantRepository;

    public TenantInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String tenantId = request.getHeader(TENANT_HEADER);
        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        // Skip tenant validation for non-tenant-specific endpoints
        String requestPath = request.getRequestURI();
        if (isPublicEndpoint(requestPath)) {
            return true;
        }

        // For tenant-specific endpoints, validate tenant ID
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("Missing tenant ID header for request: {}", requestPath);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Missing tenant ID header");
            return false;
        }

        try {
            UUID uuid = UUID.fromString(tenantId);

            // Verify tenant exists and is active
            Optional<TenantEntity> tenant = tenantRepository.findById(uuid);
            if (tenant.isEmpty()) {
                log.warn("Tenant not found: {}", uuid);
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.getWriter().write("Tenant not found");
                return false;
            }

            if (!tenant.get().isActive()) {
                log.warn("Tenant is inactive: {}", uuid);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("Tenant is inactive");
                return false;
            }

            // Set tenant context
            TenantContext.setCurrentTenant(uuid);
            log.debug("Set tenant context: {}", uuid);

            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tenant ID format: {}", tenantId);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Invalid tenant ID format");
            return false;
        } catch (Exception e) {
            log.error("Error processing tenant ID: {}", tenantId, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("Error processing tenant ID");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Clear the tenant context after the request is complete
        TenantContext.clear();
        log.debug("Cleared tenant context");
    }

    /**
     * Check if the endpoint is public (not tenant-specific)
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/public") ||
                path.startsWith("/api/auth") ||
                path.equals("/api/health") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}