package com.fractalhive.workflowcore.workitem.repository;

import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkItem entities.
 */
@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, UUID> {

    /**
     * Find work items by status.
     *
     * @param status the work item status
     * @return list of work items
     */
    List<WorkItem> findByStatus(WorkItemStatus status);

    /**
     * Find work items by type.
     *
     * @param type the work item type
     * @return list of work items
     */
    List<WorkItem> findByType(String type);
}
