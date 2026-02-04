package com.hydrogarden;

import com.hydrogarden.test.utils.HydrogardenIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationContextTest extends HydrogardenIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void mainApplicationBeanExists() {
        assertThat(applicationContext.containsBean("hydrogardenBackendApplication")).isTrue();
    }
}
