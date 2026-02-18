package com.fractalhive.workflowcore.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration class for workflow-core-starter.
 * Enables component scanning, JPA entity scanning, and JPA repository scanning for all workflow core packages.
 * 
 * This configuration is used in dependency mode (when included as a library).
 * In standalone mode, WorkflowEngineApplication handles the configuration via @SpringBootApplication.
 */
@AutoConfiguration
@ComponentScan(basePackages = {
    "com.fractalhive.workflowcore.workflow",
    "com.fractalhive.workflowcore.approval",
    "com.fractalhive.workflowcore.workitem",
    "com.fractalhive.workflowcore.taskmanagement",
    "com.fractalhive.workflowcore.rulesengine",
    "com.fractalhive.workflowcore.application",
    "com.fractalhive.workflowcore.user",
    "com.fractalhive.workflowcore.tenant",
    "com.fractalhive.workflowcore.common",
    "com.fractalhive.workflowcore.config"
})
@EntityScan(basePackages = {
    "com.fractalhive.workflowcore.workflow.entity",
    "com.fractalhive.workflowcore.approval.entity",
    "com.fractalhive.workflowcore.workitem.entity",
    "com.fractalhive.workflowcore.rulesengine.entity",
    "com.fractalhive.workflowcore.application.entity",
    "com.fractalhive.workflowcore.common.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.fractalhive.workflowcore.workflow.repository",
    "com.fractalhive.workflowcore.approval.repository",
    "com.fractalhive.workflowcore.workitem.repository",
    "com.fractalhive.workflowcore.rulesengine.repository",
    "com.fractalhive.workflowcore.application.repository"
})
public class WorkflowCoreAutoConfiguration {
}
