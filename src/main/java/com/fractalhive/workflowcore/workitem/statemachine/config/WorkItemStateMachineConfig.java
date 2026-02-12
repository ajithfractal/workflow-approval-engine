package com.fractalhive.workflowcore.workitem.statemachine.config;

import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import com.fractalhive.workflowcore.workitem.repository.WorkItemVersionRepository;
import com.fractalhive.workflowcore.workitem.statemachine.action.*;
import com.fractalhive.workflowcore.workitem.statemachine.enums.WorkItemEvent;
import com.fractalhive.workflowcore.workitem.statemachine.guard.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.Set;

/**
 * Configuration for the Work Item State Machine.
 * Defines states, transitions, guards, and actions for work item lifecycle.
 */
@Configuration
@EnableStateMachineFactory(name = "workItemStateMachineFactory")
public class WorkItemStateMachineConfig extends StateMachineConfigurerAdapter<WorkItemStatus, WorkItemEvent> {

    private final WorkItemRepository workItemRepository;
    private final WorkItemVersionRepository workItemVersionRepository;

    public WorkItemStateMachineConfig(WorkItemRepository workItemRepository,
                                      WorkItemVersionRepository workItemVersionRepository) {
        this.workItemRepository = workItemRepository;
        this.workItemVersionRepository = workItemVersionRepository;
    }

    @Override
    public void configure(StateMachineStateConfigurer<WorkItemStatus, WorkItemEvent> states) throws Exception {
        states
            .withStates()
            .initial(WorkItemStatus.DRAFT)
            .states(Set.of(WorkItemStatus.values()));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<WorkItemStatus, WorkItemEvent> transitions) throws Exception {
        WorkItemDraftGuard draftGuard = new WorkItemDraftGuard();
        WorkItemSubmittedGuard submittedGuard = new WorkItemSubmittedGuard();
        WorkItemInReviewGuard inReviewGuard = new WorkItemInReviewGuard();
        WorkItemReworkGuard reworkGuard = new WorkItemReworkGuard();

        SubmitWorkItemAction submitAction = new SubmitWorkItemAction(workItemRepository, workItemVersionRepository);
        StartReviewAction startReviewAction = new StartReviewAction(workItemRepository);
        ApproveWorkItemAction approveAction = new ApproveWorkItemAction(workItemRepository);
        RejectWorkItemAction rejectAction = new RejectWorkItemAction(workItemRepository);
        SendToReworkAction sendToReworkAction = new SendToReworkAction(workItemRepository);
        ArchiveWorkItemAction archiveAction = new ArchiveWorkItemAction(workItemRepository);
        CancelWorkItemAction cancelAction = new CancelWorkItemAction(workItemRepository);

        transitions
            .withExternal()
                .source(WorkItemStatus.DRAFT)
                .target(WorkItemStatus.SUBMITTED)
                .event(WorkItemEvent.SUBMIT)
                .guard(draftGuard)
                .action(submitAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.DRAFT)
                .target(WorkItemStatus.CANCELLED)
                .event(WorkItemEvent.CANCEL)
                .action(cancelAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.SUBMITTED)
                .target(WorkItemStatus.IN_REVIEW)
                .event(WorkItemEvent.START_REVIEW)
                .guard(submittedGuard)
                .action(startReviewAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.SUBMITTED)
                .target(WorkItemStatus.CANCELLED)
                .event(WorkItemEvent.CANCEL)
                .action(cancelAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.IN_REVIEW)
                .target(WorkItemStatus.APPROVED)
                .event(WorkItemEvent.APPROVE)
                .guard(inReviewGuard)
                .action(approveAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.IN_REVIEW)
                .target(WorkItemStatus.REJECTED)
                .event(WorkItemEvent.REJECT)
                .guard(inReviewGuard)
                .action(rejectAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.IN_REVIEW)
                .target(WorkItemStatus.REWORK)
                .event(WorkItemEvent.SEND_TO_REWORK)
                .guard(inReviewGuard)
                .action(sendToReworkAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.IN_REVIEW)
                .target(WorkItemStatus.CANCELLED)
                .event(WorkItemEvent.CANCEL)
                .action(cancelAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.REWORK)
                .target(WorkItemStatus.SUBMITTED)
                .event(WorkItemEvent.SUBMIT)
                .guard(reworkGuard)
                .action(submitAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.REWORK)
                .target(WorkItemStatus.CANCELLED)
                .event(WorkItemEvent.CANCEL)
                .action(cancelAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.APPROVED)
                .target(WorkItemStatus.ARCHIVED)
                .event(WorkItemEvent.ARCHIVE)
                .action(archiveAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.APPROVED)
                .target(WorkItemStatus.CANCELLED)
                .event(WorkItemEvent.CANCEL)
                .action(cancelAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.REJECTED)
                .target(WorkItemStatus.ARCHIVED)
                .event(WorkItemEvent.ARCHIVE)
                .action(archiveAction)
            .and()
            .withExternal()
                .source(WorkItemStatus.REJECTED)
                .target(WorkItemStatus.CANCELLED)
                .event(WorkItemEvent.CANCEL)
                .action(cancelAction);
    }
}
