package com.example.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Mock payment gateway — no real charge is made.
 *
 * Failure modes (in priority order):
 *   1. {@code booking.payment.always-fail=true}: every payment fails.
 *      Set via application.yml or @TestPropertySource for failure-path testing.
 *   2. Amount exactly 0.01: treated as a "magic failure" amount for targeted tests
 *      without needing a Spring context restart.
 */
@Service
public class MockPaymentGatewayImpl implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayImpl.class);

    private static final BigDecimal MAGIC_FAIL_AMOUNT = new BigDecimal("0.01");

    @Value("${booking.payment.always-fail:false}")
    private boolean alwaysFail;

    @Override
    public boolean charge(String customer, BigDecimal amount, String reference) {
        boolean success = !alwaysFail && amount.compareTo(MAGIC_FAIL_AMOUNT) != 0;
        log.info("PAYMENT [{}] customer={} amount={} ref={}", success ? "SUCCESS" : "FAILED", customer, amount, reference);
        return success;
    }

    /** Used by tests to toggle failure mode without restarting the Spring context. */
    public void setAlwaysFail(boolean alwaysFail) {
        this.alwaysFail = alwaysFail;
    }
}
