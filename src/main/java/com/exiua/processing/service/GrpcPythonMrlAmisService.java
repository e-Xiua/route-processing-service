package com.exiua.processing.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.exiua.processing.config.GrpcPythonMrlAmisConfigurationProperties;
import com.exiua.processing.model.ProcessingPOI;
import com.exiua.processing.model.RouteProcessingRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import route.optimization.RouteOptimization;
import route.optimization.RouteOptimizationServiceGrpc;

/**
 * gRPC client service for communicating with Python MRL-AMIS model
 */
@Service
public class GrpcPythonMrlAmisService {
    
    private static final Logger logger = LoggerFactory.getLogger(GrpcPythonMrlAmisService.class);
    
    private final GrpcPythonMrlAmisConfigurationProperties grpcConfig;
    private ManagedChannel channel;
    private RouteOptimizationServiceGrpc.RouteOptimizationServiceBlockingStub blockingStub;

    @Autowired
    public GrpcPythonMrlAmisService(GrpcPythonMrlAmisConfigurationProperties grpcConfig) {
        this.grpcConfig = grpcConfig;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing gRPC connection to Python MRL-AMIS service at {}:{}", 
                   grpcConfig.getHost(), grpcConfig.getPort());
        
        // Build gRPC channel
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forAddress(grpcConfig.getHost(), grpcConfig.getPort());
        
        if (!grpcConfig.isEnableTls()) {
            channelBuilder.usePlaintext();
        }
        
        channel = channelBuilder.build();
        
        blockingStub = RouteOptimizationServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(grpcConfig.getRequestTimeoutSeconds(), TimeUnit.SECONDS);
        
        // Test connection
        try {
            testConnection();
            logger.info("gRPC connection to Python MRL-AMIS service established successfully");
        } catch (Exception e) {
            logger.warn("Initial gRPC connection test failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            logger.info("Shutting down gRPC channel");
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while shutting down gRPC channel", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Process route optimization using gRPC communication with Python MRL-AMIS model
     */
    public RouteOptimizationResult processRoute(RouteProcessingRequest request) throws Exception {
        logger.info("=== PROCESSING ROUTE VIA GRPC ===");
        logger.info("Route ID: {}", request.getRouteId());
        logger.info("User ID: {}", request.getUserId());
        logger.info("Number of POIs: {}", request.getPois() != null ? request.getPois().size() : 0);
        
        try {
            // Convert Java request to gRPC request
            RouteOptimization.RouteOptimizationRequest grpcRequest = 
                    convertToGrpcRequest(request);
            
            logger.info("=== SENDING GRPC REQUEST TO PYTHON SERVICE ===");
            logger.info("gRPC Request: {}", grpcRequest.toString());
            
            // Call Python service via gRPC with retry logic
            RouteOptimization.RouteOptimizationResponse grpcResponse = 
                    callWithRetry(grpcRequest);
            
            logger.info("=== RECEIVED GRPC RESPONSE FROM PYTHON SERVICE ===");
            logger.info("Success: {}", grpcResponse.getSuccess());
            logger.info("Message: {}", grpcResponse.getMessage());
            
            // Convert gRPC response to Java result
            RouteOptimizationResult result = convertFromGrpcResponse(grpcResponse);
            result.setProcessedAt(LocalDateTime.now());
            
            logger.info("Route processing completed successfully via gRPC for route: {}", request.getRouteId());
            return result;
            
        } catch (StatusRuntimeException e) {
            logger.error("gRPC call failed with status: {}, description: {}", 
                        e.getStatus().getCode(), e.getStatus().getDescription());
            throw new RuntimeException("gRPC communication failed: " + e.getStatus().getDescription(), e);
        } catch (Exception e) {
            logger.error("Error in gRPC route processing", e);
            throw new RuntimeException("Route processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test gRPC connection with health check
     */
    private void testConnection() {
        try {
            RouteOptimization.HealthRequest healthRequest = 
                RouteOptimization.HealthRequest.newBuilder()
                    .setServiceName("route-processing-service")
                    .build();
            
            RouteOptimization.HealthResponse healthResponse = 
                blockingStub.withDeadlineAfter(grpcConfig.getConnectionTimeoutSeconds(), TimeUnit.SECONDS)
                           .healthCheck(healthRequest);
            
            logger.info("Health check response: healthy={}, status={}, version={}", 
                       healthResponse.getIsHealthy(), healthResponse.getStatus(), healthResponse.getVersion());
        } catch (Exception e) {
            throw new RuntimeException("Health check failed", e);
        }
    }

    /**
     * Call gRPC service with retry logic
     */
    private RouteOptimization.RouteOptimizationResponse callWithRetry(
            RouteOptimization.RouteOptimizationRequest request) throws Exception {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= grpcConfig.getMaxRetryAttempts(); attempt++) {
            try {
                logger.info("gRPC call attempt {} of {}", attempt, grpcConfig.getMaxRetryAttempts());
                return blockingStub.optimizeRoute(request);
                
            } catch (StatusRuntimeException e) {
                lastException = e;
                logger.warn("gRPC call attempt {} failed: {}", attempt, e.getStatus().getDescription());
                
                if (attempt < grpcConfig.getMaxRetryAttempts()) {
                    // Wait before retry (exponential backoff)
                    try {
                        long waitTime = (long) (Math.pow(2, attempt - 1) * 1000); // 1s, 2s, 4s, etc.
                        logger.info("Waiting {}ms before retry", waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry wait", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("All gRPC retry attempts failed", lastException);
    }

    /**
     * Convert Java request to gRPC request format
     */
    private RouteOptimization.RouteOptimizationRequest convertToGrpcRequest(
            RouteProcessingRequest request) {
        
        RouteOptimization.RouteOptimizationRequest.Builder builder = 
            RouteOptimization.RouteOptimizationRequest.newBuilder()
                .setRouteId(request.getRouteId() != null ? request.getRouteId() : "")
                .setUserId(request.getUserId() != null ? request.getUserId() : "");
        
        // Convert POIs
        if (request.getPois() != null) {
            for (ProcessingPOI poi : request.getPois()) {
                RouteOptimization.POI.Builder poiBuilder = 
                    RouteOptimization.POI.newBuilder()
                        .setId(poi.getId().intValue())
                        .setName(poi.getName() != null ? poi.getName() : "")
                        .setLatitude(poi.getLatitude() != null ? poi.getLatitude() : 0.0)
                        .setLongitude(poi.getLongitude() != null ? poi.getLongitude() : 0.0)
                        .setCategory(poi.getCategory() != null ? poi.getCategory() : "")
                        .setSubcategory(poi.getSubcategory() != null ? poi.getSubcategory() : "")
                        .setVisitDuration(poi.getVisitDuration() != null ? poi.getVisitDuration() : 60)
                        .setCost(poi.getCost() != null ? poi.getCost() : 0.0)
                        .setRating(poi.getRating() != null ? poi.getRating() : 4.0)
                        .setDescription(poi.getDescription() != null ? poi.getDescription() : "")
                        .setAccessibility(poi.getAccessibility() != null ? poi.getAccessibility() : true)
                        .setProviderId(poi.getProviderId() != null ? poi.getProviderId().intValue() : 0)
                        .setProviderName(poi.getProviderName() != null ? poi.getProviderName() : "");
                
                builder.addPois(poiBuilder.build());
            }
        }
        
        // Convert preferences
        RouteOptimization.RoutePreferences.Builder preferencesBuilder = 
            RouteOptimization.RoutePreferences.newBuilder();
        
        if (request.getPreferences() != null) {
            preferencesBuilder
                .setOptimizeFor(request.getPreferences().getOptimizeFor() != null ? 
                               request.getPreferences().getOptimizeFor() : "distance")
                .setMaxTotalTime(request.getPreferences().getMaxTotalTime() != null ? 
                               request.getPreferences().getMaxTotalTime() : 480)
                .setMaxTotalCost(request.getPreferences().getMaxTotalCost() != null ? 
                               request.getPreferences().getMaxTotalCost() : 500.0)
                .setAccessibilityRequired(request.getPreferences().getAccessibilityRequired() != null ? 
                                        request.getPreferences().getAccessibilityRequired() : false);
        } else {
            preferencesBuilder
                .setOptimizeFor("distance")
                .setMaxTotalTime(480)
                .setMaxTotalCost(500.0)
                .setAccessibilityRequired(false);
        }
        
        builder.setPreferences(preferencesBuilder.build());
        
        // Convert constraints
        RouteOptimization.RouteConstraints.Builder constraintsBuilder = 
            RouteOptimization.RouteConstraints.newBuilder();
        
        if (request.getConstraints() != null) {
            constraintsBuilder
                .setStartTime(request.getConstraints().getStartTime() != null ? 
                             request.getConstraints().getStartTime() : "08:00")
                .setLunchBreakRequired(request.getConstraints().getLunchBreakRequired() != null ? 
                                     request.getConstraints().getLunchBreakRequired() : true)
                .setLunchBreakDuration(request.getConstraints().getLunchBreakDuration() != null ? 
                                     request.getConstraints().getLunchBreakDuration() : 60);
        } else {
            constraintsBuilder
                .setStartTime("08:00")
                .setLunchBreakRequired(true)
                .setLunchBreakDuration(60);
        }
        
        builder.setConstraints(constraintsBuilder.build());
        
        return builder.build();
    }

    /**
     * Convert gRPC response to Java result format
     */
    private RouteOptimizationResult convertFromGrpcResponse(
            RouteOptimization.RouteOptimizationResponse response) {
        
        RouteOptimizationResult result = new RouteOptimizationResult();
        result.setRequestId(response.getRouteId());
        result.setOptimizedRouteId(response.getRouteId() + "-optimized");
        result.setAlgorithm("MRL-AMIS-gRPC");
        
        if (!response.getSuccess()) {
            throw new RuntimeException("Python MRL-AMIS optimization failed: " + response.getMessage());
        }
        
        if (response.hasResults()) {
            RouteOptimization.OptimizationResults results = response.getResults();
            
            result.setTotalDistanceKm(results.getTotalDistanceKm());
            result.setTotalTimeMinutes(results.getTotalTimeMinutes());
            result.setOptimizationScore(results.getOptimizationScore());
            
            // Convert optimized sequence
            List<OptimizedPOI> optimizedPOIs = new ArrayList<>();
            for (RouteOptimization.OptimizedPOI grpcPOI : results.getOptimizedSequenceList()) {
                OptimizedPOI optimizedPOI = new OptimizedPOI();
                optimizedPOI.setPoiId((long) grpcPOI.getPoiId());
                optimizedPOI.setName(grpcPOI.getPoiName());
                optimizedPOI.setLatitude(grpcPOI.getLatitude());
                optimizedPOI.setLongitude(grpcPOI.getLongitude());
                optimizedPOI.setVisitOrder(grpcPOI.getVisitOrder());
                optimizedPOI.setEstimatedVisitTime(grpcPOI.getEstimatedVisitTime());
                optimizedPOI.setArrivalTime(grpcPOI.getArrivalTime());
                optimizedPOI.setDepartureTime(grpcPOI.getDepartureTime());
                optimizedPOIs.add(optimizedPOI);
            }
            result.setOptimizedSequence(optimizedPOIs);
        }
        
        return result;
    }

    // Reuse existing inner classes from PythonMrlAmisService
    
    /**
     * Result of route optimization
     */
    public static class RouteOptimizationResult {
        private String requestId;
        private String optimizedRouteId;
        private List<OptimizedPOI> optimizedSequence;
        private Double totalDistanceKm;
        private Integer totalTimeMinutes;
        private String algorithm;
        private Double optimizationScore;
        private LocalDateTime processedAt;

        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getOptimizedRouteId() { return optimizedRouteId; }
        public void setOptimizedRouteId(String optimizedRouteId) { this.optimizedRouteId = optimizedRouteId; }
        public List<OptimizedPOI> getOptimizedSequence() { return optimizedSequence; }
        public void setOptimizedSequence(List<OptimizedPOI> optimizedSequence) { this.optimizedSequence = optimizedSequence; }
        public Double getTotalDistanceKm() { return totalDistanceKm; }
        public void setTotalDistanceKm(Double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }
        public Integer getTotalTimeMinutes() { return totalTimeMinutes; }
        public void setTotalTimeMinutes(Integer totalTimeMinutes) { this.totalTimeMinutes = totalTimeMinutes; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Double getOptimizationScore() { return optimizationScore; }
        public void setOptimizationScore(Double optimizationScore) { this.optimizationScore = optimizationScore; }
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    }

    public static class OptimizedPOI {
        private Long poiId;
        private String name;
        private Double latitude;
        private Double longitude;
        private Integer visitOrder;
        private Integer estimatedVisitTime;
        private String arrivalTime;
        private String departureTime;

        // Getters and Setters
        public Long getPoiId() { return poiId; }
        public void setPoiId(Long poiId) { this.poiId = poiId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public Integer getVisitOrder() { return visitOrder; }
        public void setVisitOrder(Integer visitOrder) { this.visitOrder = visitOrder; }
        public Integer getEstimatedVisitTime() { return estimatedVisitTime; }
        public void setEstimatedVisitTime(Integer estimatedVisitTime) { this.estimatedVisitTime = estimatedVisitTime; }
        public String getArrivalTime() { return arrivalTime; }
        public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
        public String getDepartureTime() { return departureTime; }
        public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    }
}