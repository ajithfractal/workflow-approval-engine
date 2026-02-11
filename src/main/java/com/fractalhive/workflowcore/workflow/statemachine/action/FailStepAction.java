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
 * Action to fail a step - sets completedAt timestamp and updates status to FAILED.
 */
public class FailStepAction implements Action<StepStatus, WorkflowStepInstanceEvent> {

    private final WorkflowStepInstanceRepository workflowStepInstanceRepository;

    public FailStepAction(WorkflowStepInstanceRepository workflowStepInstanceRepository) {
        this.workflowStepInstanceRepository = workflowStepInstanceRepository;
    }

    @Override
    public void execute(StateContext<StepStatus, WorkflowStepInstanceEvent> context) {
        WorkflowStepInstance stepInstance = context.getExtendedState().get("stepInstance", WorkflowStepInstance.class);
        if (stepInstance == null) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        stepInstance.setCompletedAt(now);
        stepInstance.setStatus(StepStatus.FAILED);
        workflowStepInstanceRepository.save(stepInstance);
    }
}
