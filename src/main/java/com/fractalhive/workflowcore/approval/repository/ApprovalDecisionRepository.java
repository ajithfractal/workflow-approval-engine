package com.fractalhive.workflowcore.approval.repository;

import com.fractalhive.workflowcore.approval.entity.ApprovalDecision;
import com.fractalhive.workflowcore.approval.enums.DecisionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ApprovalDecision entities.
 */
@Repository
public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, UUID> {

    /**
     * Find decision by approval task ID.
     *
     * @param approvalTaskId the approval task ID
     * @return optional approval decision
     */
    Optional<ApprovalDecision> findByApprovalTaskId(UUID approvalTaskId);

    /**
     * Find all decisions for approval tasks in a step instance.
     *
     * @param stepInstanceId the step instance ID
     * @return list of approval decisions
     */
    @Query("SELECT ad FROM ApprovalDecision ad WHERE ad.approvalTask.stepInstanceId = :stepInstanceId")
    List<ApprovalDecision> findByStepInstanceId(@Param("stepInstanceId") UUID stepInstanceId);

    /**
     * Find decisions by decision type.
     *
     * @param decision the decision type
     * @return list of approval decisions
     */
    List<ApprovalDecision> findByDecision(DecisionType decision);
}
