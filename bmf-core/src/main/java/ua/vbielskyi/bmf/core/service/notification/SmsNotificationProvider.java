package ua.vbielskyi.bmf.core.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Provider for sending notifications via SMS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationProvider implements NotificationProvider {

    private final CustomerRepository customerRepository;

    @Value("${notification.sms.enabled}")
    private boolean smsEnabled;

    @Value("${notification.sms.provider.url}")
    private String smsProviderUrl;

    @Value("${notification.sms.provider.apiKey}")
    private String smsProviderApiKey;

    /**
     * Send SMS notification to customer
     */
    public boolean sendNotification(UUID tenantId, Long telegramId, String message) {
        if (!smsEnabled) {
            log.debug("SMS notifications are disabled");
            return false;
        }

        // Get customer phone number
        Optional<CustomerEntity> customerOpt = customerRepository.findByTenantIdAndTelegramId(tenantId, telegramId);
        if (customerOpt.isEmpty() || customerOpt.get().getPhone() == null) {
            log.debug("No phone found for customer: telegramId={}, tenantId={}", telegramId, tenantId);
            return false;
        }

        CustomerEntity customer = customerOpt.get();

        try {
            // This would typically use a third-party SMS API
            // For demonstration, we'll just log it
            log.info("Would send SMS to {} with message: {}", customer.getPhone(), message);

            return true;
        } catch (Exception e) {
            log.error("Error sending SMS notification to {} for tenant {}",
                    customer.getPhone(), tenantId, e);
            return false;
        }
    }
}