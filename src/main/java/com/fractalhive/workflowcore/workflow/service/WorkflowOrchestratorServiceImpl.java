package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalType;
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
import com.fractalhive.workflowcore.workflow.entity.WorkflowStageDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStageInstance;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.enums.StageStatus;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStageDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStageInstanceRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.service.WorkflowInstanceStateMachineService;
import com.fractalhive.workflowcore.workflow.statemachine.service.WorkflowStageInstanceStateMachineService;
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
    private final WorkflowStageDefinitionRepository stageDefinitionRepository;
    private final WorkflowStageInstanceRepository stageInstanceRepository;
    private final WorkflowStepInstanceRepository stepInstanceRepository;
    private final WorkflowStepDefinitionRepository stepDefinitionRepository;
    private final WorkflowInstanceStateMachineService workflowInstanceSM;
    private final WorkflowStageInstanceStateMachineService stageInstanceSM;
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
            WorkflowStageDefinitionRepository stageDefinitionRepository,
            WorkflowStageInstanceRepository stageInstanceRepository,
            WorkflowStepInstanceRepository stepInstanceRepository,
            WorkflowStepDefinitionRepository stepDefinitionRepository,
            WorkflowInstanceStateMachineService workflowInstanceSM,
            WorkflowStageInstanceStateMachineService stageInstanceSM,
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
        this.stageDefinitionRepository = stageDefinitionRepository;
        this.stageInstanceRepository = stageInstanceRepository;
        this.stepInstanceRepository = stepInstanceRepository;
        this.stepDefinitionRepository = stepDefinitionRepository;
        this.workflowInstanceSM = workflowInstanceSM;
        this.stageInstanceSM = stageInstanceSM;
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

        // Get all stage definitions ordered by stage order (with steps loaded via JOIN FETCH)
        List<WorkflowStageDefinition> stageDefs = stageDefinitionRepository
                .findByWorkflowIdOrderByStageOrderAsc(workflowDefinitionId);

        if (stageDefs.isEmpty()) {
            throw new IllegalStateException("Workflow definition has no stages: " + workflowDefinitionId);
        }

        // Batch create stage instances and step instances
        List<WorkflowStageInstance> stageInstances = new ArrayList<>();
        List<WorkflowStepInstance> stepInstances = new ArrayList<>();

        for (WorkflowStageDefinition stageDef : stageDefs) {
            // Create stage instance
            WorkflowStageInstance stageInstance = new WorkflowStageInstance();
            stageInstance.setWorkflowInstanceId(instance.getId());
            stageInstance.setStageId(stageDef.getId());
            stageInstance.setStatus(StageStatus.NOT_STARTED);
            stageInstance.setCreatedAt(now);
            stageInstance.setCreatedBy(userId);
            stageInstances.add(stageInstance);

            // Create step instances for all steps in this stage
            List<WorkflowStepDefinition> stepDefs = stepDefinitionRepository
                    .findByStageIdOrderByStepOrderAsc(stageDef.getId());
            for (WorkflowStepDefinition stepDef : stepDefs) {
                WorkflowStepInstance stepInstance = new WorkflowStepInstance();
                stepInstance.setWorkflowInstanceId(instance.getId());
                stepInstance.setStepId(stepDef.getId());
                stepInstance.setStatus(StepStatus.NOT_STARTED);
                stepInstance.setCreatedAt(now);
                stepInstance.setCreatedBy(userId);
                stepInstances.add(stepInstance);
            }
        }

        // Batch save all instances
        stageInstanceRepository.saveAll(stageInstances);
        stepInstanceRepository.saveAll(stepInstances);
        logger.info("Created {} stage instance(s) and {} step instance(s)", stageInstances.size(), stepInstances.size());

        // Start the workflow instance (NOT_STARTED → IN_PROGRESS)
        workflowInstanceSM.start(instance.getId(), userId);

        // Move work item to IN_REVIEW
        workItemSM.startReview(workItemId, userId);

        // Start first stage (stageOrder = 1)
        WorkflowStageInstance firstStageInstance = stageInstances.stream()
                .filter(si -> {
                    WorkflowStageDefinition stageDef = stageDefinitionRepository.findById(si.getStageId())
                            .orElse(null);
                    return stageDef != null && stageDef.getStageOrder() == 1;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No stage with order 1 found"));

        startStage(firstStageInstance.getId(), userId);

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

    /**
     * Starts a stage and its steps based on stepCompletionType.
     *
     * @param stageInstanceId the stage instance ID
     * @param userId          the user ID
     */
    private void startStage(UUID stageInstanceId, String userId) {
        logger.info("Starting stage: {}", stageInstanceId);

        WorkflowStageInstance stageInstance = stageInstanceRepository.findById(stageInstanceId)
                .orElseThrow(() -> new IllegalStateException("Stage instance not found: " + stageInstanceId));

        WorkflowStageDefinition stageDef = stageDefinitionRepository.findById(stageInstance.getStageId())
                .orElseThrow(() -> new IllegalStateException("Stage definition not found: " + stageInstance.getStageId()));

        // Start the stage
        stageInstanceSM.start(stageInstanceId, userId);

        // Get all step instances for this stage
        List<WorkflowStepInstance> stepInstances = stepInstanceRepository
                .findByWorkflowInstanceId(stageInstance.getWorkflowInstanceId())
                .stream()
                .filter(si -> {
                    WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(si.getStepId())
                            .orElse(null);
                    return stepDef != null && stepDef.getStageId().equals(stageInstance.getStageId());
                })
                .collect(Collectors.toList());

        if (stepInstances.isEmpty()) {
            logger.warn("No steps found for stage: {}", stageInstanceId);
            return;
        }

        // Start steps based on stepCompletionType
        if (stageDef.getStepCompletionType() == ApprovalType.ALL) {
            // Sequential execution - start first step only
            WorkflowStepInstance firstStep = stepInstances.stream()
                    .filter(si -> {
                        WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(si.getStepId())
                                .orElse(null);
                        return stepDef != null && stepDef.getStepOrder() == 1;
                    })
                    .findFirst()
                    .orElse(null);

            if (firstStep != null) {
                stepInstanceSM.start(firstStep.getId(), userId);
                List<UUID> taskIds = taskManagementService.createTasksForStep(firstStep.getId(), userId);
                logger.info("Started first step: {} (order: 1) and created {} tasks", firstStep.getId(), taskIds.size());
                evaluateAndApplyAutoApproveRules(firstStep.getId(), userId);
            }
        } else {
            // Parallel execution (ANY or N_OF_M) - start all steps
            for (WorkflowStepInstance stepInstance : stepInstances) {
                stepInstanceSM.start(stepInstance.getId(), userId);
                List<UUID> taskIds = taskManagementService.createTasksForStep(stepInstance.getId(), userId);
                logger.info("Started parallel step: {} and created {} tasks", stepInstance.getId(), taskIds.size());
                evaluateAndApplyAutoApproveRules(stepInstance.getId(), userId);
            }
            logger.info("Started {} parallel steps for stage {}", stepInstances.size(), stageInstanceId);
        }
    }

    private void handleStepCompletion(UUID stepInstanceId, String userId) {
        logger.info("Step completed: {}", stepInstanceId);

        // Complete the current step
        stepInstanceSM.complete(stepInstanceId, userId);

        // Get the step instance and find its stage
        WorkflowStepInstance stepInstance = stepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> new IllegalStateException("Step instance not found: " + stepInstanceId));

        WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(stepInstance.getStepId())
                .orElseThrow(() -> new IllegalStateException("Step definition not found: " + stepInstance.getStepId()));

        // Find the stage instance for this step
        WorkflowStageInstance stageInstance = stageInstanceRepository
                .findByWorkflowInstanceId(stepInstance.getWorkflowInstanceId())
                .stream()
                .filter(si -> si.getStageId().equals(stepDef.getStageId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stage instance not found for step: " + stepInstanceId));

        WorkflowStageDefinition stageDef = stageDefinitionRepository.findById(stageInstance.getStageId())
                .orElseThrow(() -> new IllegalStateException("Stage definition not found: " + stageInstance.getStageId()));

        // Check if stage completion criteria is met
        if (isStageComplete(stageInstance.getId(), stageDef)) {
            // Stage is complete, move to next stage or complete workflow
            handleStageCompletion(stageInstance.getId(), userId);
        } else {
            // Stage not complete yet
            if (stageDef.getStepCompletionType() == ApprovalType.ALL) {
                // Sequential execution - start next step in same stage
                startNextStepInStage(stageInstance.getId(), stepDef.getStepOrder(), userId);
            }
            // For parallel execution (ANY/N_OF_M), wait for more step completions
        }
    }

    /**
     * Checks if a stage is complete based on its stepCompletionType.
     *
     * @param stageInstanceId the stage instance ID
     * @param stageDef        the stage definition
     * @return true if stage is complete, false otherwise
     */
    private boolean isStageComplete(UUID stageInstanceId, WorkflowStageDefinition stageDef) {
        WorkflowStageInstance stageInstance = stageInstanceRepository.findById(stageInstanceId)
                .orElseThrow(() -> new IllegalStateException("Stage instance not found: " + stageInstanceId));

        // Get all step instances for this stage
        List<WorkflowStepInstance> stepInstances = stepInstanceRepository
                .findByWorkflowInstanceId(stageInstance.getWorkflowInstanceId())
                .stream()
                .filter(si -> {
                    WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(si.getStepId())
                            .orElse(null);
                    return stepDef != null && stepDef.getStageId().equals(stageInstance.getStageId());
                })
                .collect(Collectors.toList());

        if (stepInstances.isEmpty()) {
            return false;
        }

        long completedCount = stepInstances.stream()
                .filter(si -> si.getStatus() == StepStatus.COMPLETED)
                .count();

        switch (stageDef.getStepCompletionType()) {
            case ALL:
                // All steps must be completed
                return completedCount == stepInstances.size();
            case ANY:
                // Any step completion completes the stage
                return completedCount > 0;
            case N_OF_M:
                // Minimum number of steps must be completed
                int minRequired = stageDef.getMinStepCompletions() != null
                        ? stageDef.getMinStepCompletions()
                        : stepInstances.size();
                return completedCount >= minRequired;
            default:
                return false;
        }
    }

    /**
     * Starts the next step in a stage (for sequential execution).
     *
     * @param stageInstanceId the stage instance ID
     * @param currentStepOrder the current step order
     * @param userId          the user ID
     */
    private void startNextStepInStage(UUID stageInstanceId, int currentStepOrder, String userId) {
        WorkflowStageInstance stageInstance = stageInstanceRepository.findById(stageInstanceId)
                .orElseThrow(() -> new IllegalStateException("Stage instance not found: " + stageInstanceId));

        // Get all step instances for this stage
        List<WorkflowStepInstance> stepInstances = stepInstanceRepository
                .findByWorkflowInstanceId(stageInstance.getWorkflowInstanceId())
                .stream()
                .filter(si -> {
                    WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(si.getStepId())
                            .orElse(null);
                    return stepDef != null
                            && stepDef.getStageId().equals(stageInstance.getStageId())
                            && si.getStatus() == StepStatus.NOT_STARTED;
                })
                .collect(Collectors.toList());

        // Find next step order
        int nextOrder = stepInstances.stream()
                .map(si -> {
                    WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(si.getStepId())
                            .orElse(null);
                    return stepDef != null ? stepDef.getStepOrder() : Integer.MAX_VALUE;
                })
                .filter(order -> order > currentStepOrder)
                .min(Integer::compare)
                .orElse(-1);

        if (nextOrder == -1) {
            logger.debug("No next step found in stage {}", stageInstanceId);
            return;
        }

        // Start the next step
        WorkflowStepInstance nextStep = stepInstances.stream()
                .filter(si -> {
                    WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(si.getStepId())
                            .orElse(null);
                    return stepDef != null && stepDef.getStepOrder() == nextOrder;
                })
                .findFirst()
                .orElse(null);

        if (nextStep != null) {
            stepInstanceSM.start(nextStep.getId(), userId);
            List<UUID> taskIds = taskManagementService.createTasksForStep(nextStep.getId(), userId);
            logger.info("Started next step: {} (order: {}) in stage {} and created {} tasks",
                    nextStep.getId(), nextOrder, stageInstanceId, taskIds.size());
            evaluateAndApplyAutoApproveRules(nextStep.getId(), userId);
        }
    }

    /**
     * Handles stage completion - starts next stage or completes workflow.
     *
     * @param stageInstanceId the completed stage instance ID
     * @param userId          the user ID
     */
    private void handleStageCompletion(UUID stageInstanceId, String userId) {
        logger.info("Stage completed: {}", stageInstanceId);

        // Complete the stage
        stageInstanceSM.complete(stageInstanceId, userId);

        WorkflowStageInstance completedStage = stageInstanceRepository.findById(stageInstanceId)
                .orElseThrow(() -> new IllegalStateException("Stage instance not found: " + stageInstanceId));

        WorkflowStageDefinition completedStageDef = stageDefinitionRepository.findById(completedStage.getStageId())
                .orElseThrow(() -> new IllegalStateException("Stage definition not found: " + completedStage.getStageId()));

        UUID workflowInstanceId = completedStage.getWorkflowInstanceId();

        // Find next stage
        List<WorkflowStageInstance> allStages = stageInstanceRepository
                .findByWorkflowInstanceIdOrderByStageOrderAsc(workflowInstanceId);

        int nextStageOrder = allStages.stream()
                .filter(si -> {
                    WorkflowStageDefinition stageDef = stageDefinitionRepository.findById(si.getStageId())
                            .orElse(null);
                    return stageDef != null
                            && stageDef.getStageOrder() > completedStageDef.getStageOrder()
                            && si.getStatus() == StageStatus.NOT_STARTED;
                })
                .map(si -> {
                    WorkflowStageDefinition stageDef = stageDefinitionRepository.findById(si.getStageId())
                            .orElse(null);
                    return stageDef != null ? stageDef.getStageOrder() : Integer.MAX_VALUE;
                })
                .min(Integer::compare)
                .orElse(-1);

        if (nextStageOrder == -1) {
            // No more stages → complete workflow
            logger.info("All stages completed. Completing workflow instance: {}", workflowInstanceId);
            workflowInstanceSM.complete(workflowInstanceId, userId);

            WorkflowInstance workflowInstance = workflowInstanceRepository.findById(workflowInstanceId)
                    .orElseThrow(() -> new IllegalStateException("Workflow instance not found: " + workflowInstanceId));
            workItemSM.approve(workflowInstance.getWorkItemId(), userId);
            logger.info("Workflow completed and work item approved. Work item ID: {}", workflowInstance.getWorkItemId());
        } else {
            // Start next stage
            WorkflowStageInstance nextStage = allStages.stream()
                    .filter(si -> {
                        WorkflowStageDefinition stageDef = stageDefinitionRepository.findById(si.getStageId())
                                .orElse(null);
                        return stageDef != null && stageDef.getStageOrder() == nextStageOrder;
                    })
                    .findFirst()
                    .orElse(null);

            if (nextStage != null) {
                startStage(nextStage.getId(), userId);
            }
        }
    }

    private void handleStepRejection(UUID stepInstanceId, String userId) {
        logger.info("Step rejected: {}", stepInstanceId);

        // Get the step instance
        WorkflowStepInstance stepInstance = stepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> new IllegalStateException("Step instance not found: " + stepInstanceId));
        UUID workflowInstanceId = stepInstance.getWorkflowInstanceId();

        // Fail the current step
        stepInstanceSM.fail(stepInstanceId, userId, "Step rejected by approver");

        // Fail the stage containing this step
        WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(stepInstance.getStepId())
                .orElseThrow(() -> new IllegalStateException("Step definition not found: " + stepInstance.getStepId()));

        WorkflowStageInstance stageInstance = stageInstanceRepository
                .findByWorkflowInstanceId(workflowInstanceId)
                .stream()
                .filter(si -> si.getStageId().equals(stepDef.getStageId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stage instance not found for step: " + stepInstanceId));

        stageInstanceSM.fail(stageInstance.getId(), userId, "Stage failed due to step rejection");

        // Cancel all pending tasks in remaining steps (future steps)
        List<WorkflowStepInstance> remaining = stepInstanceRepository
                .findByWorkflowInstanceIdAndStatus(workflowInstanceId, StepStatus.NOT_STARTED);
        for (WorkflowStepInstance remainingStep : remaining) {
            approvalTaskSM.cancelAllForStep(remainingStep.getId());
        }

        // Cancel all remaining stages
        List<WorkflowStageInstance> remainingStages = stageInstanceRepository
                .findByWorkflowInstanceIdAndStatus(workflowInstanceId, StageStatus.NOT_STARTED);
        for (WorkflowStageInstance remainingStage : remainingStages) {
            stageInstanceSM.fail(remainingStage.getId(), userId, "Stage cancelled due to workflow failure");
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
