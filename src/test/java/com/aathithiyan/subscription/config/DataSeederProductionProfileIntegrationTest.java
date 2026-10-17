package com.aathithiyan.subscription.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "prod"})
class DataSeederProductionProfileIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Verify that DataSeeder bean is not registered in full Spring Boot application context under prod profile")
    void testDataSeederNotRegisteredInProductionContext() {
        assertThat(applicationContext.containsBean("dataSeeder"))
                .as("DataSeeder must not be registered in Spring ApplicationContext when prod profile is active")
                .isFalse();
    }
}
