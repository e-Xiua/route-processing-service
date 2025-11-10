package com.exiua.processing.model;

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

