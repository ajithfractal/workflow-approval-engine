package com.fractalhive.workflowcore.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration class for workflow-core-starter.
 * Enables component scanning for all workflow core packages.
 */
@AutoConfiguration
@ComponentScan(basePackages = {
    "com.fractalhive.workflowcore.workflow",
    "com.fractalhive.workflowcore.approval",
    "com.fractalhive.workflowcore.workitem",
    "com.fractalhive.workflowcore.common",
    "com.fractalhive.workflowcore.config"
})
public class WorkflowCoreAutoConfiguration {
}
