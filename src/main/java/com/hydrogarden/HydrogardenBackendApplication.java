package com.hydrogarden;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties
@ConfigurationPropertiesScan
public class HydrogardenBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(HydrogardenBackendApplication.class, args);
    }
}
