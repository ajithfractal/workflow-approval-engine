package com.fractalhive.workflowcore.application.entity;

import com.fractalhive.workflowcore.common.entity.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * Represents a registered application in the workflow engine.
 * Each registered application gets its own database schema for data isolation.
 * This entity is stored in the default/public schema (not tenant-specific).
 */
@Entity
@Table(name = "registered_application", schema = "public")
@Getter
@Setter
public class RegisteredApplication extends BaseEntity {

    @Column(name = "application_name", nullable = false, unique = true, length = 100)
    private String applicationName;

    @Column(name = "application_code", nullable = false, unique = true, length = 50)
    private String applicationCode;

    /**
     * API endpoints as key-value pairs stored in JSONB.
     * Example: {"userApi": "https://app1.example.com/api/users", "notificationApi": "https://app1.example.com/api/notifications"}
     * At minimum should contain "userApi" key.
     */
    @Type(JsonType.class)
    @Column(name = "api_endpoints", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> apiEndpoints;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "schema_name", nullable = false, unique = true, length = 50)
    private String schemaName;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
