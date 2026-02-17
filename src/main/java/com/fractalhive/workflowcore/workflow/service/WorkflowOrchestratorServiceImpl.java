package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.approval.enums.RuleEvaluationResult;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import com.fractalhive.workflowcore.approval.service.ApprovalRuleEvaluator;
import com.fractalhive.workflowcore.approval.service.ApprovalTaskStateMachineService;
import com.fractalhive.workflowcore.rulesengine.dto.RuleContext;
import com.fractalhive.workflowcore.rulesengine.dto.RuleExecutionResult;
import com.fractalhive.workflowcore.rulesengine.enums.RuleType;
import com.fractalhive.workflowcore.rulesengine.service.RuleContextBuilder;
import com.fractalhive.workflowcore.rulesengine.service.RuleEngineService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final RuleEngineService ruleEngineService;
    private final RuleContextBuilder ruleContextBuilder;
    private final ApprovalTaskRepository approvalTaskRepository;

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
            ApprovalRuleEvaluator ruleEvaluator,
            RuleEngineService ruleEngineService,
            RuleContextBuilder ruleContextBuilder,
            ApprovalTaskRepository approvalTaskRepository) {
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
        this.ruleEngineService = ruleEngineService;
        this.ruleContextBuilder = ruleContextBuilder;
        this.approvalTaskRepository = approvalTaskRepository;
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

        // Start all steps with stepOrder = 1 (parallel execution)
        List<WorkflowStepInstance> steps = stepInstanceRepository
                .findByWorkflowInstanceIdAndStatus(instance.getId(), StepStatus.NOT_STARTED);

        if (!steps.isEmpty()) {
            // Get step definitions to find steps with order = 1
            List<WorkflowStepInstance> firstOrderSteps = new ArrayList<>();
            for (WorkflowStepInstance step : steps) {
                WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(step.getStepId())
                        .orElse(null);
                if (stepDef != null && stepDef.getStepOrder() == 1) {
                    firstOrderSteps.add(step);
                }
            }

            // Start all first-order steps in parallel
            for (WorkflowStepInstance firstStep : firstOrderSteps) {
                stepInstanceSM.start(firstStep.getId(), userId);
                List<UUID> taskIds = taskManagementService.createTasksForStep(firstStep.getId(), userId);
                logger.info("Started parallel step: {} (order: 1) and created {} tasks",
                        firstStep.getId(), taskIds.size());
                
                // Evaluate auto-approve rules after task creation
                evaluateAndApplyAutoApproveRules(firstStep.getId(), userId);
            }
            logger.info("Started {} parallel steps for order 1", firstOrderSteps.size());
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
            
            // Check for auto-approve rules
            try {
                WorkflowStepInstance stepInstance = stepInstanceRepository.findById(stepInstanceId)
                        .orElseThrow(() -> new IllegalStateException("Step instance not found: " + stepInstanceId));
                
                RuleContext context = ruleContextBuilder.buildForStepInstance(stepInstanceId);
                RuleExecutionResult ruleResult = ruleEngineService.evaluateRules(
                        stepInstance.getStepId(),
                        RuleType.AUTO_APPROVE,
                        context);
                
                if (ruleResult.getRuleMatched() && Boolean.TRUE.equals(ruleResult.getShouldAutoApprove())) {
                    logger.info("Auto-approve rule matched, auto-approving remaining tasks for step: {}", stepInstanceId);
                    // Auto-approve remaining pending tasks
                    List<ApprovalTask> pendingTasks = approvalTaskRepository
                            .findByStepInstanceIdAndStatus(stepInstanceId, TaskStatus.PENDING);
                    
                    for (ApprovalTask pendingTask : pendingTasks) {
                        if (!pendingTask.getId().equals(taskId)) { // Don't auto-approve the task that triggered this
                            approvalTaskSM.approve(pendingTask.getId(), userId, "Auto-approved by rule engine");
                            logger.info("Auto-approved task: {}", pendingTask.getId());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error evaluating auto-approve rules: {}", e.getMessage());
            }
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

        // Get the completed step's definition to check its order
        WorkflowStepDefinition completedStepDef = stepDefinitionRepository.findById(stepInstance.getStepId())
                .orElseThrow(() -> new IllegalStateException("Step definition not found: " + stepInstance.getStepId()));

        int completedStepOrder = completedStepDef.getStepOrder();

        // Check if there are other steps with the same order still in progress
        List<WorkflowStepInstance> allSteps = stepInstanceRepository
                .findByWorkflowInstanceId(workflowInstanceId);

        // Check for parallel steps (same order) still in progress
        boolean hasParallelStepsInProgress = allSteps.stream()
                .anyMatch(ps -> {
                    if (ps.getId().equals(stepInstanceId)) {
                        return false; // Skip the just-completed step
                    }
                    WorkflowStepDefinition psDef = stepDefinitionRepository.findById(ps.getStepId())
                            .orElse(null);
                    return psDef != null
                            && psDef.getStepOrder().equals(completedStepOrder)
                            && ps.getStatus() == StepStatus.IN_PROGRESS;
                });

        // If parallel steps are still in progress, don't advance yet
        if (hasParallelStepsInProgress) {
            logger.info("Parallel steps with order {} still in progress, waiting for completion", completedStepOrder);
            return;
        }

        // All steps of this order are complete, now check for next steps
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
            // Find the next step order
            int nextOrder = getNextStepOrder(workflowInstanceId, completedStepOrder);

            if (nextOrder == -1) {
                logger.warn("No next step order found after order {}", completedStepOrder);
                return;
            }

            // Start ALL steps with the next order (parallel execution)
            List<WorkflowStepInstance> nextSteps = remaining.stream()
                    .filter(step -> {
                        WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(step.getStepId())
                                .orElse(null);
                        return stepDef != null && stepDef.getStepOrder().equals(nextOrder);
                    })
                    .collect(Collectors.toList());

            // Start all next steps in parallel
            for (WorkflowStepInstance nextStep : nextSteps) {
                stepInstanceSM.start(nextStep.getId(), userId);
                List<UUID> taskIds = taskManagementService.createTasksForStep(nextStep.getId(), userId);
                logger.info("Started next step: {} (order: {}) and created {} tasks",
                        nextStep.getId(), nextOrder, taskIds.size());
                
                // Evaluate auto-approve rules after task creation
                evaluateAndApplyAutoApproveRules(nextStep.getId(), userId);
            }
            logger.info("Started {} parallel steps for order {}", nextSteps.size(), nextOrder);
        }
    }

    /**
     * Helper method to get the next step order after a given order.
     *
     * @param workflowInstanceId the workflow instance ID
     * @param currentOrder       the current step order
     * @return the next step order, or -1 if no next step exists
     */
    private int getNextStepOrder(UUID workflowInstanceId, int currentOrder) {
        WorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new IllegalStateException("Workflow instance not found: " + workflowInstanceId));

        // Get all step definitions ordered by step order
        List<WorkflowStepDefinition> allStepDefs = stepDefinitionRepository
                .findByWorkflowIdOrderByStepOrderAsc(instance.getWorkflowId());

        // Find the next order after currentOrder
        return allStepDefs.stream()
                .map(WorkflowStepDefinition::getStepOrder)
                .filter(order -> order > currentOrder)
                .findFirst()
                .orElse(-1); // No next step
    }

    private void handleStepRejection(UUID stepInstanceId, String userId) {
        logger.info("Step rejected: {}", stepInstanceId);

        // Get the step instance
        WorkflowStepInstance stepInstance = stepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> new IllegalStateException("Step instance not found: " + stepInstanceId));
        UUID workflowInstanceId = stepInstance.getWorkflowInstanceId();

        // Fail the current step
        stepInstanceSM.fail(stepInstanceId, userId, "Step rejected by approver");

        // Cancel all pending tasks in remaining steps (future steps)
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

    /**
     * Evaluates auto-approve rules for a step instance and auto-approves all tasks if rule matches.
     * This is called right after tasks are created for a step.
     *
     * @param stepInstanceId the step instance ID
     * @param userId         the user ID (system user for auto-approval)
     */
    private void evaluateAndApplyAutoApproveRules(UUID stepInstanceId, String userId) {
        try {
            WorkflowStepInstance stepInstance = stepInstanceRepository.findById(stepInstanceId)
                    .orElseThrow(() -> new IllegalStateException("Step instance not found: " + stepInstanceId));

            // Build rule context for evaluation
            RuleContext context = ruleContextBuilder.buildForStepInstance(stepInstanceId);
            
            // Evaluate auto-approve rules
            RuleExecutionResult ruleResult = ruleEngineService.evaluateRules(
                    stepInstance.getStepId(),
                    RuleType.AUTO_APPROVE,
                    context);

            if (ruleResult.getRuleMatched() && Boolean.TRUE.equals(ruleResult.getShouldAutoApprove())) {
                logger.info("Auto-approve rule matched for step: {}, auto-approving all tasks", stepInstanceId);
                
                // Get all pending tasks for this step
                List<ApprovalTask> pendingTasks = approvalTaskRepository
                        .findByStepInstanceIdAndStatus(stepInstanceId, TaskStatus.PENDING);

                if (pendingTasks.isEmpty()) {
                    logger.debug("No pending tasks to auto-approve for step: {}", stepInstanceId);
                    return;
                }

                // Auto-approve all pending tasks
                for (ApprovalTask pendingTask : pendingTasks) {
                    approvalTaskSM.approve(pendingTask.getId(), userId, "Auto-approved by rule engine");
                    logger.info("Auto-approved task: {} for step: {}", pendingTask.getId(), stepInstanceId);
                }

                // After auto-approving all tasks, evaluate step completion
                // This will trigger the step completion logic and move to next steps
                RuleEvaluationResult evaluationResult = ruleEvaluator.evaluate(stepInstanceId);
                if (evaluationResult == RuleEvaluationResult.COMPLETE) {
                    logger.info("Step {} completed after auto-approval, handling step completion", stepInstanceId);
                    handleStepCompletion(stepInstanceId, userId);
                }
            } else {
                logger.debug("No auto-approve rule matched for step: {}", stepInstanceId);
            }
        } catch (Exception e) {
            logger.warn("Error evaluating auto-approve rules for step {}: {}", stepInstanceId, e.getMessage(), e);
            // Don't throw exception - allow workflow to continue even if rule evaluation fails
        }
    }
}
