package com.fractalhive.workflowcore.workflow.statemachine.config;

import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.action.CompleteStepAction;
import com.fractalhive.workflowcore.workflow.statemachine.action.FailStepAction;
import com.fractalhive.workflowcore.workflow.statemachine.action.StartStepAction;
import com.fractalhive.workflowcore.workflow.statemachine.enums.WorkflowStepInstanceEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.Set;

/**
 * Configuration for the Workflow Step Instance State Machine.
 * Defines states, transitions, and actions for workflow step instance lifecycle.
 */
@Configuration
@EnableStateMachineFactory
public class WorkflowStepInstanceStateMachineConfig extends StateMachineConfigurerAdapter<StepStatus, WorkflowStepInstanceEvent> {

    private final WorkflowStepInstanceRepository workflowStepInstanceRepository;

    public WorkflowStepInstanceStateMachineConfig(WorkflowStepInstanceRepository workflowStepInstanceRepository) {
        this.workflowStepInstanceRepository = workflowStepInstanceRepository;
    }

    @Override
    public void configure(StateMachineStateConfigurer<StepStatus, WorkflowStepInstanceEvent> states) throws Exception {
        states
            .withStates()
            .initial(StepStatus.NOT_STARTED)
            .states(Set.of(StepStatus.values()));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<StepStatus, WorkflowStepInstanceEvent> transitions) throws Exception {
        StartStepAction startAction = new StartStepAction(workflowStepInstanceRepository);
        CompleteStepAction completeAction = new CompleteStepAction(workflowStepInstanceRepository);
        FailStepAction failAction = new FailStepAction(workflowStepInstanceRepository);

        transitions
            .withExternal()
                .source(StepStatus.NOT_STARTED)
                .target(StepStatus.IN_PROGRESS)
                .event(WorkflowStepInstanceEvent.START)
                .action(startAction)
            .and()
            .withExternal()
                .source(StepStatus.IN_PROGRESS)
                .target(StepStatus.COMPLETED)
                .event(WorkflowStepInstanceEvent.COMPLETE)
                .action(completeAction)
            .and()
            .withExternal()
                .source(StepStatus.IN_PROGRESS)
                .target(StepStatus.FAILED)
                .event(WorkflowStepInstanceEvent.FAIL)
                .action(failAction);
    }
}
