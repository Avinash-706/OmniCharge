package com.omnicharge.notification.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.notification.dto.AdminReportRequest;
import com.omnicharge.notification.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminReportController {

    private final IEmailService emailService;
    private final LogEventPublisher logEventPublisher;

    /**
     * POST /api/admin/reports/send-email
     * Sends an executive summary report to the hardcoded admin email.
     * The frontend generates a PDF, Base64-encodes it, and sends it here for dispatch.
     */
    @PostMapping("/send-email")
    public ResponseEntity<ApiResponse<String>> sendReportEmail(@RequestBody AdminReportRequest request) {
        /* 
         * ==========================================
         * ⚠️ HARDCODED EMAIL OVERRIDE (ACTIVE) ⚠️
         * ==========================================
         * Currently, all reports are forced to send to your email for testing.
         * 
         * 🛠️ TO SWITCH TO THE ACTUAL ADMIN (DYNAMIC EMAIL):
         * 1. Delete or comment out the line below:
         *    String targetEmail = "avunashdhanuka@gmail.com";
         * 
         * 2. Uncomment the line below it:
         *    // String targetEmail = request.getAdminEmail();
         */
        String targetEmail = "avunashdhanuka@gmail.com";
        // String targetEmail = request.getAdminEmail();

        try {
            
            log.info("Processing executive report dispatch to: {}", targetEmail);
            String subject = request.getReportSubject() != null ? request.getReportSubject() : "OmniCharge Executive Summary Report";

            if (request.getPdfBase64() != null && !request.getPdfBase64().isEmpty()) {
                log.info("PDF attachment detected. Decoding Base64 ({} chars)...", request.getPdfBase64().length());
                byte[] pdfBytes = java.util.Base64.getDecoder().decode(request.getPdfBase64());
                log.info("Decoded PDF size: {} bytes", pdfBytes.length);

                emailService.sendEmailWithAttachment(
                    targetEmail,
                    subject,
                    "<p>Please find the attached <strong>OmniCharge Executive Confidential Report</strong> PDF.</p>",
                    pdfBytes,
                    "OmniCharge_Executive_Report.pdf"
                );
            } else {
                log.info("No attachment. Sending standard HTML email...");
                emailService.sendGenericHtmlEmail(targetEmail, subject, request.getReportHtml());
            }

            log.info("Executive report dispatched successfully to: {}", targetEmail);

            // Resilient centralized logging
            publishAdminLog("REPORT_DISPATCHED", "INFO",
                    "Executive report emailed successfully to " + targetEmail,
                    Map.of("recipient", targetEmail, "subject", subject));

            return ResponseEntity.ok(ApiResponse.success("Report emailed successfully to " + targetEmail, "SENT"));
        } catch (Exception e) {
            log.error("Failed to dispatch report to: {}", targetEmail, e); // Compilation fix

            publishAdminLog("REPORT_DISPATCH_FAILED", "ERROR",
                    "Failed to dispatch executive report: " + e.getMessage(),
                    Map.of("recipient", targetEmail, "error", e.getMessage()));

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to dispatch report: " + e.getMessage()));
        }
    }

    /**
     * Resilient log publisher — if logging-service/RabbitMQ is down,
     * this will NOT crash the main application. Fallback to console.
     */
    private void publishAdminLog(String eventType, String level, String message, Map<String, String> context) {
        try {
            LogEvent logEvent = new LogEvent();
            logEvent.setServiceName("notification-service");
            logEvent.setLevel(level);
            logEvent.setMessage(message);
            logEvent.setEventType(eventType);
            logEvent.setContext(new HashMap<>(context));
            logEvent.setLogger(this.getClass().getName());
            logEvent.setTimestamp(LocalDateTime.now());
            logEventPublisher.publish(logEvent);
        } catch (Exception e) {
            log.warn("[LOGGING FALLBACK] Failed to publish log to centralized service: {}", e.getMessage());
            log.info("[FALLBACK LOG] eventType={}, level={}, message={}", eventType, level, message);
        }
    }
}
