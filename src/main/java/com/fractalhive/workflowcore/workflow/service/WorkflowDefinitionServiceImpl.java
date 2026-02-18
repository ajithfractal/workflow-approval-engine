package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.approval.enums.ApprovalType;
import com.fractalhive.workflowcore.workflow.dto.ApproverRequest;
import com.fractalhive.workflowcore.workflow.dto.StageDefinitionRequest;
import com.fractalhive.workflowcore.workflow.dto.StepDefinitionRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionCreateRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionResponse;
import com.fractalhive.workflowcore.rulesengine.entity.WorkflowStepRule;
import com.fractalhive.workflowcore.rulesengine.repository.WorkflowStepRuleRepository;
import com.fractalhive.workflowcore.workflow.entity.WorkflowDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStageDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepApprover;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.repository.WorkflowDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStageDefinitionRepository;
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
    private final WorkflowStageDefinitionRepository workflowStageDefinitionRepository;
    private final WorkflowStepDefinitionRepository workflowStepDefinitionRepository;
    private final WorkflowStepApproverRepository workflowStepApproverRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStepRuleRepository workflowStepRuleRepository;

    public WorkflowDefinitionServiceImpl(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowStageDefinitionRepository workflowStageDefinitionRepository,
            WorkflowStepDefinitionRepository workflowStepDefinitionRepository,
            WorkflowStepApproverRepository workflowStepApproverRepository,
            WorkflowInstanceRepository workflowInstanceRepository,
            WorkflowStepRuleRepository workflowStepRuleRepository) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStageDefinitionRepository = workflowStageDefinitionRepository;
        this.workflowStepDefinitionRepository = workflowStepDefinitionRepository;
        this.workflowStepApproverRepository = workflowStepApproverRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.workflowStepRuleRepository = workflowStepRuleRepository;
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

        // If a previous version exists, copy its stages, steps and approvers to the new version
        if (previousVersion.isPresent()) {
            UUID previousWorkflowId = previousVersion.get().getId();
            List<WorkflowStageDefinition> previousStages = workflowStageDefinitionRepository
                    .findByWorkflowIdOrderByStageOrderAsc(previousWorkflowId);

            if (!previousStages.isEmpty()) {
                copyStagesStepsAndApprovers(previousWorkflowId, saved.getId(), createdBy);
                logger.info("Copied {} stage(s) from previous version {} v{} (ID: {}) to new version {} v{} (ID: {})",
                        previousStages.size(),
                        previousVersion.get().getName(), previousVersion.get().getVersion(), previousWorkflowId,
                        request.getName(), request.getVersion(), saved.getId());
            }
        }

        return saved.getId();
    }

    @Override
    @Transactional
    public UUID createStage(UUID workflowId, StageDefinitionRequest request, String createdBy) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));

        checkNoInstancesExist(workflowId, workflow.getName(), workflow.getVersion());

        if (request.getStepCompletionType() == ApprovalType.N_OF_M) {
            if (request.getMinStepCompletions() == null || request.getMinStepCompletions() <= 0) {
                throw new IllegalArgumentException("minStepCompletions must be > 0 for N_OF_M step completion type");
            }
        }

        WorkflowStageDefinition stage = new WorkflowStageDefinition();
        stage.setWorkflowId(workflowId);
        stage.setStageName(request.getStageName());
        stage.setStageOrder(request.getStageOrder());
        stage.setStepCompletionType(request.getStepCompletionType());
        stage.setMinStepCompletions(request.getMinStepCompletions());

        Timestamp now = Timestamp.from(Instant.now());
        stage.setCreatedAt(now);
        stage.setCreatedBy(createdBy);

        WorkflowStageDefinition saved = workflowStageDefinitionRepository.save(stage);
        logger.info("Created stage definition: {} (ID: {}) for workflow {}", request.getStageName(), saved.getId(), workflowId);

        if (request.getSteps() != null && !request.getSteps().isEmpty()) {
            for (StepDefinitionRequest stepRequest : request.getSteps()) {
                addStepToStage(saved.getId(), stepRequest, createdBy);
            }
            logger.info("Created {} step(s) for stage {} during stage creation", request.getSteps().size(), saved.getId());
        }

        return saved.getId();
    }

    @Override
    @Transactional
    public void updateStage(UUID stageId, StageDefinitionRequest request, String updatedBy) {
        WorkflowStageDefinition stage = workflowStageDefinitionRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage definition not found: " + stageId));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(stage.getWorkflowId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + stage.getWorkflowId()));

        checkNoInstancesExist(stage.getWorkflowId(), workflow.getName(), workflow.getVersion());

        if (request.getStepCompletionType() == ApprovalType.N_OF_M) {
            if (request.getMinStepCompletions() == null || request.getMinStepCompletions() <= 0) {
                throw new IllegalArgumentException("minStepCompletions must be > 0 for N_OF_M step completion type");
            }
        }

        stage.setStageName(request.getStageName());
        stage.setStageOrder(request.getStageOrder());
        stage.setStepCompletionType(request.getStepCompletionType());
        stage.setMinStepCompletions(request.getMinStepCompletions());
        stage.setUpdatedAt(Timestamp.from(Instant.now()));
        stage.setUpdatedBy(updatedBy);

        workflowStageDefinitionRepository.save(stage);
        logger.info("Updated stage definition: {} (ID: {})", request.getStageName(), stageId);
    }

    @Override
    @Transactional
    public void deleteStage(UUID stageId) {
        WorkflowStageDefinition stage = workflowStageDefinitionRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage definition not found: " + stageId));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(stage.getWorkflowId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + stage.getWorkflowId()));

        checkNoInstancesExist(stage.getWorkflowId(), workflow.getName(), workflow.getVersion());

        List<WorkflowStepDefinition> steps = workflowStepDefinitionRepository.findByStageId(stageId);
        for (WorkflowStepDefinition step : steps) {
            List<WorkflowStepApprover> approvers = workflowStepApproverRepository.findByStepId(step.getId());
            workflowStepApproverRepository.deleteAll(approvers);
            workflowStepDefinitionRepository.delete(step);
        }

        workflowStageDefinitionRepository.delete(stage);
        logger.info("Deleted stage definition: {} (ID: {})", stage.getStageName(), stageId);
    }

    @Override
    @Transactional
    public UUID addStepToStage(UUID stageId, StepDefinitionRequest request, String createdBy) {
        WorkflowStageDefinition stage = workflowStageDefinitionRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage definition not found: " + stageId));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(stage.getWorkflowId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + stage.getWorkflowId()));

        checkNoInstancesExist(stage.getWorkflowId(), workflow.getName(), workflow.getVersion());

        if (request.getApprovalType() == ApprovalType.N_OF_M) {
            if (request.getMinApprovals() == null || request.getMinApprovals() <= 0) {
                throw new IllegalArgumentException("minApprovals must be > 0 for N_OF_M approval type");
            }
        }

        // Validate stepOrder is provided
        if (request.getStepOrder() == null) {
            throw new IllegalArgumentException("stepOrder is required");
        }

        if (request.getApprovers() != null && !request.getApprovers().isEmpty()) {
            int approverCount = request.getApprovers().size();
            if (request.getApprovalType() == ApprovalType.N_OF_M) {
                if (request.getMinApprovals() != null && request.getMinApprovals() > approverCount) {
                    throw new IllegalArgumentException(
                            String.format("minApprovals (%d) cannot exceed total approvers (%d) for N_OF_M approval type",
                                    request.getMinApprovals(), approverCount));
                }
            }
        }

        WorkflowStepDefinition step = new WorkflowStepDefinition();
        step.setStageId(stageId);
        step.setStepName(request.getStepName());
        step.setStepOrder(request.getStepOrder());
        step.setApprovalType(request.getApprovalType());
        step.setMinApprovals(request.getMinApprovals());
        step.setSlaHours(request.getSlaHours());

        Timestamp now = Timestamp.from(Instant.now());
        step.setCreatedAt(now);
        step.setCreatedBy(createdBy);

        WorkflowStepDefinition saved = workflowStepDefinitionRepository.save(step);
        logger.info("Created step definition: {} (ID: {}) for stage {}", request.getStepName(), saved.getId(), stageId);

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

        WorkflowStageDefinition stage = workflowStageDefinitionRepository.findById(step.getStageId())
                .orElseThrow(() -> new IllegalArgumentException("Stage definition not found: " + step.getStageId()));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(stage.getWorkflowId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + stage.getWorkflowId()));

        // Block modification if workflow instances exist
        checkNoInstancesExist(stage.getWorkflowId(), workflow.getName(), workflow.getVersion());

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

        // Delete all stages, steps and approvers first
        List<WorkflowStageDefinition> stages = workflowStageDefinitionRepository
                .findByWorkflowIdOrderByStageOrderAsc(workflowId);

        for (WorkflowStageDefinition stage : stages) {
            List<WorkflowStepDefinition> steps = workflowStepDefinitionRepository.findByStageId(stage.getId());
            for (WorkflowStepDefinition step : steps) {
                List<WorkflowStepApprover> approvers = workflowStepApproverRepository.findByStepId(step.getId());
                workflowStepApproverRepository.deleteAll(approvers);
                workflowStepDefinitionRepository.delete(step);
            }
            workflowStageDefinitionRepository.delete(stage);
        }

        workflowDefinitionRepository.delete(workflow);
        logger.info("Deleted workflow definition: {} v{} (ID: {})", workflow.getName(), workflow.getVersion(), workflowId);
    }

    @Override
    @Transactional
    public void updateStep(UUID stepId, StepDefinitionRequest request, String updatedBy) {
        WorkflowStepDefinition step = workflowStepDefinitionRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepId));

        WorkflowStageDefinition stage = workflowStageDefinitionRepository.findById(step.getStageId())
                .orElseThrow(() -> new IllegalArgumentException("Stage definition not found: " + step.getStageId()));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(stage.getWorkflowId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + stage.getWorkflowId()));

        // Block modification if workflow instances exist
        checkNoInstancesExist(stage.getWorkflowId(), workflow.getName(), workflow.getVersion());

        // Validate minApprovals for N_OF_M
        if (request.getApprovalType() == ApprovalType.N_OF_M) {
            if (request.getMinApprovals() == null || request.getMinApprovals() <= 0) {
                throw new IllegalArgumentException("minApprovals must be > 0 for N_OF_M approval type");
            }
        }

        // Validate and update stepOrder
        if (request.getStepOrder() == null) {
            throw new IllegalArgumentException("stepOrder is required");
        }

        // Note: stepOrder is NOT unique - multiple steps can have the same order for parallel execution

        step.setStepName(request.getStepName());
        step.setStepOrder(request.getStepOrder());
        step.setApprovalType(request.getApprovalType());
        step.setMinApprovals(request.getMinApprovals());
        step.setSlaHours(request.getSlaHours());
        step.setUpdatedAt(Timestamp.from(Instant.now()));
        step.setUpdatedBy(updatedBy);

        workflowStepDefinitionRepository.save(step);
        
        // Handle approvers update if provided
        if (request.getApprovers() != null && !request.getApprovers().isEmpty()) {
            // Get existing approvers
            List<WorkflowStepApprover> existingApprovers = workflowStepApproverRepository.findByStepId(stepId);
            
            // Validate minApprovals constraint for N_OF_M with new approver count
            if (request.getApprovalType() == ApprovalType.N_OF_M) {
                if (request.getMinApprovals() != null && request.getMinApprovals() > request.getApprovers().size()) {
                    throw new IllegalArgumentException(
                            String.format("minApprovals (%d) cannot exceed total approvers (%d) for N_OF_M approval type",
                                    request.getMinApprovals(), request.getApprovers().size()));
                }
            }
            
            // Delete all existing approvers
            workflowStepApproverRepository.deleteAll(existingApprovers);
            
            // Create new approvers
            Timestamp now = Timestamp.from(Instant.now());
            createApproversForStep(stepId, request.getApprovers(), updatedBy, now);
            
            logger.info("Updated {} approver(s) for step {} (ID: {})", 
                    request.getApprovers().size(), request.getStepName(), stepId);
        }
        
        logger.info("Updated step definition: {} (ID: {})", request.getStepName(), stepId);
    }

    @Override
    @Transactional
    public void deleteStep(UUID stepId) {
        WorkflowStepDefinition step = workflowStepDefinitionRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepId));

        WorkflowStageDefinition stage = workflowStageDefinitionRepository.findById(step.getStageId())
                .orElseThrow(() -> new IllegalArgumentException("Stage definition not found: " + step.getStageId()));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(stage.getWorkflowId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + stage.getWorkflowId()));

        // Block modification if workflow instances exist
        checkNoInstancesExist(stage.getWorkflowId(), workflow.getName(), workflow.getVersion());

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

        WorkflowStageDefinition stage = workflowStageDefinitionRepository.findById(step.getStageId())
                .orElseThrow(() -> new IllegalArgumentException("Stage definition not found: " + step.getStageId()));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(stage.getWorkflowId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + stage.getWorkflowId()));

        // Block modification if workflow instances exist
        checkNoInstancesExist(stage.getWorkflowId(), workflow.getName(), workflow.getVersion());

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
     * Copies all stages, steps, approvers, and rules from one workflow definition to another.
     *
     * @param sourceWorkflowId the source workflow definition ID
     * @param targetWorkflowId the target workflow definition ID
     * @param createdBy        the user performing the copy
     */
    private void copyStagesStepsAndApprovers(UUID sourceWorkflowId, UUID targetWorkflowId, String createdBy) {
        Timestamp now = Timestamp.from(Instant.now());

        List<WorkflowStageDefinition> originalStages = workflowStageDefinitionRepository
                .findByWorkflowIdOrderByStageOrderAsc(sourceWorkflowId);

        int totalStepsCopied = 0;
        int totalRulesCopied = 0;

        for (WorkflowStageDefinition originalStage : originalStages) {
            // Create new stage
            WorkflowStageDefinition newStage = new WorkflowStageDefinition();
            newStage.setWorkflowId(targetWorkflowId);
            newStage.setStageName(originalStage.getStageName());
            newStage.setStageOrder(originalStage.getStageOrder());
            newStage.setStepCompletionType(originalStage.getStepCompletionType());
            newStage.setMinStepCompletions(originalStage.getMinStepCompletions());
            newStage.setCreatedAt(now);
            newStage.setCreatedBy(createdBy);
            newStage = workflowStageDefinitionRepository.save(newStage);

            // Copy all steps for this stage
            List<WorkflowStepDefinition> originalSteps = workflowStepDefinitionRepository
                    .findByStageIdOrderByStepOrderAsc(originalStage.getId());

            for (WorkflowStepDefinition originalStep : originalSteps) {
                // Create new step
                WorkflowStepDefinition newStep = new WorkflowStepDefinition();
                newStep.setStageId(newStage.getId());
                newStep.setStepName(originalStep.getStepName());
                newStep.setStepOrder(originalStep.getStepOrder());
                newStep.setApprovalType(originalStep.getApprovalType());
                newStep.setMinApprovals(originalStep.getMinApprovals());
                newStep.setSlaHours(originalStep.getSlaHours());
                newStep.setCreatedAt(now);
                newStep.setCreatedBy(createdBy);
                newStep = workflowStepDefinitionRepository.save(newStep);
                totalStepsCopied++;

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

                // Copy all rules for this step
                List<WorkflowStepRule> originalRules = workflowStepRuleRepository.findByStepDefinitionId(originalStep.getId());
                for (WorkflowStepRule originalRule : originalRules) {
                    WorkflowStepRule newRule = new WorkflowStepRule();
                    newRule.setStepDefinitionId(newStep.getId());
                    newRule.setRuleName(originalRule.getRuleName());
                    newRule.setRuleType(originalRule.getRuleType());
                    newRule.setRuleExpression(originalRule.getRuleExpression());
                    newRule.setPriority(originalRule.getPriority());
                    newRule.setIsActive(originalRule.getIsActive());
                    newRule.setDescription(originalRule.getDescription());
                    newRule.setCreatedAt(now);
                    newRule.setCreatedBy(createdBy);
                    workflowStepRuleRepository.save(newRule);
                    totalRulesCopied++;
                }
            }
        }

        logger.info("Copied {} stage(s), {} step(s), their approvers, and {} rule(s) from workflow {} to workflow {}",
                originalStages.size(), totalStepsCopied, totalRulesCopied, sourceWorkflowId, targetWorkflowId);
    }

    private WorkflowDefinitionResponse toResponse(WorkflowDefinition workflow) {
        List<WorkflowStageDefinition> stages = workflowStageDefinitionRepository
                .findByWorkflowIdOrderByStageOrderAsc(workflow.getId());

        List<WorkflowDefinitionResponse.StageDefinitionResponse> stageResponses = stages.stream()
                .map(stage -> {
                    List<WorkflowStepDefinition> steps = workflowStepDefinitionRepository
                            .findByStageIdOrderByStepOrderAsc(stage.getId());

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

                    return WorkflowDefinitionResponse.StageDefinitionResponse.builder()
                            .stageId(stage.getId())
                            .stageName(stage.getStageName())
                            .stageOrder(stage.getStageOrder())
                            .stepCompletionType(stage.getStepCompletionType().name())
                            .minStepCompletions(stage.getMinStepCompletions())
                            .steps(stepResponses)
                            .build();
                })
                .collect(Collectors.toList());

        return WorkflowDefinitionResponse.builder()
                .workflowId(workflow.getId())
                .name(workflow.getName())
                .version(workflow.getVersion())
                .isActive(workflow.getIsActive())
                .stages(stageResponses)
                .build();
    }
}
