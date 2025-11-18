package com.exiua.processing.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.exiua.processing.model.RouteProcessingRequest;

/**
 * Service que expone procesamiento concurrente de rutas usando CompletableFuture
 * y el ThreadPoolTaskExecutor configurado.
 */
@Service
public class ConcurrentRouteProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentRouteProcessingService.class);

    private final GrpcPythonMrlAmisService grpcService;
    private final Executor routeProcessingExecutor;

    public ConcurrentRouteProcessingService(
            GrpcPythonMrlAmisService grpcService,
            @Qualifier("routeProcessingExecutor") Executor routeProcessingExecutor) {
        this.grpcService = grpcService;
        this.routeProcessingExecutor = routeProcessingExecutor;
    }

    /**
     * Ejecuta una optimización de forma asíncrona.
     */
    public CompletableFuture<GrpcPythonMrlAmisService.RouteOptimizationResult> processAsync(RouteProcessingRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Procesando optimización async para routeId={}", request.getRouteId());
                return grpcService.processRoute(request);
            } catch (Exception e) {
                throw new RouteAsyncProcessingException("Fallo procesando ruta async: " + e.getMessage(), e);
            }
        }, routeProcessingExecutor);
    }

    public static class RouteAsyncProcessingException extends RuntimeException {
        public RouteAsyncProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
