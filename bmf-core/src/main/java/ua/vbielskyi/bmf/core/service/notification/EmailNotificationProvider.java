package ua.vbielskyi.bmf.core.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ua.vbielskyi.bmf.core.entity.customer.CustomerEntity;
import ua.vbielskyi.bmf.core.entity.tenant.TenantEntity;
import ua.vbielskyi.bmf.core.repository.customer.CustomerRepository;
import ua.vbielskyi.bmf.core.repository.tenant.TenantRepository;

//import javax.mail.internet.MimeMessage;
import java.util.Optional;
import java.util.UUID;

/**
 * Provider for sending notifications via Email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationProvider implements NotificationProvider {

    //private final JavaMailSender mailSender;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    @Value("${notification.email.from:{}}")
    private String defaultFromEmail;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Send email notification to customer
     */
    public boolean sendNotification(UUID tenantId, Long telegramId, String message) {
        if (!emailEnabled) {
            log.debug("Email notifications are disabled");
            return false;
        }

        // Get customer email
        Optional<CustomerEntity> customerOpt = customerRepository.findByTenantIdAndTelegramId(tenantId, telegramId);
        if (customerOpt.isEmpty() || customerOpt.get().getEmail() == null) {
            log.debug("No email found for customer: telegramId={}, tenantId={}", telegramId, tenantId);
            return false;
        }

        // Get tenant information
        Optional<TenantEntity> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            log.error("Tenant not found: {}", tenantId);
            return false;
        }

        TenantEntity tenant = tenantOpt.get();
        CustomerEntity customer = customerOpt.get();

        try {
//            MimeMessage mimeMessage = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
//
//            helper.setFrom(defaultFromEmail);
//            helper.setTo(customer.getEmail());
//            helper.setSubject("Notification from " + tenant.getShopName());
//            helper.setText(message, true); // true indicates HTML content
//
//            mailSender.send(mimeMessage);

            log.debug("Sent email notification to {} for tenant {}", customer.getEmail(), tenantId);
            return true;
        } catch (Exception e) {
            log.error("Error sending email notification to {} for tenant {}",
                    customer.getEmail(), tenantId, e);
            return false;
        }
    }

    /**
     * Send email with attachment
     */
    public boolean sendEmailWithAttachment(UUID tenantId, Long telegramId, String subject,
                                           String message, String attachmentPath) {
        // Implementation for sending email with attachment
        return false;
    }
}