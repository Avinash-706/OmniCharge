package com.omnicharge.operator.client;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.NumverifyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumverifyClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private NumverifyClient numverifyClient;

    @Captor
    private ArgumentCaptor<LogEvent> logEventCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(numverifyClient, "apiKey", "test-api-key");
    }

    @Test
    void detectOperator_ShouldReturnResponse_WhenApiCallSuccessful() {
        // Arrange
        String mobileNumber = "9876543210";
        NumverifyResponse expectedResponse = new NumverifyResponse();
        expectedResponse.setValid(true);
        expectedResponse.setCarrier("Airtel");
        expectedResponse.setCountryCode("IN");
        
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenReturn(expectedResponse);

        // Act
        NumverifyResponse result = numverifyClient.detectOperator(mobileNumber);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValid()).isTrue();
        assertThat(result.getCarrier()).isEqualTo("Airtel");
        
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertThat(logEvent.getEventType()).isEqualTo("NUMVERIFY_API_CALL");
        assertThat(logEvent.getServiceName()).isEqualTo("operator-service");
        assertThat(logEvent.getContext()).containsEntry("responseStatus", "SUCCESS");
        assertThat(logEvent.getContext()).containsEntry("mobileNumber", mobileNumber);
        assertThat(logEvent.getContext()).containsEntry("carrierDetected", "Airtel");
    }

    @Test
    void detectOperator_ShouldReturnNull_WhenApiCallFails() {
        // Arrange
        String mobileNumber = "9876543210";
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenThrow(new RestClientException("API Error"));

        // Act
        NumverifyResponse result = numverifyClient.detectOperator(mobileNumber);

        // Assert
        assertThat(result).isNull();
        
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertThat(logEvent.getEventType()).isEqualTo("NUMVERIFY_API_CALL");
        assertThat(logEvent.getContext()).containsEntry("responseStatus", "FAILED");
        assertThat(logEvent.getContext()).containsEntry("errorMessage", "API Error");
    }

    @Test
    void detectOperator_ShouldConstructCorrectUrl() {
        // Arrange
        String mobileNumber = "9876543210";
        NumverifyResponse response = new NumverifyResponse();
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenReturn(response);

        // Act
        numverifyClient.detectOperator(mobileNumber);

        // Assert
        String expectedUrl = "http://apilayer.net/api/validate?access_key=test-api-key&number=919876543210";
        verify(restTemplate).getForObject(eq(expectedUrl), eq(NumverifyResponse.class));
    }

    @Test
    void detectOperator_ShouldLogResponseTime() {
        // Arrange
        String mobileNumber = "9876543210";
        NumverifyResponse response = new NumverifyResponse();
        response.setCarrier("Jio");
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenReturn(response);

        // Act
        numverifyClient.detectOperator(mobileNumber);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertThat(logEvent.getContext()).containsKey("responseTimeMs");
        assertThat(logEvent.getContext().get("responseTimeMs")).isInstanceOf(Long.class);
    }

    @Test
    void detectOperator_ShouldHandleNullCarrier() {
        // Arrange
        String mobileNumber = "9876543210";
        NumverifyResponse response = new NumverifyResponse();
        response.setValid(false);
        response.setCarrier(null);
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenReturn(response);

        // Act
        NumverifyResponse result = numverifyClient.detectOperator(mobileNumber);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCarrier()).isNull();
        
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertThat(logEvent.getContext()).containsEntry("carrierDetected", "null");
    }

    @Test
    void detectOperator_ShouldLogApiEndpoint() {
        // Arrange
        String mobileNumber = "9876543210";
        NumverifyResponse response = new NumverifyResponse();
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenReturn(response);

        // Act
        numverifyClient.detectOperator(mobileNumber);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertThat(logEvent.getContext()).containsEntry("apiEndpoint", "http://apilayer.net/api/validate");
    }

    @Test
    void detectOperator_ShouldLogErrorDetails_WhenExceptionOccurs() {
        // Arrange
        String mobileNumber = "9876543210";
        String errorMessage = "Connection timeout";
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenThrow(new RestClientException(errorMessage));

        // Act
        numverifyClient.detectOperator(mobileNumber);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertThat(logEvent.getContext()).containsEntry("errorMessage", errorMessage);
        assertThat(logEvent.getContext()).containsEntry("responseStatus", "FAILED");
        assertThat(logEvent.getContext()).containsKey("responseTimeMs");
    }

    @Test
    void detectOperator_ShouldHandleNullResponse() {
        // Arrange
        String mobileNumber = "9876543210";
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenReturn(null);

        // Act
        NumverifyResponse result = numverifyClient.detectOperator(mobileNumber);

        // Assert
        assertThat(result).isNull();
        
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertThat(logEvent.getContext()).containsEntry("carrierDetected", "null");
    }

    @Test
    void detectOperator_ShouldSetCorrectLogLevel() {
        // Arrange
        String mobileNumber = "9876543210";
        NumverifyResponse response = new NumverifyResponse();
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
            .thenReturn(response);

        // Act
        numverifyClient.detectOperator(mobileNumber);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertThat(logEvent.getLevel()).isEqualTo("INFO");
        assertThat(logEvent.getLogger()).isEqualTo("com.omnicharge.operator.client.NumverifyClient");
    }
}
