package com.fractalhive.workflowcore.workflow.statemachine.config;

import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.action.*;
import com.fractalhive.workflowcore.workflow.statemachine.enums.WorkflowInstanceEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.Set;

/**
 * Configuration for the Workflow Instance State Machine.
 * Defines states, transitions, and actions for workflow instance lifecycle.
 */
@Configuration
@EnableStateMachineFactory(name = "workflowInstanceStateMachineFactory")
public class WorkflowInstanceStateMachineConfig extends StateMachineConfigurerAdapter<WorkflowStatus, WorkflowInstanceEvent> {

    private final WorkflowInstanceRepository workflowInstanceRepository;

    public WorkflowInstanceStateMachineConfig(WorkflowInstanceRepository workflowInstanceRepository) {
        this.workflowInstanceRepository = workflowInstanceRepository;
    }

    @Override
    public void configure(StateMachineStateConfigurer<WorkflowStatus, WorkflowInstanceEvent> states) throws Exception {
        states
            .withStates()
            .initial(WorkflowStatus.NOT_STARTED)
            .states(Set.of(WorkflowStatus.values()));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<WorkflowStatus, WorkflowInstanceEvent> transitions) throws Exception {
        StartWorkflowAction startAction = new StartWorkflowAction(workflowInstanceRepository);
        CompleteWorkflowAction completeAction = new CompleteWorkflowAction(workflowInstanceRepository);
        FailWorkflowAction failAction = new FailWorkflowAction(workflowInstanceRepository);
        CancelWorkflowAction cancelAction = new CancelWorkflowAction(workflowInstanceRepository);

        transitions
            .withExternal()
                .source(WorkflowStatus.NOT_STARTED)
                .target(WorkflowStatus.IN_PROGRESS)
                .event(WorkflowInstanceEvent.START)
                .action(startAction)
            .and()
            .withExternal()
                .source(WorkflowStatus.NOT_STARTED)
                .target(WorkflowStatus.CANCELLED)
                .event(WorkflowInstanceEvent.CANCEL)
                .action(cancelAction)
            .and()
            .withExternal()
                .source(WorkflowStatus.IN_PROGRESS)
                .target(WorkflowStatus.COMPLETED)
                .event(WorkflowInstanceEvent.COMPLETE)
                .action(completeAction)
            .and()
            .withExternal()
                .source(WorkflowStatus.IN_PROGRESS)
                .target(WorkflowStatus.FAILED)
                .event(WorkflowInstanceEvent.FAIL)
                .action(failAction)
            .and()
            .withExternal()
                .source(WorkflowStatus.IN_PROGRESS)
                .target(WorkflowStatus.CANCELLED)
                .event(WorkflowInstanceEvent.CANCEL)
                .action(cancelAction);
    }
}
