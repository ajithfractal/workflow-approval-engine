package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.approval.enums.RuleEvaluationResult;
import com.fractalhive.workflowcore.approval.service.ApprovalRuleEvaluator;
import com.fractalhive.workflowcore.approval.service.ApprovalTaskStateMachineService;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskResponse;
import com.fractalhive.workflowcore.taskmanagement.service.TaskManagementService;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionResponse;
import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.service.WorkflowInstanceStateMachineService;
import com.fractalhive.workflowcore.workflow.statemachine.service.WorkflowStepInstanceStateMachineService;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import com.fractalhive.workflowcore.workitem.statemachine.service.WorkItemStateMachineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of WorkflowOrchestratorService.
 * Coordinates workflow execution by orchestrating state machines and services.
 */
@Service
public class WorkflowOrchestratorServiceImpl implements WorkflowOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowOrchestratorServiceImpl.class);

    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkItemRepository workItemRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStepInstanceRepository stepInstanceRepository;
    private final WorkflowStepDefinitionRepository stepDefinitionRepository;
    private final WorkflowInstanceStateMachineService workflowInstanceSM;
    private final WorkflowStepInstanceStateMachineService stepInstanceSM;
    private final WorkItemStateMachineService workItemSM;
    private final TaskManagementService taskManagementService;
    private final ApprovalTaskStateMachineService approvalTaskSM;
    private final ApprovalRuleEvaluator ruleEvaluator;

    public WorkflowOrchestratorServiceImpl(
            WorkflowDefinitionService workflowDefinitionService,
            WorkItemRepository workItemRepository,
            WorkflowInstanceRepository workflowInstanceRepository,
            WorkflowStepInstanceRepository stepInstanceRepository,
            WorkflowStepDefinitionRepository stepDefinitionRepository,
            WorkflowInstanceStateMachineService workflowInstanceSM,
            WorkflowStepInstanceStateMachineService stepInstanceSM,
            WorkItemStateMachineService workItemSM,
            TaskManagementService taskManagementService,
            ApprovalTaskStateMachineService approvalTaskSM,
            ApprovalRuleEvaluator ruleEvaluator) {
        this.workflowDefinitionService = workflowDefinitionService;
        this.workItemRepository = workItemRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.stepInstanceRepository = stepInstanceRepository;
        this.stepDefinitionRepository = stepDefinitionRepository;
        this.workflowInstanceSM = workflowInstanceSM;
        this.stepInstanceSM = stepInstanceSM;
        this.workItemSM = workItemSM;
        this.taskManagementService = taskManagementService;
        this.approvalTaskSM = approvalTaskSM;
        this.ruleEvaluator = ruleEvaluator;
    }

    @Override
    @Transactional
    public UUID startWorkflow(UUID workItemId, UUID workflowDefinitionId, String userId) {
        logger.info("Starting workflow for work item: {} with definition: {}", workItemId, workflowDefinitionId);

        // Verify work item exists
        workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + workItemId));

        // Get workflow definition
        WorkflowDefinitionResponse workflowDef = workflowDefinitionService.getWorkflow(workflowDefinitionId);
        if (workflowDef == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + workflowDefinitionId);
        }

        Timestamp now = Timestamp.from(Instant.now());

        // Create workflow instance
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowId(workflowDefinitionId);
        instance.setWorkflowVersion(workflowDef.getVersion());
        instance.setWorkItemId(workItemId);
        instance.setStatus(WorkflowStatus.NOT_STARTED);
        instance.setCreatedAt(now);
        instance.setCreatedBy(userId);
        instance = workflowInstanceRepository.save(instance);

        logger.info("Created workflow instance: {}", instance.getId());

        // Get all step definitions ordered by step order
        List<WorkflowStepDefinition> stepDefs = stepDefinitionRepository
                .findByWorkflowIdOrderByStepOrderAsc(workflowDefinitionId);

        if (stepDefs.isEmpty()) {
            throw new IllegalStateException("Workflow definition has no steps: " + workflowDefinitionId);
        }

        // Create step instances for all steps
        for (WorkflowStepDefinition stepDef : stepDefs) {
            WorkflowStepInstance stepInstance = new WorkflowStepInstance();
            stepInstance.setWorkflowInstanceId(instance.getId());
            stepInstance.setStepId(stepDef.getId());
            stepInstance.setStatus(StepStatus.NOT_STARTED);
            stepInstance.setCreatedAt(now);
            stepInstance.setCreatedBy(userId);
            stepInstanceRepository.save(stepInstance);
            logger.debug("Created step instance: {} for step: {}", stepInstance.getId(), stepDef.getStepName());
        }

        // Start the workflow instance (NOT_STARTED → IN_PROGRESS)
        workflowInstanceSM.start(instance.getId(), userId);

        // Move work item to IN_REVIEW
        workItemSM.startReview(workItemId, userId);

        // Start the first step and create tasks
        List<WorkflowStepInstance> steps = stepInstanceRepository
                .findByWorkflowInstanceIdAndStatus(instance.getId(), StepStatus.NOT_STARTED);

        if (!steps.isEmpty()) {
            WorkflowStepInstance firstStep = steps.get(0);
            stepInstanceSM.start(firstStep.getId(), userId);
            List<UUID> taskIds = taskManagementService.createTasksForStep(firstStep.getId(), userId);
            logger.info("Started first step: {} and created {} tasks", firstStep.getId(), taskIds.size());
        }

        logger.info("Workflow started successfully. Instance ID: {}", instance.getId());
        return instance.getId();
    }

    @Override
    @Transactional
    public void handleApprovalDecision(UUID taskId, String userId, DecisionType decision, String comments) {
        logger.info("Handling approval decision for task: {} by user: {} with decision: {}", taskId, userId, decision);

        // Get task details to find the step instance
        TaskResponse task = taskManagementService.getTask(taskId);
        if (task == null || task.getStepInstanceId() == null) {
            throw new IllegalArgumentException("Task not found or invalid: " + taskId);
        }

        UUID stepInstanceId = task.getStepInstanceId();

        // Record the approval/rejection decision
        if (decision == DecisionType.APPROVED) {
            approvalTaskSM.approve(taskId, userId, comments);
        } else if (decision == DecisionType.REJECTED) {
            approvalTaskSM.reject(taskId, userId, comments);
        } else {
            throw new IllegalArgumentException("Invalid decision type: " + decision);
        }

        // Evaluate step completion rules
        RuleEvaluationResult result = ruleEvaluator.evaluate(stepInstanceId);
        logger.debug("Step evaluation result for step instance {}: {}", stepInstanceId, result);

        if (result == RuleEvaluationResult.COMPLETE) {
            handleStepCompletion(stepInstanceId, userId);
        } else if (result == RuleEvaluationResult.REJECTED) {
            handleStepRejection(stepInstanceId, userId);
        }
        // else PENDING → do nothing, wait for more approvals
    }

    @Override
    @Transactional
    public void cancelWorkflow(UUID workflowInstanceId, String userId) {
        logger.info("Cancelling workflow instance: {} by user: {}", workflowInstanceId, userId);

        WorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + workflowInstanceId));

        // Get all active step instances
        List<WorkflowStepInstance> activeSteps = stepInstanceRepository
                .findByWorkflowInstanceIdAndStatus(workflowInstanceId, StepStatus.IN_PROGRESS);

        // Cancel all pending tasks in active steps
        for (WorkflowStepInstance step : activeSteps) {
            approvalTaskSM.cancelAllForStep(step.getId());
        }

        // Cancel the workflow instance
        workflowInstanceSM.cancel(workflowInstanceId, userId);

        // Cancel the work item
        workItemSM.cancel(instance.getWorkItemId(), userId);

        logger.info("Workflow cancelled successfully. Instance ID: {}", workflowInstanceId);
    }

    // ===== Private helper methods =====

    private void handleStepCompletion(UUID stepInstanceId, String userId) {
        logger.info("Step completed: {}", stepInstanceId);

        // Complete the current step
        stepInstanceSM.complete(stepInstanceId, userId);

        // Get the workflow instance ID
        WorkflowStepInstance stepInstance = stepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> new IllegalStateException("Step instance not found: " + stepInstanceId));
        UUID workflowInstanceId = stepInstance.getWorkflowInstanceId();

        // Check for next step
        List<WorkflowStepInstance> remaining = stepInstanceRepository
                .findByWorkflowInstanceIdAndStatus(workflowInstanceId, StepStatus.NOT_STARTED);

        if (remaining.isEmpty()) {
            // All steps completed → complete workflow
            logger.info("All steps completed. Completing workflow instance: {}", workflowInstanceId);
            workflowInstanceSM.complete(workflowInstanceId, userId);

            // Approve the work item
            WorkflowInstance workflowInstance = workflowInstanceRepository.findById(workflowInstanceId)
                    .orElseThrow(() -> new IllegalStateException("Workflow instance not found: " + workflowInstanceId));
            workItemSM.approve(workflowInstance.getWorkItemId(), userId);
            logger.info("Workflow completed and work item approved. Work item ID: {}", workflowInstance.getWorkItemId());
        } else {
            // Start the next step
            WorkflowStepInstance nextStep = remaining.get(0);
            logger.info("Starting next step: {}", nextStep.getId());
            stepInstanceSM.start(nextStep.getId(), userId);
            List<UUID> taskIds = taskManagementService.createTasksForStep(nextStep.getId(), userId);
            logger.info("Created {} tasks for next step: {}", taskIds.size(), nextStep.getId());
        }
    }

    private void handleStepRejection(UUID stepInstanceId, String userId) {
        logger.info("Step rejected: {}", stepInstanceId);

        // Fail the current step
        stepInstanceSM.fail(stepInstanceId, userId, "Step rejected by approver");

        // Get the workflow instance ID
        WorkflowStepInstance stepInstance = stepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> new IllegalStateException("Step instance not found: " + stepInstanceId));
        UUID workflowInstanceId = stepInstance.getWorkflowInstanceId();

        // Cancel all pending tasks in remaining steps
        List<WorkflowStepInstance> remaining = stepInstanceRepository
                .findByWorkflowInstanceIdAndStatus(workflowInstanceId, StepStatus.NOT_STARTED);
        for (WorkflowStepInstance remainingStep : remaining) {
            approvalTaskSM.cancelAllForStep(remainingStep.getId());
        }

        // Fail the workflow instance
        workflowInstanceSM.fail(workflowInstanceId, userId, "Workflow rejected due to step rejection");

        // Reject the work item
        WorkflowInstance workflowInstance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new IllegalStateException("Workflow instance not found: " + workflowInstanceId));
        workItemSM.reject(workflowInstance.getWorkItemId(), userId);
        logger.info("Workflow failed and work item rejected. Work item ID: {}", workflowInstance.getWorkItemId());
    }
}
