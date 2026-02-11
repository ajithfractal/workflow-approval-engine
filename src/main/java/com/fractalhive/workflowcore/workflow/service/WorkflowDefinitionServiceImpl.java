package com.fractalhive.workflowcore.workflow.service;

import com.fractalhive.workflowcore.approval.enums.ApprovalType;
import com.fractalhive.workflowcore.workflow.dto.ApproverRequest;
import com.fractalhive.workflowcore.workflow.dto.StepDefinitionRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionCreateRequest;
import com.fractalhive.workflowcore.workflow.dto.WorkflowDefinitionResponse;
import com.fractalhive.workflowcore.workflow.entity.WorkflowDefinition;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepApprover;
import com.fractalhive.workflowcore.workflow.entity.WorkflowStepDefinition;
import com.fractalhive.workflowcore.workflow.repository.WorkflowDefinitionRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepApproverRepository;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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

    public WorkflowDefinitionServiceImpl(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowStepDefinitionRepository workflowStepDefinitionRepository,
            WorkflowStepApproverRepository workflowStepApproverRepository) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStepDefinitionRepository = workflowStepDefinitionRepository;
        this.workflowStepApproverRepository = workflowStepApproverRepository;
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

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName(request.getName());
        workflow.setVersion(request.getVersion());
        workflow.setIsActive(true);

        Timestamp now = Timestamp.from(Instant.now());
        workflow.setCreatedAt(now);
        workflow.setCreatedBy(createdBy);

        WorkflowDefinition saved = workflowDefinitionRepository.save(workflow);
        logger.info("Created workflow definition: {} v{} (ID: {})", request.getName(), request.getVersion(), saved.getId());
        return saved.getId();
    }

    @Override
    @Transactional
    public UUID createStep(UUID workflowId, StepDefinitionRequest request, String createdBy) {
        // Verify workflow exists
        workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));

        // Validate minApprovals for N_OF_M approval type
        if (request.getApprovalType() == ApprovalType.N_OF_M) {
            if (request.getMinApprovals() == null || request.getMinApprovals() <= 0) {
                throw new IllegalArgumentException("minApprovals must be > 0 for N_OF_M approval type");
            }
        }

        // Determine step order
        final Integer stepOrder;
        if (request.getStepOrder() == null) {
            // Auto-assign step order as next available
            List<WorkflowStepDefinition> existingSteps = workflowStepDefinitionRepository
                    .findByWorkflowIdOrderByStepOrderAsc(workflowId);
            stepOrder = existingSteps.isEmpty() ? 1 : existingSteps.get(existingSteps.size() - 1).getStepOrder() + 1;
        } else {
            // Validate step order uniqueness
            List<WorkflowStepDefinition> existingSteps = workflowStepDefinitionRepository
                    .findByWorkflowIdOrderByStepOrderAsc(workflowId);
            boolean orderExists = existingSteps.stream()
                    .anyMatch(step -> step.getStepOrder().equals(request.getStepOrder()));
            if (orderExists) {
                throw new IllegalArgumentException(
                        String.format("Step order %d already exists for workflow %s", request.getStepOrder(), workflowId));
            }
            stepOrder = request.getStepOrder();
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
        return saved.getId();
    }

    @Override
    @Transactional
    public UUID addApprover(UUID stepId, ApproverRequest request, String createdBy) {
        WorkflowStepDefinition step = workflowStepDefinitionRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step definition not found: " + stepId));

        // Validate minApprovals constraint for N_OF_M
        if (step.getApprovalType() == ApprovalType.N_OF_M) {
            List<WorkflowStepApprover> existingApprovers = workflowStepApproverRepository.findByStepId(stepId);
            int totalApprovers = existingApprovers.size() + 1; // +1 for the new approver
            if (step.getMinApprovals() != null && step.getMinApprovals() > totalApprovers) {
                throw new IllegalArgumentException(
                        String.format("minApprovals (%d) cannot exceed total approvers (%d) for N_OF_M approval type",
                                step.getMinApprovals(), totalApprovers));
            }
        }

        WorkflowStepApprover approver = new WorkflowStepApprover();
        approver.setStepId(stepId);
        approver.setApproverType(request.getApproverType());
        approver.setApproverValue(request.getApproverValue());

        Timestamp now = Timestamp.from(Instant.now());
        approver.setCreatedAt(now);
        approver.setCreatedBy(createdBy);

        WorkflowStepApprover saved = workflowStepApproverRepository.save(approver);
        logger.info("Added approver {} (ID: {}) to step {}", request.getApproverValue(), saved.getId(), stepId);
        return saved.getId();
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
