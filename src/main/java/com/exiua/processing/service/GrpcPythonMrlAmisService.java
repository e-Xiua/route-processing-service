package com.exiua.processing.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ProcessingMessageService messageService;
    private ManagedChannel channel;
    private RouteOptimizationServiceGrpc.RouteOptimizationServiceBlockingStub blockingStub;

    public GrpcPythonMrlAmisService(GrpcPythonMrlAmisConfigurationProperties grpcConfig,
                                   ProcessingMessageService messageService) {
        this.grpcConfig = grpcConfig;
        this.messageService = messageService;
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
        
        // Create stub WITHOUT deadline - we'll set fresh deadline per call
        blockingStub = RouteOptimizationServiceGrpc.newBlockingStub(channel);
        
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
     * Resultado de una operación de polling
     */
    public static class PollingResult {
        private final JobStatus status;
        private final String jobId;
        private final String message;
        private final float progress;
        private final RouteOptimization.RouteOptimizationResponse grpcResponse;
        private final int attempts;

        private PollingResult(Builder builder) {
            this.status = builder.status;
            this.jobId = builder.jobId;
            this.message = builder.message;
            this.progress = builder.progress;
            this.grpcResponse = builder.grpcResponse;
            this.attempts = builder.attempts;
        }

        // Getters
        public JobStatus getStatus() { return status; }
        public String getJobId() { return jobId; }
        public String getMessage() { return message; }
        public float getProgress() { return progress; }
        public RouteOptimization.RouteOptimizationResponse getGrpcResponse() { return grpcResponse; }
        public int getAttempts() { return attempts; }

        public boolean isCompleted() {
            return status == JobStatus.COMPLETED;
        }

        public boolean shouldRetry() {
            return !status.isFinal();
        }

        // Builder pattern
        public static class Builder {
            private JobStatus status;
            private String jobId;
            private String message;
            private float progress;
            private RouteOptimization.RouteOptimizationResponse grpcResponse;
            private int attempts;

            public Builder status(JobStatus status) {
                this.status = status;
                return this;
            }

            public Builder jobId(String jobId) {
                this.jobId = jobId;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder progress(float progress) {
                this.progress = progress;
                return this;
            }

            public Builder grpcResponse(RouteOptimization.RouteOptimizationResponse grpcResponse) {
                this.grpcResponse = grpcResponse;
                return this;
            }

            public Builder attempts(int attempts) {
                this.attempts = attempts;
                return this;
            }

            public PollingResult build() {
                return new PollingResult(this);
            }
        }

        @Override
        public String toString() {
            return String.format("PollingResult{status=%s, jobId='%s', progress=%.1f%%, attempts=%d, message='%s'}",
                    status, jobId, progress, attempts, message);
        }
    }

    /**
     * Process route optimization using gRPC communication with Python MRL-AMIS model
     */
// ...existing code...

    /**
     * Process route optimization using gRPC communication with Python MRL-AMIS model
     */
    public RouteOptimizationResult processRoute(RouteProcessingRequest request) throws Exception {
        logger.info("╔════════════════════════════════════════════════════════════════");
        logger.info("║ PROCESSING ROUTE VIA GRPC");
        logger.info("║ Route ID: {}", request.getRouteId());
        logger.info("║ User ID: {}", request.getUserId());
        logger.info("║ Number of POIs: {}", request.getPois() != null ? request.getPois().size() : 0);
        logger.info("╚════════════════════════════════════════════════════════════════");
        
        messageService.enviarMensajeEstado(
            "Started gRPC processing for route " + request.getRouteId()
        );
        
        try {
            // 1. Convertir solicitud a formato gRPC
            RouteOptimization.RouteOptimizationRequest grpcRequest = 
                    convertToGrpcRequest(request);
            
            logger.info("→ Sending gRPC request to Python service...");
            logGrpcRequest(grpcRequest);
            
            // 2. Enviar solicitud inicial
            RouteOptimization.RouteOptimizationResponse initialResponse = 
                    callWithRetry(grpcRequest);
            
            logger.info("← Received initial gRPC response");
            logGrpcResponse(initialResponse, "INITIAL");
            
            // 3. Crear resultado de polling inicial
            PollingResult pollingResult = GrpcResponseFactory.createPollingResult(initialResponse, 0);
            logger.info("📊 Polling Result: {}", pollingResult);
            
            // 4. Si está en cola o procesando, hacer polling
            if (pollingResult.shouldRetry()) {
                logger.info("⏳ Job {} is {}, starting polling...", 
                           pollingResult.getJobId(), pollingResult.getStatus());
                
                pollingResult = pollJobUntilComplete(
                    pollingResult.getJobId(), 
                    request.getRouteId()
                );
            }
            
            // 5. Verificar resultado final
            if (pollingResult.getStatus().isError()) {
                String errorMsg = String.format(
                    "Optimization failed with status %s: %s", 
                    pollingResult.getStatus(), 
                    pollingResult.getMessage()
                );
                logger.error("❌ {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            // 6. Convertir a resultado final
            RouteOptimizationResult result = GrpcResponseFactory.createOptimizationResult(
                pollingResult.getGrpcResponse()
            );
            
            logger.info("✅ Route processing completed successfully");
            logger.info("   Score: {}, Distance: {}km, Time: {}min", 
                       result.getOptimizationScore(),
                       result.getTotalDistanceKm(),
                       result.getTotalTimeMinutes());
            
            messageService.enviarMensajeResultados(
                String.format("Completed route %s - Score: %.2f, Distance: %.1fkm",
                             request.getRouteId(),
                             result.getOptimizationScore(),
                             result.getTotalDistanceKm())
            );
            
            return result;
            
        } catch (Exception e) {
            logger.error("💥 Error in gRPC route processing", e);
            messageService.enviarMensajeEstado(
                "Processing Error for route " + request.getRouteId() + ": " + e.getMessage()
            );
            throw new RuntimeException("Route processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test gRPC connection with health check
     */
    private void testConnection() {
        String requestId = "health-" + System.currentTimeMillis();
        
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
            
            // Send health check success message
            messageService.enviarMensajeEstado(
                "gRPC Health Check - Success: " + healthResponse.getStatus() + 
                " for " + grpcConfig.getHost() + ":" + grpcConfig.getPort()
            );
            
        } catch (Exception e) {
            // Send health check failure message
            messageService.enviarMensajeEstado(
                "gRPC Health Check - Failed: " + e.getMessage() + 
                " for " + grpcConfig.getHost() + ":" + grpcConfig.getPort()
            );
            
            throw new RuntimeException("Health check failed", e);
        }
    }

    /**
     * Call gRPC service with retry logic
     */
    private RouteOptimization.RouteOptimizationResponse callWithRetry(
            RouteOptimization.RouteOptimizationRequest request) throws Exception {
        
        Exception lastException = null;
        String requestId = request.getRouteId() + "-retry-" + System.currentTimeMillis();
        
        for (int attempt = 1; attempt <= grpcConfig.getMaxRetryAttempts(); attempt++) {
            try {
                logger.info("gRPC call attempt {} of {}", attempt, grpcConfig.getMaxRetryAttempts());
                
                // Create a NEW stub with a FRESH deadline for each retry attempt
                RouteOptimizationServiceGrpc.RouteOptimizationServiceBlockingStub stubWithDeadline = 
                    blockingStub.withDeadlineAfter(grpcConfig.getRequestTimeoutSeconds(), TimeUnit.SECONDS);
                
                return stubWithDeadline.optimizeRoute(request);
                
            } catch (StatusRuntimeException e) {
                lastException = e;
                logger.warn("gRPC call attempt {} failed: {}", attempt, e.getStatus().getDescription());
                
                // Send retry attempt message
                messageService.enviarMensajeEstado(
                    "gRPC Retry Attempt " + attempt + "/" + grpcConfig.getMaxRetryAttempts() + 
                    " for route " + request.getRouteId() + ": " + e.getStatus().getDescription()
                );
                
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
// ...existing code...

    /**
     * Factory para convertir respuestas gRPC a resultados Java
     */
    private static class GrpcResponseFactory {
        
        /**
         * Crea un PollingResult desde una respuesta gRPC
         */
        public static PollingResult createPollingResult(
                RouteOptimization.RouteOptimizationResponse response, 
                int attempts) {
            
            String statusStr = response.getStatus();
            JobStatus status = JobStatus.fromGrpcStatus(statusStr);
            
            return new PollingResult.Builder()
                    .status(status)
                    .jobId(response.getJobId())
                    .message(response.getMessage())
                    .progress(calculateProgress(response.getQueuePosition()))
                    .grpcResponse(response)
                    .attempts(attempts)
                    .build();
        }
        
        /**
         * Convierte una respuesta gRPC completa a RouteOptimizationResult
         */
        public static RouteOptimizationResult createOptimizationResult(
                RouteOptimization.RouteOptimizationResponse response) {
            
            RouteOptimizationResult result = new RouteOptimizationResult();
            result.setRequestId(response.getRouteId());
            result.setOptimizedRouteId(response.getJobId() + "-optimized");
            result.setAlgorithm("MRL-AMIS-gRPC");
            result.setProcessedAt(LocalDateTime.now());
            
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
        
        private static float calculateProgress(int queuePosition) {
            // Estimación simple: mientras menor la posición en cola, mayor el progreso
            return Math.max(0, 100 - (queuePosition * 10));
        }
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

        // Static factory method for success result
        public static RouteOptimizationResult success(String message, String jobId) {
            RouteOptimizationResult result = new RouteOptimizationResult();
            result.setRequestId(jobId);
            result.setOptimizedRouteId(jobId + "-optimized");
            result.setAlgorithm("MRL-AMIS-gRPC");
            // Optionally, you can add a field for message if needed
            return result;
        }

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


    public enum JobStatus {

    QUEUED("Trabajo en cola", false, false),
    PROCESSING("Procesando", false, false),
    COMPLETED("Completado exitosamente", true, false),
    FAILED("Falló", true, true),
    TIMEOUT("Tiempo agotado", true, true),
    UNKNOWN("Estado desconocido", false, false);

    private final String description;
    private final boolean isFinal;
    private final boolean isError;

    JobStatus(String description, boolean isFinal, boolean isError) {
        this.description = description;
        this.isFinal = isFinal;
        this.isError = isError;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isError() {
        return isError;
    }

    /**
     * Convierte un string de status de gRPC a enum
         */
    public static JobStatus fromGrpcStatus(String grpcStatus) {
        if (grpcStatus == null || grpcStatus.isEmpty()) {
            return UNKNOWN;
        }

        String statusUpper = grpcStatus.toUpperCase().trim();
        
        switch (statusUpper) {
            case "QUEUED":
                return QUEUED;
            case "PROCESSING":
                return PROCESSING;
            case "COMPLETED", "SUCCESS":
                return COMPLETED;
            case "FAILED", "ERROR":
                return FAILED;
            case "TIMEOUT":
                return TIMEOUT;
            default:
                return UNKNOWN;
        }
    }

}

// ...existing code...

    /**
     * Hace polling del estado del trabajo hasta que se complete
     */
    private PollingResult pollJobUntilComplete(String jobId, String routeId) {
        int maxAttempts = 60; // 60 intentos
        int delaySeconds = 5; // 5 segundos entre intentos
        int totalMaxSeconds = maxAttempts * delaySeconds;
        
        logger.info("╔════════════════════════════════════════════════════════════════");
        logger.info("║ STARTING POLLING FOR JOB: {}", jobId);
        logger.info("║ Max attempts: {}, Delay: {}s, Total max time: {}s", 
                   maxAttempts, delaySeconds, totalMaxSeconds);
        logger.info("╚════════════════════════════════════════════════════════════════");
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.info("🔄 Polling attempt {}/{} for job {}", attempt, maxAttempts, jobId);
                
                // CORRECCIÓN: Usar el método correcto del proto
                RouteOptimization.JobStatusRequest statusRequest = 
                    RouteOptimization.JobStatusRequest.newBuilder()
                        .setJobId(jobId)
                        .build();
                
                // CORRECCIÓN: Llamar al método getJobStatus con fresh deadline
                RouteOptimization.JobStatusResponse statusResponse = 
                    blockingStub.withDeadlineAfter(grpcConfig.getConnectionTimeoutSeconds(), TimeUnit.SECONDS)
                               .getJobStatus(statusRequest);
                
                logJobStatusResponse(statusResponse, attempt);
                
                // Crear resultado de polling
                PollingResult pollingResult = new PollingResult.Builder()
                        .status(JobStatus.fromGrpcStatus(statusResponse.getStatus()))
                        .jobId(jobId)
                        .message(statusResponse.getMessage())
                        .progress(statusResponse.getProgress())
                        .attempts(attempt)
                        .build();
                
                logger.info("📊 {}", pollingResult);
                
                // Enviar mensaje de estado a RabbitMQ
                messageService.enviarMensajeEstado(String.format(
                    "Route %s: %s (%.1f%%) - Attempt %d/%d",
                    routeId, 
                    pollingResult.getStatus().getDescription(),
                    pollingResult.getProgress(),
                    attempt,
                    maxAttempts
                ));
                
                // Si terminó (éxito o error)
                if (pollingResult.getStatus().isFinal()) {
                    if (pollingResult.isCompleted()) {
                        logger.info("✅ Job {} completed successfully after {} attempts", 
                                   jobId, attempt);
                        
                        // IMPORTANTE: Obtener el resultado completo con fresh deadline
                        RouteOptimization.JobResultRequest resultRequest = 
                            RouteOptimization.JobResultRequest.newBuilder()
                                .setJobId(jobId)
                                .build();
                        
                        RouteOptimization.RouteOptimizationResponse fullResponse = 
                            blockingStub.withDeadlineAfter(grpcConfig.getRequestTimeoutSeconds(), TimeUnit.SECONDS)
                                       .getJobResult(resultRequest);
                        
                        // Actualizar el pollingResult con la respuesta completa
                        pollingResult = new PollingResult.Builder()
                                .status(JobStatus.COMPLETED)
                                .jobId(jobId)
                                .message(statusResponse.getMessage())
                                .progress(100.0f)
                                .grpcResponse(fullResponse)
                                .attempts(attempt)
                                .build();
                        
                    } else {
                        logger.error("❌ Job {} failed with status: {}", 
                                    jobId, pollingResult.getStatus());
                    }
                    return pollingResult;
                }
                
                // Esperar antes del siguiente intento
                if (attempt < maxAttempts) {
                    logger.info("⏸️  Waiting {}s before next polling attempt...", delaySeconds);
                    Thread.sleep(delaySeconds * 1000);
                }
                
            } catch (StatusRuntimeException e) {
                logger.warn("⚠️  gRPC error during polling attempt {}: {}", 
                           attempt, e.getStatus().getDescription());
                
                if (attempt == maxAttempts) {
                    return new PollingResult.Builder()
                            .status(JobStatus.FAILED)
                            .jobId(jobId)
                            .message("Polling failed: " + e.getStatus().getDescription())
                            .attempts(attempt)
                            .build();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("🛑 Polling interrupted");
                return new PollingResult.Builder()
                        .status(JobStatus.FAILED)
                        .jobId(jobId)
                        .message("Polling interrupted")
                        .attempts(attempt)
                        .build();
            }
        }
        
        logger.error("⏱️  Polling timeout after {} seconds for job {}", totalMaxSeconds, jobId);
        return new PollingResult.Builder()
                .status(JobStatus.TIMEOUT)
                .jobId(jobId)
                .message("Timeout after " + totalMaxSeconds + " seconds")
                .attempts(maxAttempts)
                .build();
    }

    // Métodos de logging auxiliares
    
    private void logGrpcRequest(RouteOptimization.RouteOptimizationRequest request) {
        logger.debug("  RouteId: {}", request.getRouteId());
        logger.debug("  UserId: {}", request.getUserId());
        logger.debug("  POIs count: {}", request.getPoisCount());
        logger.debug("  Preferences: optimize_for={}, max_time={}, max_cost={}", 
                    request.getPreferences().getOptimizeFor(),
                    request.getPreferences().getMaxTotalTime(),
                    request.getPreferences().getMaxTotalCost());
    }
    
    private void logGrpcResponse(RouteOptimization.RouteOptimizationResponse response, String label) {
        logger.info("  [{}] Status: {}", label, response.getStatus());
        logger.info("  [{}] JobId: {}", label, response.getJobId());
        logger.info("  [{}] Message: {}", label, response.getMessage());
        logger.info("  [{}] Queue Position: {}", label, response.getQueuePosition());
    }
    
    private void logJobStatusResponse(RouteOptimization.JobStatusResponse response, int attempt) {
        logger.info("  └─ Status: {}", response.getStatus());
        logger.info("  └─ Progress: {:.1f}%", response.getProgress());
        logger.info("  └─ Message: {}", response.getMessage());
    }
}
// ...existing code...