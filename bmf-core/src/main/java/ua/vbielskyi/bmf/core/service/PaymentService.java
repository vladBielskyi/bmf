package ua.vbielskyi.bmf.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vbielskyi.bmf.common.model.order.OrderStatus;
import ua.vbielskyi.bmf.common.model.order.PaymentMethod;
import ua.vbielskyi.bmf.common.model.order.PaymentStatus;
import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.order.PaymentEntity;
import ua.vbielskyi.bmf.core.repository.order.OrderRepository;
import ua.vbielskyi.bmf.core.repository.order.PaymentRepository;
import ua.vbielskyi.bmf.core.service.notification.NotificationService;
import ua.vbielskyi.bmf.core.service.order.OrderWorkflowService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderWorkflowService orderWorkflowService;
    private final NotificationService notificationService;
    private final Map<PaymentMethod, PaymentProvider> paymentProviders;

    /**
     * Create a payment for an order
     */
    @Transactional
    public PaymentEntity createPayment(UUID orderId, PaymentMethod paymentMethod) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Check if payment already exists
        Optional<PaymentEntity> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        // Create new payment
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(order.getFinalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentEntity savedPayment = paymentRepository.save(payment);

        // Update order payment method
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        log.info("Created payment for order {}: method={}, amount={}",
                orderId, paymentMethod, payment.getAmount());

        return savedPayment;
    }

    /**
     * Initialize payment with provider
     */
    public PaymentInitResult initializePayment(UUID paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        OrderEntity order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", payment.getOrderId()));

        PaymentProvider provider = getPaymentProvider(payment.getPaymentMethod());

        try {
            // Initialize payment with the provider
            PaymentInitResult result = provider.initializePayment(payment, order);

            // Update payment with provider details
            payment.setPaymentProvider(provider.getProviderName());
            payment.setTransactionId(result.getTransactionId());
            payment.setPaymentDetails(result.getPaymentDetails());
            payment.setStatus(PaymentStatus.PROCESSING);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update order payment status
            order.setPaymentStatus(PaymentStatus.PROCESSING);
            order.setPaymentTransactionId(result.getTransactionId());
            orderRepository.save(order);

            log.info("Initialized payment {} with provider {}: transactionId={}",
                    paymentId, provider.getProviderName(), result.getTransactionId());

            return result;
        } catch (Exception e) {
            log.error("Error initializing payment {} with provider {}",
                    paymentId, provider.getProviderName(), e);

            // Update payment with error
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update order payment status
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            throw new PaymentProcessingException("Error initializing payment: " + e.getMessage(), e);
        }
    }

    /**
     * Process payment webhook callback
     */
    @Transactional
    public void processPaymentWebhook(String transactionId, PaymentStatus status, String providerReference) {
        PaymentEntity payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));

        OrderEntity order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", payment.getOrderId()));

        // Update payment status
        payment.setStatus(status);
        payment.setUpdatedAt(LocalDateTime.now());

        // Add provider reference if available
        if (providerReference != null) {
            String updatedDetails = payment.getPaymentDetails();
            if (updatedDetails == null) {
                updatedDetails = "";
            }
            updatedDetails += "\nProvider Reference: " + providerReference;
            payment.setPaymentDetails(updatedDetails);
        }

        paymentRepository.save(payment);

        // Update order payment status
        order.setPaymentStatus(status);
        orderRepository.save(order);

        // If payment completed, update order status to confirmed
        if (status == PaymentStatus.COMPLETED && order.getStatus() == OrderStatus.NEW) {
            orderWorkflowService.updateOrderStatus(order.getId(), OrderStatus.CONFIRMED, "payment-system");
            notificationService.sendPaymentConfirmation(order);
        } else if (status == PaymentStatus.FAILED) {
            // Notify customer of failed payment
            notificationService.sendPaymentFailureNotification(order);
        }

        log.info("Processed payment webhook for transaction {}: status={}", transactionId, status);
    }

    /**
     * Generate invoice for payment
     */
    public String generateInvoice(UUID paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        OrderEntity order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", payment.getOrderId()));

        // Generate invoice logic
        // This would typically involve creating a PDF or other document format

        log.info("Generated invoice for payment {}, order {}", paymentId, payment.getOrderId());

        return "invoice-" + payment.getId() + ".pdf";
    }

    /**
     * Get payment provider for payment method
     */
    private PaymentProvider getPaymentProvider(PaymentMethod paymentMethod) {
        PaymentProvider provider = paymentProviders.get(paymentMethod);

        if (provider == null) {
            throw new IllegalArgumentException("No payment provider configured for method: " + paymentMethod);
        }

        return provider;
    }

    /**
     * Payment initialization result
     */
    @lombok.Data
    @lombok.Builder
    public static class PaymentInitResult {
        private String transactionId;
        private String paymentUrl;
        private String paymentDetails;
        private Map<String, String> additionalData;
    }

    /**
     * Exception for payment processing errors
     */
    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message) {
            super(message);
        }

        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}