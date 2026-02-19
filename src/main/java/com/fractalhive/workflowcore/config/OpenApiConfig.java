package com.fractalhive.workflowcore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Workflow Engine API")
                        .version("1.0.0")
                        .description("Workflow Engine API Documentation"));
    }

    @Bean
    public GroupedOpenApi workflowApi() {
        return GroupedOpenApi.builder()
                .group("workflow")
                .packagesToScan("com.fractalhive.workflowcore")
                .build();
    }

    @Bean
    public GroupedOpenApi keycloakApi() {
        return GroupedOpenApi.builder()
                .group("keycloak")
                .packagesToScan("com.fractalhive.keycloak.controller")
                .build();
    }
}
