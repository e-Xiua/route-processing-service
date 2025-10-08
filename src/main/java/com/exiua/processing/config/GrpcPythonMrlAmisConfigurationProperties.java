package com.exiua.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for gRPC communication with Python MRL-AMIS service
 */
@Component
@ConfigurationProperties(prefix = "grpc.python-mrl-amis")
@Validated
public class GrpcPythonMrlAmisConfigurationProperties {

    /**
     * Host where Python gRPC server is running
     */
    @NotBlank
    private String host = "localhost";

    /**
     * Port where Python gRPC server is listening
     */
    @Positive
    private int port = 50051;

    /**
     * Connection timeout in seconds
     */
    @Positive
    private long connectionTimeoutSeconds = 30;

    /**
     * Request timeout in seconds (should be long for MRL-AMIS processing)
     */
    @Positive
    private long requestTimeoutSeconds = 600; // 10 minutes

    /**
     * Maximum retry attempts for failed requests
     */
    @Positive
    private int maxRetryAttempts = 3;

    /**
     * Enable TLS for gRPC connection
     */
    private boolean enableTls = false;

    // Getters and Setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(long connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public long getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(long requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public boolean isEnableTls() {
        return enableTls;
    }

    public void setEnableTls(boolean enableTls) {
        this.enableTls = enableTls;
    }
}