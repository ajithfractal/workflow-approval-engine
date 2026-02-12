package com.fractalhive.workflowcore.approval.statemachine.config;

import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalCommentRepository;
import com.fractalhive.workflowcore.approval.repository.ApprovalDecisionRepository;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import com.fractalhive.workflowcore.approval.statemachine.action.*;
import com.fractalhive.workflowcore.approval.statemachine.guard.DelegateAcceptGuard;
import com.fractalhive.workflowcore.approval.statemachine.guard.TaskPendingGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.Set;

/**
 * Configuration for the Approval Task State Machine.
 * Defines states, transitions, guards, and actions for approval task lifecycle.
 */
@Configuration
@EnableStateMachineFactory(name = "approvalTaskStateMachineFactory")
public class ApprovalTaskStateMachineConfig extends StateMachineConfigurerAdapter<TaskStatus, ApprovalTaskEvent> {

    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final ApprovalCommentRepository approvalCommentRepository;

    public ApprovalTaskStateMachineConfig(ApprovalTaskRepository approvalTaskRepository,
                                         ApprovalDecisionRepository approvalDecisionRepository,
                                         ApprovalCommentRepository approvalCommentRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalDecisionRepository = approvalDecisionRepository;
        this.approvalCommentRepository = approvalCommentRepository;
    }

    @Override
    public void configure(StateMachineStateConfigurer<TaskStatus, ApprovalTaskEvent> states) throws Exception {
        states
            .withStates()
            .initial(TaskStatus.PENDING)
            .states(Set.of(TaskStatus.values()));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TaskStatus, ApprovalTaskEvent> transitions) throws Exception {
        TaskPendingGuard taskPendingGuard = new TaskPendingGuard();
        DelegateAcceptGuard delegateAcceptGuard = new DelegateAcceptGuard();
        
        RecordApprovalDecisionAction recordApprovalDecisionAction = 
            new RecordApprovalDecisionAction(approvalTaskRepository, approvalDecisionRepository, approvalCommentRepository);
        DelegateTaskAction delegateTaskAction = new DelegateTaskAction(approvalTaskRepository);
        AcceptDelegationAction acceptDelegationAction = new AcceptDelegationAction(approvalTaskRepository);
        ExpireTaskAction expireTaskAction = new ExpireTaskAction(approvalTaskRepository);
        CancelTaskAction cancelTaskAction = new CancelTaskAction(approvalTaskRepository);

        transitions
            .withExternal()
                .source(TaskStatus.PENDING)
                .target(TaskStatus.APPROVED)
                .event(ApprovalTaskEvent.APPROVE)
                .guard(taskPendingGuard)
                .action(recordApprovalDecisionAction)
            .and()
            .withExternal()
                .source(TaskStatus.PENDING)
                .target(TaskStatus.REJECTED)
                .event(ApprovalTaskEvent.REJECT)
                .guard(taskPendingGuard)
                .action(recordApprovalDecisionAction)
            .and()
            .withExternal()
                .source(TaskStatus.PENDING)
                .target(TaskStatus.DELEGATED)
                .event(ApprovalTaskEvent.DELEGATE)
                .guard(taskPendingGuard)
                .action(delegateTaskAction)
            .and()
            .withExternal()
                .source(TaskStatus.PENDING)
                .target(TaskStatus.EXPIRED)
                .event(ApprovalTaskEvent.SLA_BREACH)
                .action(expireTaskAction)
            .and()
            .withExternal()
                .source(TaskStatus.PENDING)
                .target(TaskStatus.CANCELLED)
                .event(ApprovalTaskEvent.WORKFLOW_CANCELLED)
                .action(cancelTaskAction)
            .and()
            .withExternal()
                .source(TaskStatus.DELEGATED)
                .target(TaskStatus.PENDING)
                .event(ApprovalTaskEvent.ACCEPT)
                .guard(delegateAcceptGuard)
                .action(acceptDelegationAction);
    }
}
