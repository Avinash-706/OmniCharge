package com.omnicharge.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportRequest {
    private String adminEmail;
    private String reportSubject;
    private String reportHtml;
    private String pdfBase64;
}
