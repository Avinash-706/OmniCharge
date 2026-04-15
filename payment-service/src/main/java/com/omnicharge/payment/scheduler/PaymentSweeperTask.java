package com.omnicharge.payment.scheduler;

import com.omnicharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import com.omnicharge.payment.service.IPaymentService;
import com.omnicharge.payment.service.IRazorpayPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zombie Transaction Sweeper.
 * Runs every 5 minutes. Finds PENDING transactions older than 15 minutes
 * (user abandoned the Razorpay modal / closed the tab), marks them FAILED,
 * and publishes PaymentRejectedEvent so recharge-service can roll back too.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSweeperTask {

    private static final int TIMEOUT_MINUTES = 2;

    private final TransactionRepository transactionRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final LogEventPublisher logEventPublisher;
    @Lazy
    private final IPaymentService paymentService;
    private final IRazorpayPaymentService razorpayPaymentService;

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 60 * 1000) // every 1 min, 1 min after boot
    @Transactional
    public void sweepZombieTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<Transaction> zombies = transactionRepository.findByStatusAndCreatedDateBefore(
                PaymentStatus.PENDING, cutoff);

        if (zombies.isEmpty()) {
            return; // Nothing to sweep — silent exit for clean logs
        }

        log.warn("SWEEPER: Found {} zombie PENDING transactions older than {} minutes. Initiating SAGA rollback...",
                zombies.size(), TIMEOUT_MINUTES);

        for (Transaction txn : zombies) {
            try {
                if (txn.getRazorpayOrderId() != null) {
                    com.razorpay.Order order = razorpayPaymentService.fetchOrder(txn.getRazorpayOrderId());
                    String rzpStatus = order.get("status");

                    if ("paid".equalsIgnoreCase(rzpStatus) || "captured".equalsIgnoreCase(rzpStatus)) {
                        log.info("SWEEPER: Razorpay Order {} status is '{}'. Recovering SAGA as SUCCESS for {}", 
                                txn.getRazorpayOrderId(), rzpStatus, txn.getTransactionId());
                        
                        String paymentId = razorpayPaymentService.fetchPaymentIdForOrder(txn.getRazorpayOrderId());
                        if (paymentId == null) {
                            paymentId = "RECOVERED_" + txn.getRazorpayOrderId();
                        }
                        
                        paymentService.confirmPayment(txn.getTransactionId(), paymentId, "SYSTEM_AUTO_RECOVERY");
                        continue;
                    } else {
                        log.info("SWEEPER: Razorpay Order {} status is '{}'. Failing SAGA for {}", 
                                txn.getRazorpayOrderId(), rzpStatus, txn.getTransactionId());
                    }
                }

                paymentService.failPayment(txn.getTransactionId(), "Payment session expired. Please try again.");

            } catch (Exception e) {
                log.error("SWEEPER: Failed to roll back transaction {}: {}", txn.getTransactionId(), e.getMessage());
                // Fallback directly to fail payment if Razorpay API fails
                try {
                    paymentService.failPayment(txn.getTransactionId(), "Payment gateway timeout. Please try again.");
                } catch(Exception inner) {
                    log.error("SWEEPER: Inner fallback fail failed: {}", inner.getMessage());
                }
            }
        }

        log.info("SWEEPER: Completed. {} zombie transactions processed.", zombies.size());
    }
}
