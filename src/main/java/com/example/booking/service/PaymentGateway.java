package com.example.booking.service;

import java.math.BigDecimal;

/**
 * Abstracts payment processing. The only implementation is {@link MockPaymentGatewayImpl};
 * a real gateway would be substituted here without touching booking logic.
 */
public interface PaymentGateway {

    /**
     * @param customer  the authenticated username making the payment
     * @param amount    the amount to charge
     * @param reference an opaque booking reference for the payment record
     * @return {@code true} if payment succeeded, {@code false} if it failed
     */
    boolean charge(String customer, BigDecimal amount, String reference);
}
