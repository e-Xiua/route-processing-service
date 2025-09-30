package com.exiua.processing.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Route optimization request for Python MRL-AMIS processing
 */
public class RouteProcessingRequest {
    
    @NotBlank(message = "Route ID is required")
    @JsonProperty("route_id")
    private String routeId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @NotEmpty(message = "POIs list cannot be empty")
    @Valid
    @JsonProperty("pois")
    private List<ProcessingPOI> pois;
    
    @JsonProperty("preferences")
    private RoutePreferences preferences;
    
    @JsonProperty("constraints")
    private RouteConstraints constraints;

    // Constructors
    public RouteProcessingRequest() {}

    public RouteProcessingRequest(String routeId, List<ProcessingPOI> pois) {
        this.routeId = routeId;
        this.pois = pois;
    }

    // Getters and Setters
    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<ProcessingPOI> getPois() {
        return pois;
    }

    public void setPois(List<ProcessingPOI> pois) {
        this.pois = pois;
    }

    public RoutePreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(RoutePreferences preferences) {
        this.preferences = preferences;
    }

    public RouteConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(RouteConstraints constraints) {
        this.constraints = constraints;
    }

    /**
     * Route preferences for optimization
     */
    public static class RoutePreferences {
        @JsonProperty("optimize_for")
        private String optimizeFor = "distance"; // distance, time, cost, experience
        
        @JsonProperty("max_total_time")
        private Integer maxTotalTime; // in minutes
        
        @JsonProperty("max_total_cost")
        private Double maxTotalCost;
        
        @JsonProperty("preferred_categories")
        private List<String> preferredCategories;
        
        @JsonProperty("avoid_categories")
        private List<String> avoidCategories;
        
        @JsonProperty("accessibility_required")
        private Boolean accessibilityRequired = false;

        // Getters and Setters
        public String getOptimizeFor() {
            return optimizeFor;
        }

        public void setOptimizeFor(String optimizeFor) {
            this.optimizeFor = optimizeFor;
        }

        public Integer getMaxTotalTime() {
            return maxTotalTime;
        }

        public void setMaxTotalTime(Integer maxTotalTime) {
            this.maxTotalTime = maxTotalTime;
        }

        public Double getMaxTotalCost() {
            return maxTotalCost;
        }

        public void setMaxTotalCost(Double maxTotalCost) {
            this.maxTotalCost = maxTotalCost;
        }

        public List<String> getPreferredCategories() {
            return preferredCategories;
        }

        public void setPreferredCategories(List<String> preferredCategories) {
            this.preferredCategories = preferredCategories;
        }

        public List<String> getAvoidCategories() {
            return avoidCategories;
        }

        public void setAvoidCategories(List<String> avoidCategories) {
            this.avoidCategories = avoidCategories;
        }

        public Boolean getAccessibilityRequired() {
            return accessibilityRequired;
        }

        public void setAccessibilityRequired(Boolean accessibilityRequired) {
            this.accessibilityRequired = accessibilityRequired;
        }
    }

    /**
     * Route constraints for optimization
     */
    public static class RouteConstraints {
        @JsonProperty("start_location")
        private Location startLocation;
        
        @JsonProperty("end_location")
        private Location endLocation;
        
        @JsonProperty("start_time")
        private String startTime; // ISO 8601 format
        
        @JsonProperty("lunch_break_required")
        private Boolean lunchBreakRequired = false;
        
        @JsonProperty("lunch_break_duration")
        private Integer lunchBreakDuration = 60; // minutes

        // Getters and Setters
        public Location getStartLocation() {
            return startLocation;
        }

        public void setStartLocation(Location startLocation) {
            this.startLocation = startLocation;
        }

        public Location getEndLocation() {
            return endLocation;
        }

        public void setEndLocation(Location endLocation) {
            this.endLocation = endLocation;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public Boolean getLunchBreakRequired() {
            return lunchBreakRequired;
        }

        public void setLunchBreakRequired(Boolean lunchBreakRequired) {
            this.lunchBreakRequired = lunchBreakRequired;
        }

        public Integer getLunchBreakDuration() {
            return lunchBreakDuration;
        }

        public void setLunchBreakDuration(Integer lunchBreakDuration) {
            this.lunchBreakDuration = lunchBreakDuration;
        }
    }

    /**
     * Location coordinates
     */
    public static class Location {
        @NotNull(message = "Latitude is required")
        @JsonProperty("latitude")
        private Double latitude;
        
        @NotNull(message = "Longitude is required")
        @JsonProperty("longitude")
        private Double longitude;

        public Location() {}

        public Location(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
    }

    @Override
    public String toString() {
        return "RouteProcessingRequest{" +
                "routeId='" + routeId + '\'' +
                ", userId='" + userId + '\'' +
                ", poisCount=" + (pois != null ? pois.size() : 0) +
                '}';
    }
}