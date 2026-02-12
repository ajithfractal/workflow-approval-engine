package com.fractalhive.workflowcore.workitem.statemachine.service;

import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import com.fractalhive.workflowcore.workitem.statemachine.enums.WorkItemEvent;
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
 * Service that drives the Work Item State Machine.
 * Manages work item state machines and handles state transitions.
 */
@Service
public class WorkItemStateMachineService {

    private static final String CONTENT_REF_HEADER = "contentRef";
    private static final String SUBMITTED_BY_HEADER = "submittedBy";
    private static final String USER_ID_HEADER = "userId";
    private static final String WORK_ITEM_EXTENDED_STATE_KEY = "workItem";

    private final StateMachineFactory<WorkItemStatus, WorkItemEvent> stateMachineFactory;
    private final WorkItemRepository workItemRepository;

    public WorkItemStateMachineService(
            @Qualifier("workItemStateMachineFactory") StateMachineFactory<WorkItemStatus, WorkItemEvent> stateMachineFactory,
            WorkItemRepository workItemRepository) {
        this.stateMachineFactory = stateMachineFactory;
        this.workItemRepository = workItemRepository;
    }

    @Transactional
    public void submit(UUID workItemId, String contentRef, String submittedBy) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        StateMachine<WorkItemStatus, WorkItemEvent> stateMachine = createAndRestoreStateMachine(workItem);

        Message<WorkItemEvent> message = MessageBuilder
                .withPayload(WorkItemEvent.SUBMIT)
                .setHeader(CONTENT_REF_HEADER, contentRef)
                .setHeader(SUBMITTED_BY_HEADER, submittedBy)
                .build();

        stateMachine.sendEvent(message);
        persistState(workItem, stateMachine);
    }

    @Transactional
    public void startReview(UUID workItemId, String userId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        StateMachine<WorkItemStatus, WorkItemEvent> stateMachine = createAndRestoreStateMachine(workItem);

        Message<WorkItemEvent> message = MessageBuilder
                .withPayload(WorkItemEvent.START_REVIEW)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workItem, stateMachine);
    }

    @Transactional
    public void approve(UUID workItemId, String userId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        StateMachine<WorkItemStatus, WorkItemEvent> stateMachine = createAndRestoreStateMachine(workItem);

        Message<WorkItemEvent> message = MessageBuilder
                .withPayload(WorkItemEvent.APPROVE)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workItem, stateMachine);
    }

    @Transactional
    public void reject(UUID workItemId, String userId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        StateMachine<WorkItemStatus, WorkItemEvent> stateMachine = createAndRestoreStateMachine(workItem);

        Message<WorkItemEvent> message = MessageBuilder
                .withPayload(WorkItemEvent.REJECT)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workItem, stateMachine);
    }

    @Transactional
    public void sendToRework(UUID workItemId, String userId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        StateMachine<WorkItemStatus, WorkItemEvent> stateMachine = createAndRestoreStateMachine(workItem);

        Message<WorkItemEvent> message = MessageBuilder
                .withPayload(WorkItemEvent.SEND_TO_REWORK)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workItem, stateMachine);
    }

    @Transactional
    public void archive(UUID workItemId, String userId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        StateMachine<WorkItemStatus, WorkItemEvent> stateMachine = createAndRestoreStateMachine(workItem);

        Message<WorkItemEvent> message = MessageBuilder
                .withPayload(WorkItemEvent.ARCHIVE)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workItem, stateMachine);
    }

    @Transactional
    public void cancel(UUID workItemId, String userId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        StateMachine<WorkItemStatus, WorkItemEvent> stateMachine = createAndRestoreStateMachine(workItem);

        Message<WorkItemEvent> message = MessageBuilder
                .withPayload(WorkItemEvent.CANCEL)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(workItem, stateMachine);
    }

    private StateMachine<WorkItemStatus, WorkItemEvent> createAndRestoreStateMachine(WorkItem workItem) {
        StateMachine<WorkItemStatus, WorkItemEvent> stateMachine = stateMachineFactory.getStateMachine();

        stateMachine.getExtendedState().getVariables().put(WORK_ITEM_EXTENDED_STATE_KEY, workItem);

        // Always restore the state machine to the work item's current state
        // This ensures the state machine is properly initialized
        WorkItemStatus currentStatus = workItem.getStatus() != null 
                ? workItem.getStatus() 
                : WorkItemStatus.DRAFT;
        
        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(access -> {
                    access.resetStateMachine(
                            new DefaultStateMachineContext<>(
                                    currentStatus, null, null, null));
                });
        
        // Start the state machine to ensure it's fully initialized
        stateMachine.start();

        return stateMachine;
    }

    private void persistState(WorkItem workItem, StateMachine<WorkItemStatus, WorkItemEvent> stateMachine) {
        if (stateMachine.getState() == null || stateMachine.getState().getId() == null) {
            throw new IllegalStateException("State machine state is null after transition");
        }
        WorkItemStatus currentState = stateMachine.getState().getId();
        workItem.setStatus(currentState);
        workItemRepository.save(workItem);
    }

    private WorkItem getWorkItemOrThrow(UUID workItemId) {
        return workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + workItemId));
    }
}
