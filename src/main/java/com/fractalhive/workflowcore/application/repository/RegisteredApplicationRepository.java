package com.fractalhive.workflowcore.application.repository;

import com.fractalhive.workflowcore.application.entity.RegisteredApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RegisteredApplication entities.
 * All queries are simple lookups with no relationships, so no N+1 concerns.
 */
@Repository
public interface RegisteredApplicationRepository extends JpaRepository<RegisteredApplication, UUID> {

    /**
     * Finds an application by application code.
     *
     * @param code the application code
     * @return Optional containing the application if found
     */
    Optional<RegisteredApplication> findByApplicationCode(String code);

    /**
     * Finds an application by API key.
     *
     * @param apiKey the API key
     * @return Optional containing the application if found
     */
    Optional<RegisteredApplication> findByApiKey(String apiKey);

    /**
     * Finds an application by schema name.
     *
     * @param schemaName the schema name
     * @return Optional containing the application if found
     */
    Optional<RegisteredApplication> findBySchemaName(String schemaName);

    /**
     * Finds all active applications.
     *
     * @return list of active applications
     */
    List<RegisteredApplication> findAllByActiveTrue();
}
