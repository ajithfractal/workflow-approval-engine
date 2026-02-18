package com.fractalhive.workflowcore.workflow.statemachine.config;

import com.fractalhive.workflowcore.workflow.enums.StageStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStageInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.action.CompleteStageAction;
import com.fractalhive.workflowcore.workflow.statemachine.action.FailStageAction;
import com.fractalhive.workflowcore.workflow.statemachine.action.StartStageAction;
import com.fractalhive.workflowcore.workflow.statemachine.enums.StageEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.Set;

/**
 * Configuration for the Workflow Stage Instance State Machine.
 * Defines states, transitions, and actions for workflow stage instance lifecycle.
 */
@Configuration
@EnableStateMachineFactory(name = "workflowStageInstanceStateMachineFactory")
public class WorkflowStageInstanceStateMachineConfig extends StateMachineConfigurerAdapter<StageStatus, StageEvent> {

    private final WorkflowStageInstanceRepository workflowStageInstanceRepository;

    public WorkflowStageInstanceStateMachineConfig(WorkflowStageInstanceRepository workflowStageInstanceRepository) {
        this.workflowStageInstanceRepository = workflowStageInstanceRepository;
    }

    @Override
    public void configure(StateMachineStateConfigurer<StageStatus, StageEvent> states) throws Exception {
        states
            .withStates()
            .initial(StageStatus.NOT_STARTED)
            .states(Set.of(StageStatus.values()));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<StageStatus, StageEvent> transitions) throws Exception {
        StartStageAction startAction = new StartStageAction(workflowStageInstanceRepository);
        CompleteStageAction completeAction = new CompleteStageAction(workflowStageInstanceRepository);
        FailStageAction failAction = new FailStageAction(workflowStageInstanceRepository);

        transitions
            .withExternal()
                .source(StageStatus.NOT_STARTED)
                .target(StageStatus.IN_PROGRESS)
                .event(StageEvent.START)
                .action(startAction)
            .and()
            .withExternal()
                .source(StageStatus.IN_PROGRESS)
                .target(StageStatus.COMPLETED)
                .event(StageEvent.COMPLETE)
                .action(completeAction)
            .and()
            .withExternal()
                .source(StageStatus.IN_PROGRESS)
                .target(StageStatus.FAILED)
                .event(StageEvent.FAIL)
                .action(failAction);
    }
}
