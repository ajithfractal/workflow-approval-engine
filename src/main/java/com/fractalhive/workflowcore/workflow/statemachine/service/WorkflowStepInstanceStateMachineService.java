package com.fractalhive.workflowcore.workflow.statemachine.service;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.enums.WorkflowStepInstanceEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service that drives the Workflow Step Instance State Machine.
 * Manages workflow step instance state machines and handles state transitions.
 */
@Service
public class WorkflowStepInstanceStateMachineService {

    private static final String USER_ID_HEADER = "userId";
    private static final String REASON_HEADER = "reason";
    private static final String STEP_INSTANCE_EXTENDED_STATE_KEY = "stepInstance";

    private final StateMachineFactory<StepStatus, WorkflowStepInstanceEvent> stateMachineFactory;
    private final WorkflowStepInstanceRepository workflowStepInstanceRepository;

    public WorkflowStepInstanceStateMachineService(
            StateMachineFactory<StepStatus, WorkflowStepInstanceEvent> stateMachineFactory,
            WorkflowStepInstanceRepository workflowStepInstanceRepository) {
        this.stateMachineFactory = stateMachineFactory;
        this.workflowStepInstanceRepository = workflowStepInstanceRepository;
    }

    @Transactional
    public void start(UUID stepInstanceId, String userId) {
        WorkflowStepInstance stepInstance = getStepInstanceOrThrow(stepInstanceId);
        StateMachine<StepStatus, WorkflowStepInstanceEvent> stateMachine = createAndRestoreStateMachine(stepInstance);

        Message<WorkflowStepInstanceEvent> message = MessageBuilder
                .withPayload(WorkflowStepInstanceEvent.START)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(stepInstance, stateMachine);
    }

    @Transactional
    public void complete(UUID stepInstanceId, String userId) {
        WorkflowStepInstance stepInstance = getStepInstanceOrThrow(stepInstanceId);
        StateMachine<StepStatus, WorkflowStepInstanceEvent> stateMachine = createAndRestoreStateMachine(stepInstance);

        Message<WorkflowStepInstanceEvent> message = MessageBuilder
                .withPayload(WorkflowStepInstanceEvent.COMPLETE)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(stepInstance, stateMachine);
    }

    @Transactional
    public void fail(UUID stepInstanceId, String userId, String reason) {
        WorkflowStepInstance stepInstance = getStepInstanceOrThrow(stepInstanceId);
        StateMachine<StepStatus, WorkflowStepInstanceEvent> stateMachine = createAndRestoreStateMachine(stepInstance);

        Message<WorkflowStepInstanceEvent> message = MessageBuilder
                .withPayload(WorkflowStepInstanceEvent.FAIL)
                .setHeader(USER_ID_HEADER, userId)
                .setHeader(REASON_HEADER, reason)
                .build();

        stateMachine.sendEvent(message);
        persistState(stepInstance, stateMachine);
    }

    private StateMachine<StepStatus, WorkflowStepInstanceEvent> createAndRestoreStateMachine(WorkflowStepInstance stepInstance) {
        StateMachine<StepStatus, WorkflowStepInstanceEvent> stateMachine = stateMachineFactory.getStateMachine();

        stateMachine.getExtendedState().getVariables().put(STEP_INSTANCE_EXTENDED_STATE_KEY, stepInstance);

        if (stepInstance.getStatus() != null && stepInstance.getStatus() != StepStatus.NOT_STARTED) {
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(access -> {
                        access.resetStateMachine(
                                new DefaultStateMachineContext<>(
                                        stepInstance.getStatus(), null, null, null));
                    });
        }

        return stateMachine;
    }

    private void persistState(WorkflowStepInstance stepInstance, StateMachine<StepStatus, WorkflowStepInstanceEvent> stateMachine) {
        StepStatus currentState = stateMachine.getState().getId();
        stepInstance.setStatus(currentState);
        workflowStepInstanceRepository.save(stepInstance);
    }

    private WorkflowStepInstance getStepInstanceOrThrow(UUID stepInstanceId) {
        return workflowStepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow step instance not found: " + stepInstanceId));
    }
}
