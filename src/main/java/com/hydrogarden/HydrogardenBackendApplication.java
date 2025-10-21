package com.hydrogarden;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EnableWebSecurity
@ImportAutoConfiguration({WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
        ServletWebServerFactoryAutoConfiguration.class,EmbeddedWebServerFactoryCustomizerAutoConfiguration.class})
public class HydrogardenBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(HydrogardenBackendApplication.class, args);
    }
}
