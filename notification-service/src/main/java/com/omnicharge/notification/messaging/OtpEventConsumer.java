package com.omnicharge.notification.messaging;

import com.omnicharge.notification.dto.OtpEvent;
import com.omnicharge.notification.service.ISmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpEventConsumer {

    private final ISmsService smsService;

    @RabbitListener(queues = "notification.otp.queue")
    public void consumeOtpEvent(OtpEvent event) {
        log.info("Received mobile OTP event for number: {}", event.getMobileNumber());
        
        try {
            String message = String.format("Your OmniCharge verification OTP is: %s. Valid for 5 minutes.", event.getOtp());
            smsService.sendSms(event.getMobileNumber(), message);
            log.info("Successfully dispatched OTP verification to Twilio SMS");
        } catch (Exception e) {
            log.error("Failed to send OTP SMS to {}: {}", event.getMobileNumber(), e.getMessage());
        }
    }
}
