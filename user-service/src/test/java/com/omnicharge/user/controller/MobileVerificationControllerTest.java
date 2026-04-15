package com.omnicharge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.AuthResponse;
import com.omnicharge.user.dto.SendMobileOtpRequest;
import com.omnicharge.user.dto.VerifyMobileOtpRequest;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.service.MobileVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MobileVerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class MobileVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MobileVerificationService mobileVerificationService;

    @MockBean
    private LogEventPublisher logEventPublisher;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private org.springframework.data.redis.core.RedisTemplate redisTemplate;

    @MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @MockBean
    private com.omnicharge.user.filter.GatewayAuthenticationFilter gatewayAuthenticationFilter;

    private SendMobileOtpRequest sendOtpRequest;
    private VerifyMobileOtpRequest verifyOtpRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        sendOtpRequest = new SendMobileOtpRequest();
        sendOtpRequest.setMobileNumber("9876543210");

        verifyOtpRequest = new VerifyMobileOtpRequest();
        verifyOtpRequest.setMobileNumber("9876543210");
        verifyOtpRequest.setOtp("123456");

        authResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .role(Role.ROLE_USER)
                .fullName("Test User")
                .email("test@example.com")
                .authProvider(AuthProvider.LOCAL)
                .isProfileComplete(true)
                .isMobileVerified(true)
                .build();
    }

    @Test
    void sendOtp_Success() throws Exception {
        doNothing().when(mobileVerificationService).sendOtp(anyLong(), any(SendMobileOtpRequest.class));

        mockMvc.perform(post("/api/users/mobile-otp/send")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent successfully to mobile number"));
    }

    @Test
    void sendOtp_MissingHeader() throws Exception {
        mockMvc.perform(post("/api/users/mobile-otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendOtpRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void sendOtp_ValidationError_MissingMobileNumber() throws Exception {
        sendOtpRequest.setMobileNumber(null);

        mockMvc.perform(post("/api/users/mobile-otp/send")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendOtpRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtp_Success() throws Exception {
        when(mobileVerificationService.verifyOtp(anyLong(), any(VerifyMobileOtpRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/users/mobile-otp/verify")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.isMobileVerified").value(true));
    }

    @Test
    void verifyOtp_ValidationError_MissingOtp() throws Exception {
        verifyOtpRequest.setOtp(null);

        mockMvc.perform(post("/api/users/mobile-otp/verify")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtp_InvalidOtp() throws Exception {
        when(mobileVerificationService.verifyOtp(anyLong(), any(VerifyMobileOtpRequest.class)))
                .thenThrow(new com.omnicharge.common.exception.BadRequestException("Invalid OTP"));

        mockMvc.perform(post("/api/users/mobile-otp/verify")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isBadRequest());
    }
}
