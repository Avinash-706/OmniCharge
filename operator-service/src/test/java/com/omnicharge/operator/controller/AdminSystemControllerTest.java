package com.omnicharge.operator.controller;

import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.service.SystemCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminSystemController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemCacheService systemCacheService;

    @MockBean
    private LogEventPublisher logEventPublisher;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void rebuildCache_Success() throws Exception {
        doNothing().when(systemCacheService).rebuildRedisCache();

        mockMvc.perform(post("/api/admin/system/rebuild-cache")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Redis cache rebuild initiated and completed successfully"));

        verify(systemCacheService).rebuildRedisCache();
    }
}
