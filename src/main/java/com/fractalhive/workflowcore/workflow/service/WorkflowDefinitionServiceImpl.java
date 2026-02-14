package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.approval.enums.ApprovalType;
import com.fractalhive.workflowcore.workflow.dto.ApproverRequest;
import com.fractalhive.workflowcore.workflow.dto.StepDefinitionRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionCreateRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionResponse;
import com.fractalhive.workflowcore.workflow.entity.WorkflowDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepApprover;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.repository.WorkflowDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepApproverRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of WorkflowDefinitionService.
 */
@Service
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionServiceImpl.class);

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStepDefinitionRepository workflowStepDefinitionRepository;
    private final WorkflowStepApproverRepository workflowStepApproverRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;

    public WorkflowDefinitionServiceImpl(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowStepDefinitionRepository workflowStepDefinitionRepository,
            WorkflowStepApproverRepository workflowStepApproverRepository,
            WorkflowInstanceRepository workflowInstanceRepository) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStepDefinitionRepository = workflowStepDefinitionRepository;
        this.workflowStepApproverRepository = workflowStepApproverRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
    }

    @Override
    @Transactional
    public UUID createWorkflow(WorkflowDefinitionCreateRequest request, String createdBy) {
        // Check if workflow name + version already exists
        workflowDefinitionRepository.findByNameAndVersion(request.getName(), request.getVersion())
                .ifPresent(w -> {
                    throw new IllegalArgumentException(
                            String.format("Workflow definition already exists: %s v%d", request.getName(), request.getVersion()));
                });

        // Check if a previous version exists (for step copying)
        Optional<WorkflowDefinition> previousVersion = workflowDefinitionRepository
                .findFirstByNameOrderByVersionDesc(request.getName());

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName(request.getName());
        workflow.setVersion(request.getVersion());
        workflow.setIsActive(true);

        Timestamp now = Timestamp.from(Instant.now());
        workflow.setCreatedAt(now);
        workflow.setCreatedBy(createdBy);

        WorkflowDefinition saved = workflowDefinitionRepository.save(workflow);
        logger.info("Created workflow definition: {} v{} (ID: {})", request.getName(), request.getVersion(), saved.getId());

        // If a previous version exists, copy its steps and approvers to the new version
        if (previousVersion.isPresent()) {
            UUID previousWorkflowId = previousVersion.get().getId();
            List<WorkflowStepDefinition> previousSteps = workflowStepDefinitionRepository
                    .findByWorkflowIdOrderByStepOrderAsc(previousWorkflowId);

            if (!previousSteps.isEmpty()) {
                copyStepsAndApprovers(previousWorkflowId, saved.getId(), createdBy);
                logger.info("Copied {} step(s) from previous version {} v{} (ID: {}) to new version {} v{} (ID: {})",
                        previousSteps.size(),
                        previousVersion.get().getName(), previousVersion.get().getVersion(), previousWorkflowId,
                        request.getName(), request.getVersion(), saved.getId());
            }
        }

        return saved.getId();
    }

    @Override
    @Transactional
    public UUID createStep(UUID workflowId, StepDefinitionRequest request, String createdBy) {
        // Verify workflow exists
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));

        // Block modification if workflow instances exist
        checkNoInstancesExist(workflowId, workflow.getName(), workflow.getVersion());

        // Validate minApprovals for N_OF_M approval type
        if (request.getApprovalType() == ApprovalType.N_OF_M) {
            if (request.getMinApprovals() == null || request.getMinApprovals() <= 0) {
                throw new IllegalArgumentException("minApprovals must be > 0 for N_OF_M approval type");
            }
        }

        // Determine step order
        // Note: stepOrder is NOT unique - multiple steps can have the same order for parallel execution
        final Integer stepOrder;
        if (request.getStepOrder() == null) {
            // Auto-assign step order as next available (highest existing order + 1)
            List<WorkflowStepDefinition> existingSteps = workflowStepDefinitionRepository
                    .findByWorkflowIdOrderByStepOrderAsc(workflowId);
            stepOrder = existingSteps.isEmpty() ? 1 : existingSteps.get(existingSteps.size() - 1).getStepOrder() + 1;
        } else {
            // Allow duplicate step orders for parallel execution
            stepOrder = request.getStepOrder();
        }

        // Validate approvers if provided during step creation
        if (request.getApprovers() != null && !request.getApprovers().isEmpty()) {
            int approverCount = request.getApprovers().size();
            // Validate minApprovals constraint for N_OF_M
            if (request.getApprovalType() == ApprovalType.N_OF_M) {
                if (request.getMinApprovals() != null && request.getMinApprovals() > approverCount) {
                    throw new IllegalArgumentException(
                            String.format("minApprovals (%d) cannot exceed total approvers (%d) for N_OF_M approval type",
                                    request.getMinApprovals(), approverCount));
                }
            }
        }

        WorkflowStepDefinition step = new WorkflowStepDefinition();
        step.setWorkflowId(workflowId);
        step.setStepName(request.getStepName());
        step.setStepOrder(stepOrder);
        step.setApprovalType(request.getApprovalType());
        step.setMinApprovals(request.getMinApprovals());
        step.setSlaHours(request.getSlaHours());

        Timestamp now = Timestamp.from(Instant.now());
        step.setCreatedAt(now);
        step.setCreatedBy(createdBy);

        WorkflowStepDefinition saved = workflowStepDefinitionRepository.save(step);
        logger.info("Created step definition: {} (ID: {}) for workflow {}", request.getStepName(), saved.getId(), workflowId);

        // Create approvers if provided
        if (request.getApprovers() != null && !request.getApprovers().isEmpty()) {
            List<UUID> approverIds = createApproversForStep(saved.getId(), request.getApprovers(), createdBy, now);
            logger.info("Created {} approver(s) for step {} during step creation", approverIds.size(), saved.getId());
        }

        return saved.getId();
    }

    /**
     * Helper method to create approvers for a step.
     *
     * @param stepId    the step ID
     * @param requests  list of approver requests
     * @param createdBy the user creating the approvers
     * @param timestamp the timestamp to use for creation
     * @return list of created approver IDs
     */
    private List<UUID> createApproversForStep(UUID stepId, List<ApproverRequest> requests, String createdBy, Timestamp timestamp) {
        List<UUID> createdApproverIds = new ArrayList<>();

        for (ApproverRequest request : requests) {
            WorkflowStepApprover approver = new WorkflowStepApprover();
            approver.setStepId(stepId);
            approver.setApproverType(request.getApproverType());
            approver.setApproverValue(request.getApproverValue());
            approver.setCreatedAt(timestamp);
            approver.setCreatedBy(createdBy);

            WorkflowStepApprover saved = workflowStepApproverRepository.save(approver);
            createdApproverIds.add(saved.getId());
        }

        return createdApproverIds;
    }

    @Override
    @Transactional
    public List<UUID> addApprovers(UUID stepId, List<ApproverRequest> requests, String createdBy) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one approver must be provided");
        }

        WorkflowStepDefinition step = workflowStepDefinitionRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepId));

        // Block modification if workflow instances exist
        checkNoInstancesExistForStep(step.getWorkflowId());

        // Get existing approvers
        List<WorkflowStepApprover> existingApprovers = workflowStepApproverRepository.findByStepId(stepId);
        int totalApproversAfterAdd = existingApprovers.size() + requests.size();

        // Validate minApprovals constraint for N_OF_M
        if (step.getApprovalType() == ApprovalType.N_OF_M) {
            if (step.getMinApprovals() != null && step.getMinApprovals() > totalApproversAfterAdd) {
                throw new IllegalArgumentException(
                        String.format("minApprovals (%d) cannot exceed total approvers (%d) for N_OF_M approval type. " +
                                "Current approvers: %d, Adding: %d",
                                step.getMinApprovals(), totalApproversAfterAdd,
                                existingApprovers.size(), requests.size()));
            }
        }

        // Create all approvers
        Timestamp now = Timestamp.from(Instant.now());
        List<UUID> createdApproverIds = createApproversForStep(stepId, requests, createdBy, now);

        logger.info("Added {} approver(s) to step {} (IDs: {})",
                requests.size(), stepId, createdApproverIds);
        return createdApproverIds;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDefinitionResponse getWorkflow(UUID workflowId) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));
        return toResponse(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDefinitionResponse getWorkflowByNameAndVersion(String name, Integer version) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findByNameAndVersion(name, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Workflow definition not found: %s v%d", name, version)));
        return toResponse(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDefinitionResponse getLatestActiveWorkflow(String name) {
        WorkflowDefinition workflow = workflowDefinitionRepository
                .findFirstByNameAndIsActiveTrueOrderByVersionDesc(name)
                .orElseThrow(() -> new IllegalArgumentException("No active workflow definition found: " + name));
        return toResponse(workflow);
    }

    @Override
    @Transactional
    public void activateVersion(UUID workflowId, String userId) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));

        workflow.setIsActive(true);
        workflowDefinitionRepository.save(workflow);
        logger.info("Activated workflow version: {} v{}", workflow.getName(), workflow.getVersion());
    }

    @Override
    @Transactional
    public void deactivateVersion(UUID workflowId, String userId) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));

        workflow.setIsActive(false);
        workflowDefinitionRepository.save(workflow);
        logger.info("Deactivated workflow version: {} v{}", workflow.getName(), workflow.getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> listWorkflows() {
        List<WorkflowDefinition> workflows = workflowDefinitionRepository.findAll();
        return workflows.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UUID updateWorkflow(UUID workflowId, WorkflowDefinitionCreateRequest request, String updatedBy) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));

        // Check if any workflow instances exist for this definition
        List<WorkflowInstance> instances = workflowInstanceRepository.findByWorkflowId(workflowId);
        
        if (!instances.isEmpty()) {
            // Instances exist - create a new version instead of updating
            logger.info("Workflow definition {} v{} has {} instance(s). Creating new version instead of updating.",
                    workflow.getName(), workflow.getVersion(), instances.size());

            // Determine the next version number
            int nextVersion = workflowDefinitionRepository
                    .findFirstByNameOrderByVersionDesc(workflow.getName())
                    .map(w -> w.getVersion() + 1)
                    .orElse(workflow.getVersion() + 1);

            // Check if a workflow with the new version already exists
            Optional<WorkflowDefinition> existingNewVersion = workflowDefinitionRepository
                    .findByNameAndVersion(workflow.getName(), nextVersion);

            if (existingNewVersion.isPresent()) {
                logger.info("New version {} v{} already exists (ID: {}). Returning existing version.",
                        workflow.getName(), nextVersion, existingNewVersion.get().getId());
                return existingNewVersion.get().getId();
            }

            // Create new workflow definition version using the createWorkflow method
            // createWorkflow will automatically copy steps/approvers from the previous version
            WorkflowDefinitionCreateRequest createRequest = WorkflowDefinitionCreateRequest.builder()
                    .name(workflow.getName())
                    .version(nextVersion)
                    .build();
            UUID newWorkflowId = createWorkflow(createRequest, updatedBy);

            logger.info("Created new workflow definition version: {} v{} (ID: {}) from {} v{} (ID: {})",
                    workflow.getName(), nextVersion, newWorkflowId,
                    workflow.getName(), workflow.getVersion(), workflowId);
            
            return newWorkflowId;
        } else {
            // No instances exist - safe to update the existing workflow
            // Check if name+version combination already exists (excluding current workflow)
            workflowDefinitionRepository.findByNameAndVersion(request.getName(), request.getVersion())
                    .ifPresent(w -> {
                        if (!w.getId().equals(workflowId)) {
                            throw new IllegalArgumentException(
                                    String.format("Workflow definition already exists: %s v%d", request.getName(), request.getVersion()));
                        }
                    });

            workflow.setName(request.getName());
            workflow.setVersion(request.getVersion());
            workflow.setUpdatedAt(Timestamp.from(Instant.now()));
            workflow.setUpdatedBy(updatedBy);

            workflowDefinitionRepository.save(workflow);
            logger.info("Updated workflow definition: {} v{} (ID: {})", request.getName(), request.getVersion(), workflowId);
            
            return workflowId;
        }
    }

    @Override
    @Transactional
    public void deleteWorkflow(UUID workflowId) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));

        // Delete all steps and approvers first
        List<WorkflowStepDefinition> steps = workflowStepDefinitionRepository
                .findByWorkflowIdOrderByStepOrderAsc(workflowId);

        for (WorkflowStepDefinition step : steps) {
            List<WorkflowStepApprover> approvers = workflowStepApproverRepository.findByStepId(step.getId());
            workflowStepApproverRepository.deleteAll(approvers);
            workflowStepDefinitionRepository.delete(step);
        }

        workflowDefinitionRepository.delete(workflow);
        logger.info("Deleted workflow definition: {} v{} (ID: {})", workflow.getName(), workflow.getVersion(), workflowId);
    }

    @Override
    @Transactional
    public void updateStep(UUID stepId, StepDefinitionRequest request, String updatedBy) {
        WorkflowStepDefinition step = workflowStepDefinitionRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepId));

        // Block modification if workflow instances exist
        checkNoInstancesExistForStep(step.getWorkflowId());

        // Validate minApprovals for N_OF_M
        if (request.getApprovalType() == ApprovalType.N_OF_M) {
            if (request.getMinApprovals() == null || request.getMinApprovals() <= 0) {
                throw new IllegalArgumentException("minApprovals must be > 0 for N_OF_M approval type");
            }
        }

        // Note: stepOrder is NOT unique - multiple steps can have the same order for parallel execution

        step.setStepName(request.getStepName());
        if (request.getStepOrder() != null) {
            step.setStepOrder(request.getStepOrder());
        }
        step.setApprovalType(request.getApprovalType());
        step.setMinApprovals(request.getMinApprovals());
        step.setSlaHours(request.getSlaHours());
        step.setUpdatedAt(Timestamp.from(Instant.now()));
        step.setUpdatedBy(updatedBy);

        workflowStepDefinitionRepository.save(step);
        logger.info("Updated step definition: {} (ID: {})", request.getStepName(), stepId);
    }

    @Override
    @Transactional
    public void deleteStep(UUID stepId) {
        WorkflowStepDefinition step = workflowStepDefinitionRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepId));

        // Block modification if workflow instances exist
        checkNoInstancesExistForStep(step.getWorkflowId());

        // Delete all approvers first
        List<WorkflowStepApprover> approvers = workflowStepApproverRepository.findByStepId(stepId);
        workflowStepApproverRepository.deleteAll(approvers);

        workflowStepDefinitionRepository.delete(step);
        logger.info("Deleted step definition: {} (ID: {})", step.getStepName(), stepId);
    }

    @Override
    @Transactional
    public void removeApprover(UUID approverId) {
        WorkflowStepApprover approver = workflowStepApproverRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("Approver not found: " + approverId));

        UUID stepId = approver.getStepId();
        WorkflowStepDefinition step = workflowStepDefinitionRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepId));

        // Block modification if workflow instances exist
        checkNoInstancesExistForStep(step.getWorkflowId());

        // Get remaining approvers after removal
        List<WorkflowStepApprover> remainingApprovers = workflowStepApproverRepository.findByStepId(stepId);
        int remainingCount = remainingApprovers.size() - 1; // -1 for the one being removed

        // Validate minApprovals constraint for N_OF_M
        if (step.getApprovalType() == ApprovalType.N_OF_M) {
            if (step.getMinApprovals() != null && step.getMinApprovals() > remainingCount) {
                throw new IllegalArgumentException(
                        String.format("Cannot remove approver. minApprovals (%d) would exceed remaining approvers (%d) for N_OF_M approval type",
                                step.getMinApprovals(), remainingCount));
            }
        }

        workflowStepApproverRepository.delete(approver);
        logger.info("Removed approver {} (ID: {}) from step {}", approver.getApproverValue(), approverId, stepId);
    }

    /**
     * Checks if workflow instances exist for this definition.
     * If they do, throws an exception advising to create a new version.
     *
     * @param workflowId the workflow definition ID
     * @param name       the workflow name
     * @param version    the workflow version
     */
    private void checkNoInstancesExist(UUID workflowId, String name, Integer version) {
        List<WorkflowInstance> instances = workflowInstanceRepository.findByWorkflowId(workflowId);
        if (!instances.isEmpty()) {
            throw new IllegalStateException(
                    String.format("Workflow definition '%s' v%d is used by %d work item(s). " +
                            "Cannot modify steps/approvers. Please create a new version by calling " +
                            "PUT /api/workflow-definitions/%s to auto-create a new version.",
                            name, version, instances.size(), workflowId));
        }
    }

    /**
     * Checks if workflow instances exist for the workflow that owns the given step.
     * If they do, throws an exception advising to create a new version.
     *
     * @param workflowId the workflow definition ID (from the step)
     */
    private void checkNoInstancesExistForStep(UUID workflowId) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));
        checkNoInstancesExist(workflowId, workflow.getName(), workflow.getVersion());
    }

    /**
     * Copies all steps and approvers from one workflow definition to another.
     *
     * @param sourceWorkflowId the source workflow definition ID
     * @param targetWorkflowId the target workflow definition ID
     * @param createdBy        the user performing the copy
     */
    private void copyStepsAndApprovers(UUID sourceWorkflowId, UUID targetWorkflowId, String createdBy) {
        Timestamp now = Timestamp.from(Instant.now());

        List<WorkflowStepDefinition> originalSteps = workflowStepDefinitionRepository
                .findByWorkflowIdOrderByStepOrderAsc(sourceWorkflowId);

        for (WorkflowStepDefinition originalStep : originalSteps) {
            // Create new step
            WorkflowStepDefinition newStep = new WorkflowStepDefinition();
            newStep.setWorkflowId(targetWorkflowId);
            newStep.setStepName(originalStep.getStepName());
            newStep.setStepOrder(originalStep.getStepOrder());
            newStep.setApprovalType(originalStep.getApprovalType());
            newStep.setMinApprovals(originalStep.getMinApprovals());
            newStep.setSlaHours(originalStep.getSlaHours());
            newStep.setCreatedAt(now);
            newStep.setCreatedBy(createdBy);
            newStep = workflowStepDefinitionRepository.save(newStep);

            // Copy all approvers for this step
            List<WorkflowStepApprover> originalApprovers = workflowStepApproverRepository.findByStepId(originalStep.getId());
            for (WorkflowStepApprover originalApprover : originalApprovers) {
                WorkflowStepApprover newApprover = new WorkflowStepApprover();
                newApprover.setStepId(newStep.getId());
                newApprover.setApproverType(originalApprover.getApproverType());
                newApprover.setApproverValue(originalApprover.getApproverValue());
                newApprover.setCreatedAt(now);
                newApprover.setCreatedBy(createdBy);
                workflowStepApproverRepository.save(newApprover);
            }
        }

        logger.info("Copied {} step(s) and their approvers from workflow {} to workflow {}",
                originalSteps.size(), sourceWorkflowId, targetWorkflowId);
    }

    private WorkflowDefinitionResponse toResponse(WorkflowDefinition workflow) {
        List<WorkflowStepDefinition> steps = workflowStepDefinitionRepository
                .findByWorkflowIdOrderByStepOrderAsc(workflow.getId());

        List<WorkflowDefinitionResponse.StepDefinitionResponse> stepResponses = steps.stream()
                .map(step -> {
                    List<WorkflowStepApprover> approvers = workflowStepApproverRepository.findByStepId(step.getId());
                    List<WorkflowDefinitionResponse.ApproverResponse> approverResponses = approvers.stream()
                            .map(approver -> WorkflowDefinitionResponse.ApproverResponse.builder()
                                    .approverId(approver.getId())
                                    .approverType(approver.getApproverType().name())
                                    .approverValue(approver.getApproverValue())
                                    .build())
                            .collect(Collectors.toList());

                    return WorkflowDefinitionResponse.StepDefinitionResponse.builder()
                            .stepId(step.getId())
                            .stepName(step.getStepName())
                            .stepOrder(step.getStepOrder())
                            .approvalType(step.getApprovalType().name())
                            .minApprovals(step.getMinApprovals())
                            .slaHours(step.getSlaHours())
                            .approvers(approverResponses)
                            .build();
                })
                .collect(Collectors.toList());

        return WorkflowDefinitionResponse.builder()
                .workflowId(workflow.getId())
                .name(workflow.getName())
                .version(workflow.getVersion())
                .isActive(workflow.getIsActive())
                .steps(stepResponses)
                .build();
    }
}
