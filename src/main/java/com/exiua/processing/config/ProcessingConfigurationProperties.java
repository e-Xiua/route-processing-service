package com.exiua.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for processing service behavior
 */
@Configuration
@EnableConfigurationProperties({
    ProcessingConfigurationProperties.class,
    GrpcPythonMrlAmisConfigurationProperties.class
})
@ConfigurationProperties(prefix = "processing")
@Validated
public class ProcessingConfigurationProperties {

    /**
     * Maximum number of concurrent processing requests
     */
    @Positive
    private int maxConcurrentRequests = 5;

    /**
     * Directory for temporary data files
     */
    @NotBlank
    private String tempDataDirectory = "/tmp/route-processing";

    /**
     * Hours after which temporary files are cleaned up
     */
    @Positive
    private int cleanupAfterHours = 2;

    // Getters and Setters
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    public String getTempDataDirectory() {
        return tempDataDirectory;
    }

    public void setTempDataDirectory(String tempDataDirectory) {
        this.tempDataDirectory = tempDataDirectory;
    }

    public int getCleanupAfterHours() {
        return cleanupAfterHours;
    }

    public void setCleanupAfterHours(int cleanupAfterHours) {
        this.cleanupAfterHours = cleanupAfterHours;
    }
}