package com.fractalhive.workflowcore.workitem.repository;

import com.fractalhive.workflowcore.workitem.entity.WorkItemVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkItemVersion entities.
 */
@Repository
public interface WorkItemVersionRepository extends JpaRepository<WorkItemVersion, UUID> {

    /**
     * Find all versions for a work item, ordered by version descending.
     *
     * @param workItemId the work item ID
     * @return list of versions
     */
    List<WorkItemVersion> findByWorkItemIdOrderByVersionDesc(UUID workItemId);

    /**
     * Find a specific version of a work item.
     *
     * @param workItemId the work item ID
     * @param version     the version number
     * @return optional work item version
     */
    Optional<WorkItemVersion> findByWorkItemIdAndVersion(UUID workItemId, Integer version);

    /**
     * Find the latest version of a work item.
     *
     * @param workItemId the work item ID
     * @return optional work item version
     */
    Optional<WorkItemVersion> findFirstByWorkItemIdOrderByVersionDesc(UUID workItemId);
}
