package com.fractalhive.workflowcore.workflow.statemachine.action;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStepInstance;
import com.fractalhive.workflowcore.workflow.enums.StepStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStepInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.enums.WorkflowStepInstanceEvent;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Action to start a step - sets startedAt timestamp and updates status to IN_PROGRESS.
 */
public class StartStepAction implements Action<StepStatus, WorkflowStepInstanceEvent> {

    private final WorkflowStepInstanceRepository workflowStepInstanceRepository;

    public StartStepAction(WorkflowStepInstanceRepository workflowStepInstanceRepository) {
        this.workflowStepInstanceRepository = workflowStepInstanceRepository;
    }

    @Override
    public void execute(StateContext<StepStatus, WorkflowStepInstanceEvent> context) {
        WorkflowStepInstance stepInstance = context.getExtendedState().get("stepInstance", WorkflowStepInstance.class);
        if (stepInstance == null) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        stepInstance.setStartedAt(now);
        stepInstance.setStatus(StepStatus.IN_PROGRESS);
        workflowStepInstanceRepository.save(stepInstance);
    }
}
