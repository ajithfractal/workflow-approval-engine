package com.fractalhive.workflowcore.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration that detects deployment mode and adjusts behavior accordingly.
 * 
 * Dependency Mode: When included in another Spring Boot app (WorkflowCoreAutoConfiguration used)
 * Standalone Mode: When WorkflowEngineApplication main class is run directly
 * 
 * Mode is automatically detected:
 * - Standalone: WorkflowEngineApplication.main() is executed
 * - Dependency: WorkflowCoreAutoConfiguration is used via Spring Boot auto-configuration
 */
@Configuration
public class ModeConfiguration {

    /**
     * Deployment mode enum.
     */
    public enum DeploymentMode {
        DEPENDENCY,
        STANDALONE
    }
}
