package com.fractalhive.workflowcore.workflow.statemachine.action;

import com.fractalhive.workflowcore.workflow.entity.WorkflowStageInstance;
import com.fractalhive.workflowcore.workflow.enums.StageStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowStageInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.enums.StageEvent;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Action to complete a stage - sets completedAt timestamp and updates status to COMPLETED.
 */
public class CompleteStageAction implements Action<StageStatus, StageEvent> {

    private final WorkflowStageInstanceRepository workflowStageInstanceRepository;

    public CompleteStageAction(WorkflowStageInstanceRepository workflowStageInstanceRepository) {
        this.workflowStageInstanceRepository = workflowStageInstanceRepository;
    }

    @Override
    public void execute(StateContext<StageStatus, StageEvent> context) {
        WorkflowStageInstance stageInstance = context.getExtendedState().get("stageInstance", WorkflowStageInstance.class);
        if (stageInstance == null) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        stageInstance.setCompletedAt(now);
        stageInstance.setStatus(StageStatus.COMPLETED);
        workflowStageInstanceRepository.save(stageInstance);
    }
}
