package com.omnicharge.payment.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class RestClientConfigTest {

    private RestClientConfig config;

    @BeforeEach
    void setUp() {
        config = new RestClientConfig();
    }

    @Test
    void testRestTemplateBean() {
        RestTemplate restTemplate = config.restTemplate();
        
        assertNotNull(restTemplate);
    }

    @Test
    void testRestTemplateIsLoadBalanced() throws NoSuchMethodException {
        Method method = RestClientConfig.class.getMethod("restTemplate");
        
        assertTrue(method.isAnnotationPresent(LoadBalanced.class));
    }

    @Test
    void testRestTemplateHasBeanAnnotation() throws NoSuchMethodException {
        Method method = RestClientConfig.class.getMethod("restTemplate");
        
        assertTrue(method.isAnnotationPresent(org.springframework.context.annotation.Bean.class));
    }

    @Test
    void testConfigurationAnnotation() {
        assertTrue(RestClientConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    void testMultipleRestTemplateCallsReturnNewInstances() {
        RestTemplate restTemplate1 = config.restTemplate();
        RestTemplate restTemplate2 = config.restTemplate();
        
        assertNotSame(restTemplate1, restTemplate2);
    }

    @Test
    void testRestTemplateHasDefaultInterceptors() {
        RestTemplate restTemplate = config.restTemplate();
        
        assertNotNull(restTemplate.getInterceptors());
    }
}
