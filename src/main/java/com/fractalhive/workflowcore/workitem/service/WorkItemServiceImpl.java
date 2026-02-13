package com.fractalhive.workflowcore.workitem.service;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import com.fractalhive.workflowcore.workitem.dto.WorkItemCreateRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemResponse;
import com.fractalhive.workflowcore.workitem.dto.WorkItemSubmitRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemVersionResponse;
import com.fractalhive.workflowcore.workitem.dto.WorkflowProgressResponse;
import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.entity.WorkItemVersion;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import org.springframework.data.domain.Sort;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import com.fractalhive.workflowcore.workitem.repository.WorkItemVersionRepository;
import com.fractalhive.workflowcore.workitem.statemachine.service.WorkItemStateMachineService;
import com.fractalhive.workflowcore.workflow.entity.WorkflowDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of WorkItemService.
 * Uses WorkItemStateMachineService for all status transitions.
 */
@Service
public class WorkItemServiceImpl implements WorkItemService {

    private static final Logger logger = LoggerFactory.getLogger(WorkItemServiceImpl.class);

    private final WorkItemRepository workItemRepository;
    private final WorkItemVersionRepository workItemVersionRepository;
    private final WorkItemStateMachineService workItemStateMachineService;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStepInstanceRepository stepInstanceRepository;
    private final WorkflowStepDefinitionRepository stepDefinitionRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final ApprovalTaskRepository approvalTaskRepository;

    public WorkItemServiceImpl(
            WorkItemRepository workItemRepository,
            WorkItemVersionRepository workItemVersionRepository,
            WorkItemStateMachineService workItemStateMachineService,
            WorkflowInstanceRepository workflowInstanceRepository,
            WorkflowStepInstanceRepository stepInstanceRepository,
            WorkflowStepDefinitionRepository stepDefinitionRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            ApprovalTaskRepository approvalTaskRepository) {
        this.workItemRepository = workItemRepository;
        this.workItemVersionRepository = workItemVersionRepository;
        this.workItemStateMachineService = workItemStateMachineService;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.stepInstanceRepository = stepInstanceRepository;
        this.stepDefinitionRepository = stepDefinitionRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.approvalTaskRepository = approvalTaskRepository;
    }

    @Override
    @Transactional
    public UUID createWorkItem(WorkItemCreateRequest request, String createdBy) {
        WorkItem workItem = new WorkItem();
        workItem.setType(request.getType());
        workItem.setStatus(WorkItemStatus.DRAFT);
        workItem.setCurrentVersion(1);

        Timestamp now = Timestamp.from(Instant.now());
        workItem.setCreatedAt(now);
        workItem.setCreatedBy(createdBy);

        WorkItem saved = workItemRepository.save(workItem);
        logger.info("Created work item: {} (ID: {})", request.getType(), saved.getId());
        return saved.getId();
    }

    @Override
    @Transactional
    public UUID submitWorkItem(UUID workItemId, WorkItemSubmitRequest request, String submittedBy) {
        // Verify work item exists
        workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + workItemId));

        // Use state machine to submit (creates version and transitions to SUBMITTED)
        workItemStateMachineService.submit(workItemId, request.getContentRef(), submittedBy);

        // Find the version that was just created
        Optional<WorkItemVersion> latestVersion = workItemVersionRepository
                .findFirstByWorkItemIdOrderByVersionDesc(workItemId);

        if (latestVersion.isPresent()) {
            logger.info("Submitted work item: {} (Version: {})", workItemId, latestVersion.get().getVersion());
            return latestVersion.get().getId();
        } else {
            throw new IllegalStateException("Version was not created during submission");
        }
    }

    @Override
    @Transactional
    public UUID createVersion(UUID workItemId, String contentRef, String createdBy) {
        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + workItemId));

        // Increment version
        int newVersion = workItem.getCurrentVersion() + 1;
        workItem.setCurrentVersion(newVersion);
        workItemRepository.save(workItem);

        // Create version entity
        WorkItemVersion version = new WorkItemVersion();
        version.setWorkItemId(workItemId);
        version.setVersion(newVersion);
        version.setContentRef(contentRef);

        Timestamp now = Timestamp.from(Instant.now());
        version.setCreatedAt(now);
        version.setCreatedBy(createdBy);

        WorkItemVersion saved = workItemVersionRepository.save(version);
        logger.info("Created version {} for work item: {}", newVersion, workItemId);
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkItemResponse getWorkItem(UUID workItemId) {
        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + workItemId));

        Optional<WorkItemVersion> latestVersion = workItemVersionRepository
                .findFirstByWorkItemIdOrderByVersionDesc(workItemId);

        List<WorkItemVersion> versions = workItemVersionRepository
                .findByWorkItemIdOrderByVersionDesc(workItemId);

        WorkItemVersionResponse latestVersionResponse = latestVersion
                .map(this::toVersionResponse)
                .orElse(null);

        List<WorkItemVersionResponse> versionResponses = versions.stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());

        return WorkItemResponse.builder()
                .workItemId(workItem.getId())
                .type(workItem.getType())
                .status(workItem.getStatus())
                .currentVersion(workItem.getCurrentVersion())
                .contentRef(latestVersionResponse != null ? latestVersionResponse.getContentRef() : null)
                .createdAt(workItem.getCreatedAt())
                .createdBy(workItem.getCreatedBy())
                .latestVersion(latestVersionResponse)
                .versions(versionResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkItemVersionResponse> getVersions(UUID workItemId) {
        List<WorkItemVersion> versions = workItemVersionRepository
                .findByWorkItemIdOrderByVersionDesc(workItemId);

        return versions.stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowProgressResponse getWorkflowProgress(UUID workItemId) {
        // Find active workflow instance
        Optional<WorkflowInstance> instanceOpt = workflowInstanceRepository
                .findFirstByWorkItemIdAndStatusInOrderByCreatedAtDesc(
                        workItemId,
                        List.of(WorkflowStatus.NOT_STARTED, WorkflowStatus.IN_PROGRESS, WorkflowStatus.COMPLETED, WorkflowStatus.FAILED));

        if (instanceOpt.isEmpty()) {
            return WorkflowProgressResponse.builder()
                    .workItemId(workItemId)
                    .steps(List.of())
                    .progress(WorkflowProgressResponse.ProgressSummary.builder()
                            .completedSteps(0)
                            .totalSteps(0)
                            .percentage(0)
                            .build())
                    .build();
        }

        WorkflowInstance instance = instanceOpt.get();

        // Get workflow definition for name
        WorkflowDefinition workflowDef = workflowDefinitionRepository.findById(instance.getWorkflowId())
                .orElse(null);

        // Get all step instances ordered by step order
        List<WorkflowStepInstance> stepInstances = stepInstanceRepository
                .findByWorkflowInstanceId(instance.getId());

        // Build step progress info
        List<WorkflowProgressResponse.StepProgressInfo> stepInfos = new ArrayList<>();
        WorkflowProgressResponse.StepProgressInfo currentStepInfo = null;

        for (WorkflowStepInstance stepInstance : stepInstances) {
            WorkflowStepDefinition stepDef = stepDefinitionRepository.findById(stepInstance.getStepId())
                    .orElse(null);

            // Get tasks for this step
            List<ApprovalTask> tasks = approvalTaskRepository.findByStepInstanceId(stepInstance.getId());
            List<WorkflowProgressResponse.TaskProgressInfo> taskInfos = tasks.stream()
                    .map(task -> WorkflowProgressResponse.TaskProgressInfo.builder()
                            .taskId(task.getId())
                            .approverId(task.getApproverId())
                            .status(task.getStatus())
                            .dueAt(task.getDueAt())
                            .actedAt(task.getActedAt())
                            .build())
                    .collect(Collectors.toList());

            WorkflowProgressResponse.StepProgressInfo stepInfo = WorkflowProgressResponse.StepProgressInfo.builder()
                    .stepInstanceId(stepInstance.getId())
                    .stepId(stepInstance.getStepId())
                    .stepName(stepDef != null ? stepDef.getStepName() : "Unknown")
                    .stepOrder(stepDef != null ? stepDef.getStepOrder() : 0)
                    .status(stepInstance.getStatus())
                    .startedAt(stepInstance.getStartedAt())
                    .completedAt(stepInstance.getCompletedAt())
                    .tasks(taskInfos)
                    .build();

            stepInfos.add(stepInfo);

            // Track current step (IN_PROGRESS)
            if (stepInstance.getStatus() == StepStatus.IN_PROGRESS) {
                currentStepInfo = stepInfo;
            }
        }

        // Sort by step order
        stepInfos.sort(Comparator.comparing(WorkflowProgressResponse.StepProgressInfo::getStepOrder));

        // Calculate progress
        long completedCount = stepInfos.stream()
                .filter(s -> s.getStatus() == StepStatus.COMPLETED)
                .count();
        int totalSteps = stepInfos.size();
        int percentage = totalSteps > 0 ? (int) ((completedCount * 100) / totalSteps) : 0;

        // Build workflow instance info
        WorkflowProgressResponse.WorkflowInstanceInfo workflowInfo = WorkflowProgressResponse.WorkflowInstanceInfo.builder()
                .workflowInstanceId(instance.getId())
                .workflowId(instance.getWorkflowId())
                .workflowName(workflowDef != null ? workflowDef.getName() : "Unknown")
                .workflowVersion(instance.getWorkflowVersion())
                .status(instance.getStatus())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .build();

        return WorkflowProgressResponse.builder()
                .workItemId(workItemId)
                .workflowInstance(workflowInfo)
                .steps(stepInfos)
                .currentStep(currentStepInfo)
                .progress(WorkflowProgressResponse.ProgressSummary.builder()
                        .completedSteps((int) completedCount)
                        .totalSteps(totalSteps)
                        .percentage(percentage)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkItemResponse> getWorkItemsByWorkflowDefinitionId(UUID workflowDefinitionId) {
        // Find all workflow instances with the given workflow definition ID
        List<WorkflowInstance> workflowInstances = workflowInstanceRepository.findByWorkflowId(workflowDefinitionId);

        // Extract unique work item IDs
        List<UUID> workItemIds = workflowInstances.stream()
                .map(WorkflowInstance::getWorkItemId)
                .distinct()
                .collect(Collectors.toList());

        if (workItemIds.isEmpty()) {
            logger.debug("No work items found for workflow definition: {}", workflowDefinitionId);
            return List.of();
        }

        // Fetch work items
        List<WorkItem> workItems = workItemRepository.findAllById(workItemIds);

        // Convert to response DTOs
        return workItems.stream()
                .map(workItem -> {
                    Optional<WorkItemVersion> latestVersion = workItemVersionRepository
                            .findFirstByWorkItemIdOrderByVersionDesc(workItem.getId());

                    List<WorkItemVersion> versions = workItemVersionRepository
                            .findByWorkItemIdOrderByVersionDesc(workItem.getId());

                    WorkItemVersionResponse latestVersionResponse = latestVersion
                            .map(this::toVersionResponse)
                            .orElse(null);

                    List<WorkItemVersionResponse> versionResponses = versions.stream()
                            .map(this::toVersionResponse)
                            .collect(Collectors.toList());

                    return WorkItemResponse.builder()
                            .workItemId(workItem.getId())
                            .type(workItem.getType())
                            .status(workItem.getStatus())
                            .currentVersion(workItem.getCurrentVersion())
                            .contentRef(latestVersionResponse != null ? latestVersionResponse.getContentRef() : null)
                            .createdAt(workItem.getCreatedAt())
                            .createdBy(workItem.getCreatedBy())
                            .latestVersion(latestVersionResponse)
                            .versions(versionResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkItemResponse> listWorkItems(WorkItemStatus status, String type) {
        List<WorkItem> workItems;

        if (status != null && type != null) {
            // Filter by both status and type
            workItems = workItemRepository.findByStatusAndType(status, type);
        } else if (status != null) {
            // Filter by status only
            workItems = workItemRepository.findByStatus(status);
        } else if (type != null) {
            // Filter by type only
            workItems = workItemRepository.findByType(type);
        } else {
            // No filters - get all work items, ordered by creation date descending
            workItems = workItemRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        // Convert to response DTOs
        return workItems.stream()
                .map(workItem -> {
                    Optional<WorkItemVersion> latestVersion = workItemVersionRepository
                            .findFirstByWorkItemIdOrderByVersionDesc(workItem.getId());

                    List<WorkItemVersion> versions = workItemVersionRepository
                            .findByWorkItemIdOrderByVersionDesc(workItem.getId());

                    WorkItemVersionResponse latestVersionResponse = latestVersion
                            .map(this::toVersionResponse)
                            .orElse(null);

                    List<WorkItemVersionResponse> versionResponses = versions.stream()
                            .map(this::toVersionResponse)
                            .collect(Collectors.toList());

                    return WorkItemResponse.builder()
                            .workItemId(workItem.getId())
                            .type(workItem.getType())
                            .status(workItem.getStatus())
                            .currentVersion(workItem.getCurrentVersion())
                            .contentRef(latestVersionResponse != null ? latestVersionResponse.getContentRef() : null)
                            .createdAt(workItem.getCreatedAt())
                            .createdBy(workItem.getCreatedBy())
                            .latestVersion(latestVersionResponse)
                            .versions(versionResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private WorkItemVersionResponse toVersionResponse(WorkItemVersion version) {
        return WorkItemVersionResponse.builder()
                .versionId(version.getId())
                .workItemId(version.getWorkItemId())
                .version(version.getVersion())
                .contentRef(version.getContentRef())
                .submittedBy(version.getSubmittedBy())
                .submittedAt(version.getSubmittedAt())
                .createdAt(version.getCreatedAt())
                .createdBy(version.getCreatedBy())
                .build();
    }
}
