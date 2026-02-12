package com.fractalhive.workflowcore.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration class for workflow-core-starter.
 * Enables component scanning, JPA entity scanning, and JPA repository scanning for all workflow core packages.
 */
@AutoConfiguration
@ComponentScan(basePackages = {
    "com.fractalhive.workflowcore.workflow",
    "com.fractalhive.workflowcore.approval",
    "com.fractalhive.workflowcore.workitem",
    "com.fractalhive.workflowcore.taskmanagement",
    "com.fractalhive.workflowcore.common",
    "com.fractalhive.workflowcore.config"
})
@EntityScan(basePackages = {
    "com.fractalhive.workflowcore.workflow.entity",
    "com.fractalhive.workflowcore.approval.entity",
    "com.fractalhive.workflowcore.workitem.entity",
    "com.fractalhive.workflowcore.common.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.fractalhive.workflowcore.workflow.repository",
    "com.fractalhive.workflowcore.approval.repository",
    "com.fractalhive.workflowcore.workitem.repository"
})
public class WorkflowCoreAutoConfiguration {
}
