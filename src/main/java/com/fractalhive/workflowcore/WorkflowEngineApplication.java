package com.fractalhive.workflowcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for standalone microservice mode.
 * This class is only used when running the workflow engine as a standalone application.
 * 
 * When used as a dependency, this class is not executed and WorkflowCoreAutoConfiguration
 * handles the auto-configuration instead.
 * 
 * To run in standalone mode:
 * 1. Build with profile: mvn clean package -Pstandalone
 * 2. Run: java -jar target/workflow-core-starter-*.jar
 * 
 * Or run directly: mvn spring-boot:run -Pstandalone
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.fractalhive.workflowcore",
        "com.fractalhive.keycloak"
    },
    exclude = {
        com.fractalhive.workflowcore.config.WorkflowCoreAutoConfiguration.class
    }
)
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
public class WorkflowEngineApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(WorkflowEngineApplication.class);
        // Activate standalone profile if not already set
        if (System.getProperty("spring.profiles.active") == null 
            && System.getenv("SPRING_PROFILES_ACTIVE") == null) {
            app.setAdditionalProfiles("standalone");
        }
        app.run(args);
    }
}
