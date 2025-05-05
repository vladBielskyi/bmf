package ua.vbielskyi.bmf.core.service;

import ua.vbielskyi.bmf.core.entity.order.OrderEntity;
import ua.vbielskyi.bmf.core.entity.order.PaymentEntity;

/**
 * Interface for payment processors
 */
public interface PaymentProvider {

    /**
     * Get the provider name
     */
    String getProviderName();

    /**
     * Initialize a payment
     *
     * @param payment Payment entity
     * @param order Related order entity
     * @return Payment initialization result
     */
    PaymentService.PaymentInitResult initializePayment(PaymentEntity payment, OrderEntity order);

    /**
     * Process payment callback
     *
     * @param transactionId Transaction ID
     * @param callbackData Callback data from payment provider
     * @return True if payment was successful
     */
    boolean processPaymentCallback(String transactionId, String callbackData);
}