package com.fractalhive.workflowcore.workflow.statemachine.service;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStageInstance;
import com.fractalhive.workflowcore.workflow.enums.StageStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStageInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.enums.StageEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service that drives the Workflow Stage Instance State Machine.
 * Manages workflow stage instance state machines and handles state transitions.
 */
@Service
public class WorkflowStageInstanceStateMachineService {

    private static final String USER_ID_HEADER = "userId";
    private static final String REASON_HEADER = "reason";
    private static final String STAGE_INSTANCE_EXTENDED_STATE_KEY = "stageInstance";

    private final StateMachineFactory<StageStatus, StageEvent> stateMachineFactory;
    private final WorkflowStageInstanceRepository workflowStageInstanceRepository;

    public WorkflowStageInstanceStateMachineService(
            @Qualifier("workflowStageInstanceStateMachineFactory") StateMachineFactory<StageStatus, StageEvent> stateMachineFactory,
            WorkflowStageInstanceRepository workflowStageInstanceRepository) {
        this.stateMachineFactory = stateMachineFactory;
        this.workflowStageInstanceRepository = workflowStageInstanceRepository;
    }

    @Transactional
    public void start(UUID stageInstanceId, String userId) {
        WorkflowStageInstance stageInstance = getStageInstanceOrThrow(stageInstanceId);
        StateMachine<StageStatus, StageEvent> stateMachine = createAndRestoreStateMachine(stageInstance);

        Message<StageEvent> message = MessageBuilder
                .withPayload(StageEvent.START)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(stageInstance, stateMachine);
    }

    @Transactional
    public void complete(UUID stageInstanceId, String userId) {
        WorkflowStageInstance stageInstance = getStageInstanceOrThrow(stageInstanceId);
        StateMachine<StageStatus, StageEvent> stateMachine = createAndRestoreStateMachine(stageInstance);

        Message<StageEvent> message = MessageBuilder
                .withPayload(StageEvent.COMPLETE)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(stageInstance, stateMachine);
    }

    @Transactional
    public void fail(UUID stageInstanceId, String userId, String reason) {
        WorkflowStageInstance stageInstance = getStageInstanceOrThrow(stageInstanceId);
        StateMachine<StageStatus, StageEvent> stateMachine = createAndRestoreStateMachine(stageInstance);

        Message<StageEvent> message = MessageBuilder
                .withPayload(StageEvent.FAIL)
                .setHeader(USER_ID_HEADER, userId)
                .setHeader(REASON_HEADER, reason)
                .build();

        stateMachine.sendEvent(message);
        persistState(stageInstance, stateMachine);
    }

    private StateMachine<StageStatus, StageEvent> createAndRestoreStateMachine(WorkflowStageInstance stageInstance) {
        StateMachine<StageStatus, StageEvent> stateMachine = stateMachineFactory.getStateMachine();

        stateMachine.getExtendedState().getVariables().put(STAGE_INSTANCE_EXTENDED_STATE_KEY, stageInstance);

        StageStatus currentStatus = stageInstance.getStatus() != null 
                ? stageInstance.getStatus() 
                : StageStatus.NOT_STARTED;
        
        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(access -> {
                    access.resetStateMachine(
                            new DefaultStateMachineContext<>(
                                    currentStatus, null, null, null));
                });

        stateMachine.start();

        return stateMachine;
    }

    private void persistState(WorkflowStageInstance stageInstance, StateMachine<StageStatus, StageEvent> stateMachine) {
        if (stateMachine.getState() == null || stateMachine.getState().getId() == null) {
            throw new IllegalStateException("State machine state is null after transition");
        }
        StageStatus currentState = stateMachine.getState().getId();
        stageInstance.setStatus(currentState);
        workflowStageInstanceRepository.save(stageInstance);
    }

    private WorkflowStageInstance getStageInstanceOrThrow(UUID stageInstanceId) {
        return workflowStageInstanceRepository.findById(stageInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow stage instance not found: " + stageInstanceId));
    }
}
