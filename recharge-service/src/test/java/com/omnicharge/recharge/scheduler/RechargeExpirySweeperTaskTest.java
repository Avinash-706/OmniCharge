package com.omnicharge.recharge.scheduler;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.UserProfileResponse;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.repository.RechargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RechargeExpirySweeperTaskTest {

    @Mock
    private RechargeRepository rechargeRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private LogEventPublisher logEventPublisher;

    @Captor
    private ArgumentCaptor<Recharge> rechargeCaptor;

    private RechargeExpirySweeperTask sweeperTask;

    @BeforeEach
    void setUp() {
        sweeperTask = new RechargeExpirySweeperTask(
                rechargeRepository, rabbitTemplate, userServiceClient, logEventPublisher);
    }

    @Test
    void testSweepExpiredRecharges_WithExpiredRecharges() {
        // Given
        LocalDate today = LocalDate.now();
        
        Recharge expiredRecharge1 = createRecharge("OMNI-EXP1", 100L, "9876543210", today.minusDays(5));
        Recharge expiredRecharge2 = createRecharge("OMNI-EXP2", 200L, "9123456789", today.minusDays(10));
        
        List<Recharge> expiredRecharges = Arrays.asList(expiredRecharge1, expiredRecharge2);
        
        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(RechargeStatus.SUCCESS, today))
                .thenReturn(expiredRecharges);
        
        UserProfileResponse userProfile = UserProfileResponse.builder()
                .email("test@example.com")
                .mobileNumber("9876543210")
                .build();
        
        when(userServiceClient.getUserById(anyLong()))
                .thenReturn(ApiResponse.success("User found", userProfile));

        // When
        sweeperTask.sweepExpiredRecharges();

        // Then
        verify(rechargeRepository, times(2)).save(rechargeCaptor.capture());
        List<Recharge> savedRecharges = rechargeCaptor.getAllValues();
        
        assertThat(savedRecharges).hasSize(2);
        assertThat(savedRecharges).allMatch(r -> r.getStatus() == RechargeStatus.EXPIRED);
        
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("omnicharge.exchange"), 
                eq("plan.expiry"), 
                any(Object.class)
        );
        
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testSweepExpiredRecharges_NoExpiredRecharges() {
        // Given
        LocalDate today = LocalDate.now();
        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(RechargeStatus.SUCCESS, today))
                .thenReturn(Collections.emptyList());

        // When
        sweeperTask.sweepExpiredRecharges();

        // Then
        verify(rechargeRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void testSweepExpiredRecharges_UserServiceUnavailable() {
        // Given
        LocalDate today = LocalDate.now();
        Recharge expiredRecharge = createRecharge("OMNI-EXP1", 100L, "9876543210", today.minusDays(5));
        
        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(RechargeStatus.SUCCESS, today))
                .thenReturn(Collections.singletonList(expiredRecharge));
        
        when(userServiceClient.getUserById(anyLong()))
                .thenThrow(new RuntimeException("User service unavailable"));

        // When
        sweeperTask.sweepExpiredRecharges();

        // Then
        verify(rechargeRepository).save(rechargeCaptor.capture());
        assertThat(rechargeCaptor.getValue().getStatus()).isEqualTo(RechargeStatus.EXPIRED);
        
        // Should still publish event even without user details
        verify(rabbitTemplate).convertAndSend(
                eq("omnicharge.exchange"), 
                eq("plan.expiry"), 
                any(Object.class)
        );
    }

    @Test
    void testSweepExpiredRecharges_UserServiceReturnsNull() {
        // Given
        LocalDate today = LocalDate.now();
        Recharge expiredRecharge = createRecharge("OMNI-EXP1", 100L, "9876543210", today.minusDays(5));
        
        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(RechargeStatus.SUCCESS, today))
                .thenReturn(Collections.singletonList(expiredRecharge));
        
        when(userServiceClient.getUserById(anyLong()))
                .thenReturn(ApiResponse.error("User not found"));

        // When
        sweeperTask.sweepExpiredRecharges();

        // Then
        verify(rechargeRepository).save(rechargeCaptor.capture());
        assertThat(rechargeCaptor.getValue().getStatus()).isEqualTo(RechargeStatus.EXPIRED);
        
        verify(rabbitTemplate).convertAndSend(
                eq("omnicharge.exchange"), 
                eq("plan.expiry"), 
                any(Object.class)
        );
    }

    @Test
    void testSweepExpiredRecharges_PartialFailure() {
        // Given
        LocalDate today = LocalDate.now();
        
        Recharge expiredRecharge1 = createRecharge("OMNI-EXP1", 100L, "9876543210", today.minusDays(5));
        Recharge expiredRecharge2 = createRecharge("OMNI-EXP2", 200L, "9123456789", today.minusDays(10));
        
        List<Recharge> expiredRecharges = Arrays.asList(expiredRecharge1, expiredRecharge2);
        
        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(RechargeStatus.SUCCESS, today))
                .thenReturn(expiredRecharges);
        
        UserProfileResponse userProfile = UserProfileResponse.builder()
                .email("test@example.com")
                .mobileNumber("9876543210")
                .build();
        
        when(userServiceClient.getUserById(anyLong()))
                .thenReturn(ApiResponse.success("User found", userProfile));
        
        // First save succeeds, second fails
        when(rechargeRepository.save(any(Recharge.class)))
                .thenReturn(expiredRecharge1)
                .thenThrow(new RuntimeException("Database error"));

        // When
        sweeperTask.sweepExpiredRecharges();

        // Then
        verify(rechargeRepository, times(2)).save(any());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testSweepExpiredRecharges_RabbitMQFailure() {
        // Given
        LocalDate today = LocalDate.now();
        Recharge expiredRecharge = createRecharge("OMNI-EXP1", 100L, "9876543210", today.minusDays(5));
        
        when(rechargeRepository.findByStatusAndPlanExpiryDateBefore(RechargeStatus.SUCCESS, today))
                .thenReturn(Collections.singletonList(expiredRecharge));
        
        UserProfileResponse userProfile = UserProfileResponse.builder()
                .email("test@example.com")
                .mobileNumber("9876543210")
                .build();
        
        when(userServiceClient.getUserById(anyLong()))
                .thenReturn(ApiResponse.success("User found", userProfile));
        
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When
        sweeperTask.sweepExpiredRecharges();

        // Then
        verify(rechargeRepository).save(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    private Recharge createRecharge(String rechargeId, Long userId, String mobile, LocalDate expiryDate) {
        Recharge recharge = new Recharge();
        recharge.setRechargeId(rechargeId);
        recharge.setUserId(userId);
        recharge.setMobileNumber(mobile);
        recharge.setOperatorId(1L);
        recharge.setOperatorName("Airtel");
        recharge.setPlanId(10L);
        recharge.setPlanName("Unlimited Plan");
        recharge.setAmount(new BigDecimal("599.00"));
        recharge.setPlanValidityDays(84);
        recharge.setPlanExpiryDate(expiryDate);
        recharge.setStatus(RechargeStatus.SUCCESS);
        recharge.setTransactionId("TXN123");
        return recharge;
    }
}
