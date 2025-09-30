package com.exiua.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Route Processing Service - Intermediary between Java services and Python MRL-AMIS model
 */
@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
public class RouteProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RouteProcessingServiceApplication.class, args);
    }
}