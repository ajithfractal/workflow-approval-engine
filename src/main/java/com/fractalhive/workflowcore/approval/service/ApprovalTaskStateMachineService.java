package com.fractalhive.workflowcore.approval.service;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service that drives the Approval Task State Machine.
 * Manages individual task state machines and handles state transitions.
 */
@Service
public class ApprovalTaskStateMachineService {

    private static final String USER_ID_HEADER = "userId";
    private static final String COMMENTS_HEADER = "comments";
    private static final String TO_USER_ID_HEADER = "toUserId";
    private static final String DECISION_TYPE_HEADER = "decisionType";
    private static final String TASK_EXTENDED_STATE_KEY = "task";

    private final StateMachineFactory<TaskStatus, ApprovalTaskEvent> stateMachineFactory;
    private final ApprovalTaskRepository approvalTaskRepository;

    public ApprovalTaskStateMachineService(
            StateMachineFactory<TaskStatus, ApprovalTaskEvent> stateMachineFactory,
            ApprovalTaskRepository approvalTaskRepository) {
        this.stateMachineFactory = stateMachineFactory;
        this.approvalTaskRepository = approvalTaskRepository;
    }

    /**
     * Approves an approval task.
     *
     * @param taskId   the approval task ID
     * @param userId   the user ID approving
     * @param comments optional comments
     */
    @Transactional
    public void approve(UUID taskId, String userId, String comments) {
        ApprovalTask task = getTaskOrThrow(taskId);
        StateMachine<TaskStatus, ApprovalTaskEvent> stateMachine = createAndRestoreStateMachine(task);

        Message<ApprovalTaskEvent> message = MessageBuilder
                .withPayload(ApprovalTaskEvent.APPROVE)
                .setHeader(USER_ID_HEADER, userId)
                .setHeader(COMMENTS_HEADER, comments)
                .setHeader(DECISION_TYPE_HEADER, DecisionType.APPROVED)
                .build();

        stateMachine.sendEvent(message);
        persistState(task, stateMachine);
    }

    /**
     * Rejects an approval task.
     *
     * @param taskId   the approval task ID
     * @param userId   the user ID rejecting
     * @param comments optional comments
     */
    @Transactional
    public void reject(UUID taskId, String userId, String comments) {
        ApprovalTask task = getTaskOrThrow(taskId);
        StateMachine<TaskStatus, ApprovalTaskEvent> stateMachine = createAndRestoreStateMachine(task);

        Message<ApprovalTaskEvent> message = MessageBuilder
                .withPayload(ApprovalTaskEvent.REJECT)
                .setHeader(USER_ID_HEADER, userId)
                .setHeader(COMMENTS_HEADER, comments)
                .setHeader(DECISION_TYPE_HEADER, DecisionType.REJECTED)
                .build();

        stateMachine.sendEvent(message);
        persistState(task, stateMachine);
    }

    /**
     * Delegates an approval task to another user.
     *
     * @param taskId     the approval task ID
     * @param fromUserId the current approver user ID
     * @param toUserId   the delegate user ID
     */
    @Transactional
    public void delegate(UUID taskId, String fromUserId, String toUserId) {
        ApprovalTask task = getTaskOrThrow(taskId);
        StateMachine<TaskStatus, ApprovalTaskEvent> stateMachine = createAndRestoreStateMachine(task);

        Message<ApprovalTaskEvent> message = MessageBuilder
                .withPayload(ApprovalTaskEvent.DELEGATE)
                .setHeader(USER_ID_HEADER, fromUserId)
                .setHeader(TO_USER_ID_HEADER, toUserId)
                .build();

        stateMachine.sendEvent(message);
        persistState(task, stateMachine);
    }

    /**
     * Accepts a delegated task.
     *
     * @param taskId the approval task ID
     * @param userId the user ID accepting the delegation
     */
    @Transactional
    public void acceptDelegation(UUID taskId, String userId) {
        ApprovalTask task = getTaskOrThrow(taskId);
        StateMachine<TaskStatus, ApprovalTaskEvent> stateMachine = createAndRestoreStateMachine(task);

        Message<ApprovalTaskEvent> message = MessageBuilder
                .withPayload(ApprovalTaskEvent.ACCEPT)
                .setHeader(USER_ID_HEADER, userId)
                .build();

        stateMachine.sendEvent(message);
        persistState(task, stateMachine);
    }

    /**
     * Expires a task due to SLA breach.
     *
     * @param taskId the approval task ID
     */
    @Transactional
    public void expireTask(UUID taskId) {
        ApprovalTask task = getTaskOrThrow(taskId);
        StateMachine<TaskStatus, ApprovalTaskEvent> stateMachine = createAndRestoreStateMachine(task);

        Message<ApprovalTaskEvent> message = MessageBuilder
                .withPayload(ApprovalTaskEvent.SLA_BREACH)
                .build();

        stateMachine.sendEvent(message);
        persistState(task, stateMachine);
    }

    /**
     * Cancels all pending tasks for a step instance.
     *
     * @param stepInstanceId the step instance ID
     */
    @Transactional
    public void cancelAllForStep(UUID stepInstanceId) {
        List<ApprovalTask> pendingTasks = approvalTaskRepository.findByStepInstanceIdAndStatus(
                stepInstanceId, TaskStatus.PENDING);

        for (ApprovalTask task : pendingTasks) {
            StateMachine<TaskStatus, ApprovalTaskEvent> stateMachine = createAndRestoreStateMachine(task);

            Message<ApprovalTaskEvent> message = MessageBuilder
                    .withPayload(ApprovalTaskEvent.WORKFLOW_CANCELLED)
                    .build();

            stateMachine.sendEvent(message);
            persistState(task, stateMachine);
        }
    }

    /**
     * Creates a state machine instance and restores it to the task's current state.
     */
    private StateMachine<TaskStatus, ApprovalTaskEvent> createAndRestoreStateMachine(ApprovalTask task) {
        StateMachine<TaskStatus, ApprovalTaskEvent> stateMachine = stateMachineFactory.getStateMachine();
        
        // Put task in extended state for guards/actions to access
        stateMachine.getExtendedState().getVariables().put(TASK_EXTENDED_STATE_KEY, task);
        
        // Restore to the task's current persisted state if not PENDING
        if (task.getStatus() != null && task.getStatus() != TaskStatus.PENDING) {
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(access -> {
                        access.resetStateMachine(
                                new DefaultStateMachineContext<>(
                                        task.getStatus(), null, null, null));
                    });
        }
        
        return stateMachine;
    }

    /**
     * Persists the state machine's current state back to the task entity.
     */
    private void persistState(ApprovalTask task, StateMachine<TaskStatus, ApprovalTaskEvent> stateMachine) {
        TaskStatus currentState = stateMachine.getState().getId();
        task.setStatus(currentState);
        approvalTaskRepository.save(task);
    }

    /**
     * Gets a task by ID or throws an exception if not found.
     */
    private ApprovalTask getTaskOrThrow(UUID taskId) {
        return approvalTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Approval task not found: " + taskId));
    }
}
