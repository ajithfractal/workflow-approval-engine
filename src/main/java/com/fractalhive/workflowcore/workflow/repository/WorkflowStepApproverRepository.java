package com.fractalhive.workflowcore.workflow.repository;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStepApprover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkflowStepApprover entities.
 */
@Repository
public interface WorkflowStepApproverRepository extends JpaRepository<WorkflowStepApprover, UUID> {

    /**
     * Find all approvers for a workflow step.
     *
     * @param stepId the step ID
     * @return list of approvers
     */
    List<WorkflowStepApprover> findByStepId(UUID stepId);
}
