package com.fractalhive.workflowcore.workflow.statemachine.service;

import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.enums.WorkflowInstanceEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service that drives the Workflow Instance State Machine.
 * Manages workflow instance state machines and handles state transitions.
 */
@Service
public class WorkflowInstanceStateMachineService {

    private static final String USER_ID_HEADER = "userId";
    private static final String REASON_HEADER = "reason";
    private static final String WORKFLOW_INSTANCE_EXTENDED_STATE_KEY = "workflowInstance";

    private final StateMachineFactory<WorkflowStatus, WorkflowInstanceEvent> stateMachineFactory;
    private final WorkflowInstanceRepository workflowInstanceRepository;

    public WorkflowInstanceStateMachineService(
            StateMachineFactory<WorkflowStatus, WorkflowInstanceEvent> stateMachineFactory,
            WorkflowInstanceRepository workflowInstanceRepository) {
        this.stateMachineFactory = stateMachineFactory;
        this.workflowInstanceRepository = workflowInstanceRepository;
    }

    @Transactional
    public void start(UUID workflowInstanceId, String userId) {
        WorkflowInstance workflowInstance = getWorkflowInstanceOrThrow(workflowInstanceId);
        StateMachine<WorkflowStatus, WorkflowInstanceEvent> stateMachine = createAndRestoreStateMachine(workflowInstance);

        Message<WorkflowInstanceEvent> message = MessageBuilder
                .withPayload(WorkflowInstanceEvent.START)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workflowInstance, stateMachine);
    }

    @Transactional
    public void complete(UUID workflowInstanceId, String userId) {
        WorkflowInstance workflowInstance = getWorkflowInstanceOrThrow(workflowInstanceId);
        StateMachine<WorkflowStatus, WorkflowInstanceEvent> stateMachine = createAndRestoreStateMachine(workflowInstance);

        Message<WorkflowInstanceEvent> message = MessageBuilder
                .withPayload(WorkflowInstanceEvent.COMPLETE)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workflowInstance, stateMachine);
    }

    @Transactional
    public void fail(UUID workflowInstanceId, String userId, String reason) {
        WorkflowInstance workflowInstance = getWorkflowInstanceOrThrow(workflowInstanceId);
        StateMachine<WorkflowStatus, WorkflowInstanceEvent> stateMachine = createAndRestoreStateMachine(workflowInstance);

        Message<WorkflowInstanceEvent> message = MessageBuilder
                .withPayload(WorkflowInstanceEvent.FAIL)
                .setHeader(USER_ID_HEADER, userId)
                .setHeader(REASON_HEADER, reason)
                .build();

        stateMachine.sendEvent(message);
        persistState(workflowInstance, stateMachine);
    }

    @Transactional
    public void cancel(UUID workflowInstanceId, String userId) {
        WorkflowInstance workflowInstance = getWorkflowInstanceOrThrow(workflowInstanceId);
        StateMachine<WorkflowStatus, WorkflowInstanceEvent> stateMachine = createAndRestoreStateMachine(workflowInstance);

        Message<WorkflowInstanceEvent> message = MessageBuilder
                .withPayload(WorkflowInstanceEvent.CANCEL)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workflowInstance, stateMachine);
    }

    private StateMachine<WorkflowStatus, WorkflowInstanceEvent> createAndRestoreStateMachine(WorkflowInstance workflowInstance) {
        StateMachine<WorkflowStatus, WorkflowInstanceEvent> stateMachine = stateMachineFactory.getStateMachine();

        stateMachine.getExtendedState().getVariables().put(WORKFLOW_INSTANCE_EXTENDED_STATE_KEY, workflowInstance);

        if (workflowInstance.getStatus() != null && workflowInstance.getStatus() != WorkflowStatus.NOT_STARTED) {
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(access -> {
                        access.resetStateMachine(
                                new DefaultStateMachineContext<>(
                                        workflowInstance.getStatus(), null, null, null));
                    });
        }

        return stateMachine;
    }

    private void persistState(WorkflowInstance workflowInstance, StateMachine<WorkflowStatus, WorkflowInstanceEvent> stateMachine) {
        WorkflowStatus currentState = stateMachine.getState().getId();
        workflowInstance.setStatus(currentState);
        workflowInstanceRepository.save(workflowInstance);
    }

    private WorkflowInstance getWorkflowInstanceOrThrow(UUID workflowInstanceId) {
        return workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + workflowInstanceId));
    }
}
