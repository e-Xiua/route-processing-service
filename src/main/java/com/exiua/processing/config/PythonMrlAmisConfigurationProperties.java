package com.exiua.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Python MRL-AMIS Configuration Properties
 */
@Configuration
@ConfigurationProperties(prefix = "python.mrl-amis")
public class PythonMrlAmisConfigurationProperties {
    
    private boolean enabled = true;
    private String scriptPath;
    private String workingDirectory;
    private String condaEnvName;
    private int timeoutMinutes = 10;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getScriptPath() {
        return scriptPath;
    }
    
    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }
    
    public String getWorkingDirectory() {
        return workingDirectory;
    }
    
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    public String getCondaEnvName() {
        return condaEnvName;
    }
    
    public void setCondaEnvName(String condaEnvName) {
        this.condaEnvName = condaEnvName;
    }
    
    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }
    
    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }
}