package com.fractalhive.workflowcore.taskmanagement.service;

import com.fractalhive.workflowcore.approval.dto.ApprovalTaskCreateRequest;
import com.fractalhive.workflowcore.approval.entity.ApprovalComment;
import com.fractalhive.workflowcore.approval.entity.ApprovalDecision;
import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalCommentRepository;
import com.fractalhive.workflowcore.approval.repository.ApprovalDecisionRepository;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskReassignRequest;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskResponse;
import com.fractalhive.workflowcore.taskmanagement.resolver.ApproverResolver;
import com.fractalhive.workflowcore.workflow.entity.WorkflowDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepApprover;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.repository.WorkflowDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepApproverRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of TaskManagementService.
 * Handles task creation and basic fetching operations.
 */
@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TaskManagementServiceImpl.class);

    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalCommentRepository approvalCommentRepository;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final WorkflowStepInstanceRepository workflowStepInstanceRepository;
    private final WorkflowStepDefinitionRepository workflowStepDefinitionRepository;
    private final WorkflowStepApproverRepository workflowStepApproverRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkItemRepository workItemRepository;
    private final ApproverResolver approverResolver;

    public TaskManagementServiceImpl(
            ApprovalTaskRepository approvalTaskRepository,
            ApprovalCommentRepository approvalCommentRepository,
            ApprovalDecisionRepository approvalDecisionRepository,
            WorkflowStepInstanceRepository workflowStepInstanceRepository,
            WorkflowStepDefinitionRepository workflowStepDefinitionRepository,
            WorkflowStepApproverRepository workflowStepApproverRepository,
            WorkflowInstanceRepository workflowInstanceRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkItemRepository workItemRepository,
            @Autowired(required = false) ApproverResolver approverResolver) {
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalCommentRepository = approvalCommentRepository;
        this.approvalDecisionRepository = approvalDecisionRepository;
        this.workflowStepInstanceRepository = workflowStepInstanceRepository;
        this.workflowStepDefinitionRepository = workflowStepDefinitionRepository;
        this.workflowStepApproverRepository = workflowStepApproverRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workItemRepository = workItemRepository;
        this.approverResolver = approverResolver;
    }

    @Override
    @Transactional
    public UUID createTask(ApprovalTaskCreateRequest request, String createdBy) {
        ApprovalTask task = new ApprovalTask();
        task.setStepInstanceId(request.getStepInstanceId());
        task.setApproverId(request.getApproverId());
        task.setApproverType(request.getApproverType());
        task.setStatus(TaskStatus.PENDING);
        task.setDueAt(request.getDueAt());
        
        Timestamp now = Timestamp.from(Instant.now());
        task.setCreatedAt(now);
        task.setCreatedBy(createdBy);
        
        ApprovalTask saved = approvalTaskRepository.save(task);
        return saved.getId();
    }

    @Override
    @Transactional
    public List<UUID> createTasksForStep(UUID stepInstanceId, String createdBy) {
        WorkflowStepInstance stepInstance = workflowStepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Step instance not found: " + stepInstanceId));

        WorkflowStepDefinition stepDefinition = workflowStepDefinitionRepository.findById(stepInstance.getStepId())
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepInstance.getStepId()));

        List<WorkflowStepApprover> approvers = workflowStepApproverRepository.findByStepId(stepDefinition.getId());
        if (approvers.isEmpty()) {
            logger.warn("No approvers found for step: {}", stepDefinition.getId());
            return Collections.emptyList();
        }

        List<UUID> createdTaskIds = new ArrayList<>();
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp dueAt = calculateDueAt(stepDefinition.getSlaHours());

        for (WorkflowStepApprover approver : approvers) {
            List<String> approverIds = resolveApproverIds(approver);
            
            for (String approverId : approverIds) {
                ApprovalTask task = new ApprovalTask();
                task.setStepInstanceId(stepInstanceId);
                task.setApproverId(approverId);
                task.setApproverType(approver.getApproverType());
                task.setStatus(TaskStatus.PENDING);
                task.setDueAt(dueAt);
                task.setCreatedAt(now);
                task.setCreatedBy(createdBy);
                
                ApprovalTask saved = approvalTaskRepository.save(task);
                createdTaskIds.add(saved.getId());
            }
        }

        logger.info("Created {} tasks for step instance: {}", createdTaskIds.size(), stepInstanceId);
        return createdTaskIds;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId) {
        ApprovalTask task = approvalTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        List<ApprovalComment> comments = approvalCommentRepository.findByApprovalTaskIdOrderByCommentedAtAsc(taskId);
        Optional<ApprovalDecision> decision = approvalDecisionRepository.findByApprovalTaskId(taskId);
        List<ApprovalDecision> decisions = decision.map(List::of).orElse(Collections.emptyList());

        WorkflowStepInstance stepInstance = task.getStepInstance();
        WorkflowStepDefinition stepDefinition = null;
        WorkflowInstance workflowInstance = null;
        WorkflowDefinition workflowDefinition = null;
        WorkItem workItem = null;

        if (stepInstance != null) {
            stepDefinition = workflowStepDefinitionRepository.findById(stepInstance.getStepId()).orElse(null);
            workflowInstance = workflowInstanceRepository.findById(stepInstance.getWorkflowInstanceId()).orElse(null);
            
            if (workflowInstance != null) {
                workflowDefinition = workflowDefinitionRepository.findById(workflowInstance.getWorkflowId()).orElse(null);
                workItem = workItemRepository.findById(workflowInstance.getWorkItemId()).orElse(null);
            }
        }

        TaskResponse.TaskResponseBuilder builder = TaskResponse.builder()
                .taskId(task.getId())
                .stepInstanceId(task.getStepInstanceId())
                .approverId(task.getApproverId())
                .approverType(task.getApproverType())
                .status(task.getStatus())
                .dueAt(task.getDueAt())
                .actedAt(task.getActedAt())
                .createdAt(task.getCreatedAt())
                .createdBy(task.getCreatedBy())
                .comments(comments)
                .decisions(decisions);

        if (stepInstance != null && stepDefinition != null) {
            builder.stepInstance(TaskResponse.StepInstanceInfo.builder()
                    .stepInstanceId(stepInstance.getId())
                    .stepId(stepInstance.getStepId())
                    .stepName(stepDefinition.getStepName())
                    .stepOrder(stepDefinition.getStepOrder())
                    .build());
        }

        if (workflowInstance != null && workflowDefinition != null) {
            builder.workflowInstance(TaskResponse.WorkflowInstanceInfo.builder()
                    .workflowInstanceId(workflowInstance.getId())
                    .workflowId(workflowInstance.getWorkflowId())
                    .workflowName(workflowDefinition.getName())
                    .workflowStatus(workflowInstance.getStatus().name())
                    .build());
        }

        if (workItem != null) {
            builder.workItem(TaskResponse.WorkItemInfo.builder()
                    .workItemId(workItem.getId())
                    .type(workItem.getType())
                    .currentVersion(workItem.getCurrentVersion())
                    .workItemStatus(workItem.getStatus().name())
                    .build());
        }

        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByApprover(String approverId, TaskStatus status) {
        List<ApprovalTask> tasks;
        if (status != null) {
            tasks = approvalTaskRepository.findByApproverIdAndStatus(approverId, status);
        } else {
            tasks = approvalTaskRepository.findByApproverIdAndStatusOrderByDueAtAsc(approverId, TaskStatus.PENDING);
        }
        return tasks.stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse reassignTask(UUID taskId, TaskReassignRequest request, String userId) {
        ApprovalTask task = approvalTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.DELEGATED) {
            throw new IllegalStateException("Cannot reassign task in status: " + task.getStatus());
        }

        task.setApproverId(request.getNewApproverId());
        if (task.getStatus() == TaskStatus.DELEGATED) {
            task.setStatus(TaskStatus.PENDING);
        }

        if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
            ApprovalComment comment = new ApprovalComment();
            comment.setApprovalTaskId(taskId);
            comment.setComment("Task reassigned: " + request.getReason());
            comment.setCommentedBy(userId);
            comment.setCommentedAt(Timestamp.from(Instant.now()));
            comment.setCreatedAt(Timestamp.from(Instant.now()));
            comment.setCreatedBy(userId);
            approvalCommentRepository.save(comment);
        }

        approvalTaskRepository.save(task);
        return getTask(taskId);
    }

    // Helper methods

    private List<String> resolveApproverIds(WorkflowStepApprover approver) {
        switch (approver.getApproverType()) {
            case USER:
                return Collections.singletonList(approver.getApproverValue());
            case ROLE:
                if (approverResolver != null) {
                    return approverResolver.resolveRoleToUserIds(approver.getApproverValue());
                } else {
                    logger.warn("ApproverResolver not available. Cannot resolve ROLE approver: {}", approver.getApproverValue());
                    return Collections.emptyList();
                }
            case MANAGER:
                if (approverResolver != null) {
                    logger.warn("MANAGER approver type requires context. Cannot resolve: {}", approver.getApproverValue());
                    return Collections.emptyList();
                } else {
                    logger.warn("ApproverResolver not available. Cannot resolve MANAGER approver: {}", approver.getApproverValue());
                    return Collections.emptyList();
                }
            default:
                return Collections.emptyList();
        }
    }

    private Timestamp calculateDueAt(Integer slaHours) {
        if (slaHours == null || slaHours <= 0) {
            return null;
        }
        return Timestamp.from(Instant.now().plusSeconds(slaHours * 3600L));
    }

    private TaskResponse toTaskResponse(ApprovalTask task) {
        return getTask(task.getId());
    }
}
