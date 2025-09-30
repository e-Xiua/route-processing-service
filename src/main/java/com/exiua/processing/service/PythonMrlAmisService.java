package com.exiua.processing.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.exiua.processing.model.ProcessingPOI;
import com.exiua.processing.model.RouteProcessingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service to communicate with Python MRL-AMIS model
 */
@Service
public class PythonMrlAmisService {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonMrlAmisService.class);
    
    private final ObjectMapper objectMapper;
    
    @Value("${python.mrl-amis.script-path}")
    private String pythonScriptPath;
    
    @Value("${python.mrl-amis.working-directory}")
    private String workingDirectory;
    
    @Value("${python.mrl-amis.conda-env-name:mrl-amis-env}")
    private String condaEnvName;
    
    @Value("${python.mrl-amis.timeout-minutes:10}")
    private long timeoutMinutes;
    
    @Value("${processing.temp-data-directory:/tmp/route-processing}")
    private String tempDataDirectory;

    public PythonMrlAmisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Process route optimization using Python MRL-AMIS model
     */
    public RouteOptimizationResult processRoute(RouteProcessingRequest request) throws Exception {
        logger.info("Starting route processing for route: {}", request.getRouteId());
        
        // Create temp directory for this request
        String requestId = UUID.randomUUID().toString();
        Path tempDir = createTempDirectory(requestId);
        
        try {
            // Write input data to JSON file
            Path inputFile = writeInputData(tempDir, request);
            
            // Execute Python script
            Path outputFile = executePythonScript(tempDir, inputFile, requestId);
            
            // Read and parse results
            RouteOptimizationResult result = parseResults(outputFile);
            result.setRequestId(requestId);
            result.setProcessedAt(LocalDateTime.now());
            
            logger.info("Route processing completed successfully for route: {}", request.getRouteId());
            return result;
            
        } finally {
            // Cleanup temp files
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * Create temporary directory for processing
     */
    private Path createTempDirectory(String requestId) throws IOException {
        Path tempDir = Paths.get(tempDataDirectory, requestId);
        Files.createDirectories(tempDir);
        logger.debug("Created temp directory: {}", tempDir);
        return tempDir;
    }

    /**
     * Write input data to JSON file for Python processing
     */
    private Path writeInputData(Path tempDir, RouteProcessingRequest request) throws IOException {
        Path inputFile = tempDir.resolve("input_data.json");
        
        // Convert to Python-compatible format
        PythonInputData pythonData = convertToPythonFormat(request);
        
        try (FileWriter writer = new FileWriter(inputFile.toFile())) {
            objectMapper.writeValue(writer, pythonData);
        }
        
        logger.debug("Input data written to: {}", inputFile);
        return inputFile;
    }

    /**
     * Convert Java request to Python-compatible format
     */
    private PythonInputData convertToPythonFormat(RouteProcessingRequest request) {
        PythonInputData pythonData = new PythonInputData();
        pythonData.setRouteId(request.getRouteId());
        pythonData.setUserId(request.getUserId());
        
        // Convert POIs
        List<PythonPOI> pythonPOIs = new ArrayList<>();
        if (request.getPois() != null) {
            for (ProcessingPOI poi : request.getPois()) {
                PythonPOI pythonPOI = new PythonPOI();
                pythonPOI.setPoiId(poi.getId());
                pythonPOI.setName(poi.getName());
                pythonPOI.setLatitude(poi.getLatitude());
                pythonPOI.setLongitude(poi.getLongitude());
                pythonPOI.setCategory(poi.getCategory());
                pythonPOI.setVisitDuration(poi.getVisitDuration() != null ? poi.getVisitDuration() : 60);
                pythonPOI.setCost(poi.getCost() != null ? poi.getCost() : 0.0);
                pythonPOI.setRating(poi.getRating() != null ? poi.getRating() : 4.0);
                pythonPOIs.add(pythonPOI);
            }
        }
        pythonData.setPois(pythonPOIs);
        
        // Set default preferences if not provided
        if (request.getPreferences() != null) {
            pythonData.setOptimizeFor(request.getPreferences().getOptimizeFor());
            pythonData.setMaxTotalTime(request.getPreferences().getMaxTotalTime());
        } else {
            pythonData.setOptimizeFor("distance");
            pythonData.setMaxTotalTime(480); // 8 hours default
        }
        
        return pythonData;
    }

    /**
     * Execute Python MRL-AMIS script
     */
    private Path executePythonScript(Path tempDir, Path inputFile, String requestId) throws Exception {
        Path outputFile = tempDir.resolve("output_result.json");
        
        // Build command to execute Python script
        List<String> command = buildPythonCommand(inputFile, outputFile);
        
        logger.info("Executing Python command: {}", String.join(" ", command));
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(workingDirectory));
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug("Python output: {}", line);
            }
        }
        
        // Wait for completion with timeout
        boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python script execution timed out after " + timeoutMinutes + " minutes");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            logger.error("Python script failed with exit code: {}", exitCode);
            logger.error("Python output: {}", output.toString());
            throw new RuntimeException("Python script execution failed with exit code: " + exitCode);
        }
        
        logger.info("Python script completed successfully");
        return outputFile;
    }

    /**
     * Build Python command with conda environment
     */
    private List<String> buildPythonCommand(Path inputFile, Path outputFile) {
        List<String> command = new ArrayList<>();
        
        // Use conda if environment is specified
        if (condaEnvName != null && !condaEnvName.isEmpty()) {
            command.add("conda");
            command.add("run");
            command.add("-n");
            command.add(condaEnvName);
            command.add("python");
        } else {
            command.add("python");
        }
        
        command.add(pythonScriptPath);
        command.add("--input");
        command.add(inputFile.toString());
        command.add("--output");
        command.add(outputFile.toString());
        
        return command;
    }

    /**
     * Parse results from Python output
     */
    private RouteOptimizationResult parseResults(Path outputFile) throws IOException {
        if (!Files.exists(outputFile)) {
            throw new RuntimeException("Python script did not generate output file: " + outputFile);
        }
        
        // Read and parse the output JSON
        PythonOutputData pythonResult = objectMapper.readValue(outputFile.toFile(), PythonOutputData.class);
        
        // Convert to Java result format
        RouteOptimizationResult result = new RouteOptimizationResult();
        result.setOptimizedRouteId(pythonResult.getOptimizedRouteId());
        result.setTotalDistanceKm(pythonResult.getTotalDistance());
        result.setTotalTimeMinutes(pythonResult.getTotalTime());
        result.setOptimizationScore(pythonResult.getOptimizationScore());
        result.setAlgorithm("MRL-AMIS");
        
        // Convert optimized sequence
        List<OptimizedPOI> optimizedPOIs = new ArrayList<>();
        if (pythonResult.getOptimizedSequence() != null) {
            for (PythonOutputPOI pythonPOI : pythonResult.getOptimizedSequence()) {
                OptimizedPOI optimizedPOI = new OptimizedPOI();
                optimizedPOI.setPoiId(pythonPOI.getPoiId());
                optimizedPOI.setName(pythonPOI.getName());
                optimizedPOI.setLatitude(pythonPOI.getLatitude());
                optimizedPOI.setLongitude(pythonPOI.getLongitude());
                optimizedPOI.setVisitOrder(pythonPOI.getVisitOrder());
                optimizedPOI.setEstimatedVisitTime(pythonPOI.getEstimatedVisitTime());
                optimizedPOI.setArrivalTime(pythonPOI.getArrivalTime());
                optimizedPOI.setDepartureTime(pythonPOI.getDepartureTime());
                optimizedPOIs.add(optimizedPOI);
            }
        }
        result.setOptimizedSequence(optimizedPOIs);
        
        return result;
    }

    /**
     * Cleanup temporary directory
     */
    private void cleanupTempDirectory(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                     .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                         } catch (IOException e) {
                             logger.warn("Failed to delete temp file: {}", path, e);
                         }
                     });
                logger.debug("Cleaned up temp directory: {}", tempDir);
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }

    // Inner classes for Python communication
    
    /**
     * Input data format for Python script
     */
    public static class PythonInputData {
        private String routeId;
        private String userId;
        private List<PythonPOI> pois;
        private String optimizeFor;
        private Integer maxTotalTime;

        // Getters and Setters
        public String getRouteId() { return routeId; }
        public void setRouteId(String routeId) { this.routeId = routeId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<PythonPOI> getPois() { return pois; }
        public void setPois(List<PythonPOI> pois) { this.pois = pois; }
        public String getOptimizeFor() { return optimizeFor; }
        public void setOptimizeFor(String optimizeFor) { this.optimizeFor = optimizeFor; }
        public Integer getMaxTotalTime() { return maxTotalTime; }
        public void setMaxTotalTime(Integer maxTotalTime) { this.maxTotalTime = maxTotalTime; }
    }

    public static class PythonPOI {
        private Long poiId;
        private String name;
        private Double latitude;
        private Double longitude;
        private String category;
        private Integer visitDuration;
        private Double cost;
        private Double rating;

        // Getters and Setters
        public Long getPoiId() { return poiId; }
        public void setPoiId(Long poiId) { this.poiId = poiId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Integer getVisitDuration() { return visitDuration; }
        public void setVisitDuration(Integer visitDuration) { this.visitDuration = visitDuration; }
        public Double getCost() { return cost; }
        public void setCost(Double cost) { this.cost = cost; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
    }

    /**
     * Output data format from Python script
     */
    public static class PythonOutputData {
        private String optimizedRouteId;
        private List<PythonOutputPOI> optimizedSequence;
        private Double totalDistance;
        private Integer totalTime;
        private Double optimizationScore;
        private String generatedAt;

        // Getters and Setters
        public String getOptimizedRouteId() { return optimizedRouteId; }
        public void setOptimizedRouteId(String optimizedRouteId) { this.optimizedRouteId = optimizedRouteId; }
        public List<PythonOutputPOI> getOptimizedSequence() { return optimizedSequence; }
        public void setOptimizedSequence(List<PythonOutputPOI> optimizedSequence) { this.optimizedSequence = optimizedSequence; }
        public Double getTotalDistance() { return totalDistance; }
        public void setTotalDistance(Double totalDistance) { this.totalDistance = totalDistance; }
        public Integer getTotalTime() { return totalTime; }
        public void setTotalTime(Integer totalTime) { this.totalTime = totalTime; }
        public Double getOptimizationScore() { return optimizationScore; }
        public void setOptimizationScore(Double optimizationScore) { this.optimizationScore = optimizationScore; }
        public String getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    }

    public static class PythonOutputPOI {
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