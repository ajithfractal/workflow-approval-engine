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
     * Find the active version of a work item.
     * Only one active version exists per work item at any given time.
     *
     * @param workItemId the work item ID
     * @return optional active work item version
     */
    Optional<WorkItemVersion> findFirstByWorkItemIdAndIsActiveTrueOrderByVersionDesc(UUID workItemId);
}
