package com.fractalhive.workflowcore.workitem.service;

import com.fractalhive.workflowcore.workitem.dto.WorkItemCreateRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemResponse;
import com.fractalhive.workflowcore.workitem.dto.WorkItemSubmitRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemVersionResponse;
import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.entity.WorkItemVersion;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import com.fractalhive.workflowcore.workitem.repository.WorkItemVersionRepository;
import com.fractalhive.workflowcore.workitem.statemachine.service.WorkItemStateMachineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
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

    public WorkItemServiceImpl(
            WorkItemRepository workItemRepository,
            WorkItemVersionRepository workItemVersionRepository,
            WorkItemStateMachineService workItemStateMachineService) {
        this.workItemRepository = workItemRepository;
        this.workItemVersionRepository = workItemVersionRepository;
        this.workItemStateMachineService = workItemStateMachineService;
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
