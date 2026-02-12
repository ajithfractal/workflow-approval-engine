package com.fractalhive.workflowcore.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Base entity providing common fields for all entities.
 * Uses UUID primary keys and timestamptz for PostgreSQL compatibility.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false, 
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 50)
    private String createdBy;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Timestamp updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
