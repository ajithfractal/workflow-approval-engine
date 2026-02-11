package com.fractalhive.workflowcore.approval.service;

import com.fractalhive.workflowcore.approval.entity.ApprovalDecision;
import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalType;
import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.approval.enums.RuleEvaluationResult;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalDecisionRepository;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Evaluates approval rules for workflow steps.
 * Determines if a step's approval criteria has been met based on ApprovalType (ALL/ANY/N_OF_M).
 */
@Service
public class ApprovalRuleEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalRuleEvaluator.class);

    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final WorkflowStepInstanceRepository workflowStepInstanceRepository;
    private final WorkflowStepDefinitionRepository workflowStepDefinitionRepository;

    public ApprovalRuleEvaluator(
            ApprovalTaskRepository approvalTaskRepository,
            ApprovalDecisionRepository approvalDecisionRepository,
            WorkflowStepInstanceRepository workflowStepInstanceRepository,
            WorkflowStepDefinitionRepository workflowStepDefinitionRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalDecisionRepository = approvalDecisionRepository;
        this.workflowStepInstanceRepository = workflowStepInstanceRepository;
        this.workflowStepDefinitionRepository = workflowStepDefinitionRepository;
    }

    /**
     * Evaluates approval rules for a workflow step instance.
     *
     * @param stepInstanceId the step instance ID
     * @return evaluation result: COMPLETE, REJECTED, or PENDING
     */
    @Transactional(readOnly = true)
    public RuleEvaluationResult evaluate(UUID stepInstanceId) {
        WorkflowStepInstance stepInstance = workflowStepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Step instance not found: " + stepInstanceId));

        WorkflowStepDefinition stepDefinition = workflowStepDefinitionRepository.findById(stepInstance.getStepId())
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepInstance.getStepId()));

        List<ApprovalTask> tasks = approvalTaskRepository.findByStepInstanceId(stepInstanceId);
        
        if (tasks.isEmpty()) {
            logger.warn("No approval tasks found for step instance: {}", stepInstanceId);
            return RuleEvaluationResult.PENDING;
        }

        // Check for rejections first - if any task is rejected, step is rejected
        boolean hasRejection = tasks.stream()
                .anyMatch(task -> task.getStatus() == TaskStatus.REJECTED);
        
        if (hasRejection) {
            logger.debug("Step instance {} has rejected tasks - returning REJECTED", stepInstanceId);
            return RuleEvaluationResult.REJECTED;
        }

        // Get all decisions for tasks in this step
        List<ApprovalDecision> decisions = tasks.stream()
                .map(task -> approvalDecisionRepository.findByApprovalTaskId(task.getId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());

        // Count approvals
        long approvalCount = decisions.stream()
                .filter(decision -> decision.getDecision() == DecisionType.APPROVED)
                .count();

        int totalTasks = tasks.size();
        ApprovalType approvalType = stepDefinition.getApprovalType();
        Integer minApprovals = stepDefinition.getMinApprovals();

        return evaluateRule(approvalType, approvalCount, totalTasks, minApprovals);
    }

    /**
     * Evaluates the approval rule based on ApprovalType.
     *
     * @param approvalType  the approval type (ALL/ANY/N_OF_M)
     * @param approvalCount number of approved tasks
     * @param totalTasks    total number of tasks
     * @param minApprovals  minimum approvals required (for N_OF_M)
     * @return evaluation result
     */
    private RuleEvaluationResult evaluateRule(
            ApprovalType approvalType,
            long approvalCount,
            int totalTasks,
            Integer minApprovals) {

        switch (approvalType) {
            case ALL:
                // All tasks must be approved
                if (approvalCount == totalTasks) {
                    return RuleEvaluationResult.COMPLETE;
                }
                return RuleEvaluationResult.PENDING;

            case ANY:
                // At least one approval is sufficient
                if (approvalCount >= 1) {
                    return RuleEvaluationResult.COMPLETE;
                }
                return RuleEvaluationResult.PENDING;

            case N_OF_M:
                // At least minApprovals out of totalTasks
                if (minApprovals == null || minApprovals <= 0) {
                    logger.warn("N_OF_M approval type requires minApprovals > 0");
                    return RuleEvaluationResult.PENDING;
                }
                if (approvalCount >= minApprovals) {
                    return RuleEvaluationResult.COMPLETE;
                }
                return RuleEvaluationResult.PENDING;

            default:
                logger.warn("Unknown approval type: {}", approvalType);
                return RuleEvaluationResult.PENDING;
        }
    }
}
