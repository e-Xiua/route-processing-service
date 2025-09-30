package com.exiua.processing.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exiua.processing.model.RouteProcessingRequest;
import com.exiua.processing.service.GrpcPythonMrlAmisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for route processing with Python MRL-AMIS model
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Route Processing", description = "API for processing routes with MRL-AMIS Python model")
public class RouteProcessingController {
    
    private static final Logger logger = LoggerFactory.getLogger(RouteProcessingController.class);
    
    private final GrpcPythonMrlAmisService grpcPythonMrlAmisService;

    public RouteProcessingController(GrpcPythonMrlAmisService grpcPythonMrlAmisService) {
        this.grpcPythonMrlAmisService = grpcPythonMrlAmisService;
    }

    /**
     * Process route with Python MRL-AMIS model
     */
    @PostMapping("/process-route")
    @Operation(summary = "Process route optimization", 
               description = "Processes route optimization using Python MRL-AMIS model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Processing error")
    })
    public ResponseEntity<GrpcPythonMrlAmisService.RouteOptimizationResult> processRoute(
            @Valid @RequestBody RouteProcessingRequest request) {
        
        logger.info("=== ROUTE PROCESSING REQUEST RECEIVED ===");
        logger.info("Route ID: {}", request.getRouteId());
        logger.info("User ID: {}", request.getUserId());
        logger.info("Number of POIs: {}", request.getPois() != null ? request.getPois().size() : 0);
        
        try {
            GrpcPythonMrlAmisService.RouteOptimizationResult result = 
                grpcPythonMrlAmisService.processRoute(request);
            
            logger.info("=== ROUTE PROCESSING COMPLETED SUCCESSFULLY ===");
            logger.info("Route ID: {}", request.getRouteId());
            logger.info("Algorithm: {}", result.getAlgorithm());
            logger.info("Total Distance: {} km", result.getTotalDistanceKm());
            logger.info("Total Time: {} minutes", result.getTotalTimeMinutes());
            logger.info("Optimization Score: {}", result.getOptimizationScore());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("=== ROUTE PROCESSING FAILED ===");
            logger.error("Route ID: {}", request.getRouteId());
            logger.error("Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service health status")
    public ResponseEntity<HealthResponse> healthCheck() {
        HealthResponse health = new HealthResponse(
            "UP", 
            "Route Processing Service", 
            java.time.LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(health);
    }
    
    /**
     * Health response record
     */
    public record HealthResponse(String status, String service, String timestamp) {}
}