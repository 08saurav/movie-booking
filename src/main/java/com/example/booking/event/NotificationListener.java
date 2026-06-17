package com.example.booking.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Sends mock email notifications for booking lifecycle events.
 *
 * Both listeners are:
 *   - @TransactionalEventListener(AFTER_COMMIT): the event fires only after the
 *     publishing transaction successfully commits. If the transaction rolls back,
 *     no email is sent.
 *   - @Async: runs on the "notification-*" thread pool (AsyncConfig), so the
 *     booking/cancellation response is returned to the customer immediately and
 *     the notification is delivered in the background.
 *
 * Events carry all needed data as primitives (no JPA entity refs), because there
 * is no Hibernate session or transaction on the async thread.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    /**
     * Thread names recorded by the async confirmed-notification handler.
     * Static so the queue is class-scoped regardless of Spring proxy wrapping;
     * tests drain it with a blocking {@code poll(timeout)} instead of sleeping.
     */
    public static final LinkedBlockingQueue<String> confirmedThreads = new LinkedBlockingQueue<>();

    /**
     * Thread names recorded by the async cancelled-notification handler.
     * See {@link #confirmedThreads} for the rationale.
     */
    public static final LinkedBlockingQueue<String> cancelledThreads = new LinkedBlockingQueue<>();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        confirmedThreads.offer(Thread.currentThread().getName());
        log.info("EMAIL SENT to {}: Booking {} confirmed for {} at {} — seat {} — total {}",
                event.customer(), event.bookingId(), event.movieTitle(),
                event.theaterName(), event.seatLabel(), event.finalPrice());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onBookingCancelled(BookingCancelledEvent event) {
        cancelledThreads.offer(Thread.currentThread().getName());
        log.info("EMAIL SENT to {}: Booking {} cancelled for {} at {} — seat {} — refund {}",
                event.customer(), event.bookingId(), event.movieTitle(),
                event.theaterName(), event.seatLabel(), event.refundAmount());
    }
}
