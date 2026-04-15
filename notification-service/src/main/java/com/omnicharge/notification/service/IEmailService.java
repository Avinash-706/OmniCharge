package com.omnicharge.notification.service;

import com.omnicharge.common.event.PaymentCompletedEvent;
import com.omnicharge.common.event.RechargeCompletedEvent;

public interface IEmailService {

    void sendPaymentConfirmation(String toEmail, PaymentCompletedEvent event);

    void sendRechargeConfirmation(String toEmail, RechargeCompletedEvent event);

    void sendPlanExpiryReminder(String toEmail, String userName, String operatorName, 
                                String planName, String mobileNumber, int daysLeft);

    void sendPlanExpiredNotification(String toEmail, String userName, String operatorName, 
                                     String planName, String mobileNumber);

    void sendGenericHtmlEmail(String toEmail, String subject, String htmlBody);

    void sendEmailWithAttachment(String toEmail, String subject, String body, byte[] attachment, String fileName);
}
