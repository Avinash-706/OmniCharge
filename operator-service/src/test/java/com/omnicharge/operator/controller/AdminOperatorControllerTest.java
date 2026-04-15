package com.omnicharge.operator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.*;
import com.omnicharge.operator.entity.OperatorCategory;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.service.IOperatorService;
import com.omnicharge.operator.service.IPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminOperatorController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOperatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IOperatorService operatorService;

    @MockBean
    private IPlanService planService;

    @MockBean
    private LogEventPublisher logEventPublisher;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private OperatorResponse operatorResponse;
    private OperatorRequest operatorRequest;
    private PlanResponse planResponse;
    private PlanRequest planRequest;
    private PlanStatsResponse planStatsResponse;

    @BeforeEach
    void setUp() {
        operatorResponse = OperatorResponse.builder()
                .id(1L)
                .code("AIRTEL")
                .name("Airtel")
                .category(OperatorCategory.PREPAID)
                .logoUrl("http://logo.com")
                .isActive(true)
                .build();

        operatorRequest = new OperatorRequest(
                "Airtel",
                "AIRTEL",
                OperatorCategory.PREPAID,
                "http://logo.com"
        );

        planResponse = PlanResponse.builder()
                .id(1L)
                .operatorId(1L)
                .operatorName("Airtel")
                .planName("Unlimited 299")
                .price(new BigDecimal("299.00"))
                .validityDays(28)
                .category(PlanCategory.UNLIMITED)
                .isActive(true)
                .build();

        planRequest = new PlanRequest(
                "Unlimited 299",
                new BigDecimal("299.00"),
                28,
                null,
                null,
                null,
                null,
                PlanCategory.UNLIMITED
        );

        planStatsResponse = PlanStatsResponse.builder()
                .totalPlans(100L)
                .activePlans(80L)
                .inactivePlans(20L)
                .build();
    }

    @Test
    void getAllOperators_WithoutStatus_Success() throws Exception {
        when(operatorService.getOperatorsByStatus(null)).thenReturn(List.of(operatorResponse));

        mockMvc.perform(get("/api/admin/operators")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("AIRTEL"));
    }

    @Test
    void getAllOperators_WithActiveStatus_Success() throws Exception {
        when(operatorService.getOperatorsByStatus(true)).thenReturn(List.of(operatorResponse));

        mockMvc.perform(get("/api/admin/operators")
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].isActive").value(true));
    }

    @Test
    void getAllOperators_WithInactiveStatus_Success() throws Exception {
        OperatorResponse inactiveOperator = OperatorResponse.builder()
                .id(2L)
                .code("JIO")
                .name("Jio")
                .isActive(false)
                .build();

        when(operatorService.getOperatorsByStatus(false)).thenReturn(List.of(inactiveOperator));

        mockMvc.perform(get("/api/admin/operators")
                .param("status", "INACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].isActive").value(false));
    }

    @Test
    void createOperator_Success() throws Exception {
        when(operatorService.createOperator(any(OperatorRequest.class))).thenReturn(operatorResponse);

        mockMvc.perform(post("/api/admin/operators")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(operatorRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("AIRTEL"));
    }

    @Test
    void updateOperator_Success() throws Exception {
        when(operatorService.updateOperator(eq(1L), any(OperatorRequest.class))).thenReturn(operatorResponse);

        mockMvc.perform(put("/api/admin/operators/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(operatorRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Airtel"));
    }

    @Test
    void deleteOperator_Success() throws Exception {
        doNothing().when(operatorService).deleteOperator(1L);

        mockMvc.perform(delete("/api/admin/operators/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Operator deleted successfully"));
    }

    @Test
    void activateOperator_Success() throws Exception {
        when(operatorService.activateOperator(1L)).thenReturn(operatorResponse);

        mockMvc.perform(patch("/api/admin/operators/1/activate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void deactivateOperator_Success() throws Exception {
        OperatorResponse deactivatedOperator = OperatorResponse.builder()
                .id(1L)
                .code("AIRTEL")
                .name("Airtel")
                .isActive(false)
                .build();

        when(operatorService.deactivateOperator(1L)).thenReturn(deactivatedOperator);

        mockMvc.perform(patch("/api/admin/operators/1/deactivate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void getOperatorPlans_WithoutStatus_Success() throws Exception {
        when(planService.getPlansByOperatorAndStatus(1L, null)).thenReturn(List.of(planResponse));

        mockMvc.perform(get("/api/admin/operators/1/plans")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].planName").value("Unlimited 299"));
    }

    @Test
    void getOperatorPlans_WithActiveStatus_Success() throws Exception {
        when(planService.getPlansByOperatorAndStatus(1L, true)).thenReturn(List.of(planResponse));

        mockMvc.perform(get("/api/admin/operators/1/plans")
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].isActive").value(true));
    }

    @Test
    void createPlan_Success() throws Exception {
        when(planService.createPlan(eq(1L), any(PlanRequest.class))).thenReturn(planResponse);

        mockMvc.perform(post("/api/admin/operators/1/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planName").value("Unlimited 299"));
    }

    @Test
    void updatePlan_Success() throws Exception {
        when(planService.updatePlan(eq(1L), any(PlanRequest.class))).thenReturn(planResponse);

        mockMvc.perform(put("/api/admin/operators/plans/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.price").value(299.00));
    }

    @Test
    void deletePlan_Success() throws Exception {
        doNothing().when(planService).deletePlan(1L);

        mockMvc.perform(delete("/api/admin/operators/plans/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Plan deleted successfully"));
    }

    @Test
    void activatePlan_Success() throws Exception {
        when(planService.activatePlan(1L)).thenReturn(planResponse);

        mockMvc.perform(patch("/api/admin/operators/plans/1/activate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void deactivatePlan_Success() throws Exception {
        PlanResponse deactivatedPlan = PlanResponse.builder()
                .id(1L)
                .planName("Unlimited 299")
                .isActive(false)
                .build();

        when(planService.deactivatePlan(1L)).thenReturn(deactivatedPlan);

        mockMvc.perform(patch("/api/admin/operators/plans/1/deactivate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void searchAllPlans_WithAllParameters_Success() throws Exception {
        Page<PlanResponse> planPage = new PageImpl<>(List.of(planResponse));
        when(planService.searchPlansWithStatus(eq(1L), eq(PlanCategory.UNLIMITED), eq(true), any(Pageable.class)))
                .thenReturn(planPage);

        mockMvc.perform(get("/api/admin/operators/plans")
                .param("operatorId", "1")
                .param("category", "UNLIMITED")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "price")
                .param("sortDir", "ASC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].planName").value("Unlimited 299"));
    }

    @Test
    void searchAllPlans_WithDescendingSort_Success() throws Exception {
        Page<PlanResponse> planPage = new PageImpl<>(List.of(planResponse));
        when(planService.searchPlansWithStatus(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(planPage);

        mockMvc.perform(get("/api/admin/operators/plans")
                .param("sortBy", "price")
                .param("sortDir", "DESC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPlanStats_Success() throws Exception {
        when(planService.getPlanStats()).thenReturn(planStatsResponse);

        mockMvc.perform(get("/api/admin/operators/plans/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPlans").value(100))
                .andExpect(jsonPath("$.data.activePlans").value(80))
                .andExpect(jsonPath("$.data.inactivePlans").value(20));
    }
}
