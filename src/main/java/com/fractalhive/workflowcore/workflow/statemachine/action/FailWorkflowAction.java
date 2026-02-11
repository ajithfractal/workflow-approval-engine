package com.fractalhive.workflowcore.workflow.statemachine.action;

import com.fractalhive.workflowcore.workflow.entity.WorkflowInstance;
import com.fractalhive.workflowcore.workflow.enums.WorkflowStatus;
import com.fractalhive.workflowcore.workflow.repository.WorkflowInstanceRepository;
import com.fractalhive.workflowcore.workflow.statemachine.enums.WorkflowInstanceEvent;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Action to fail a workflow - sets completedAt timestamp and updates status to FAILED.
 */
public class FailWorkflowAction implements Action<WorkflowStatus, WorkflowInstanceEvent> {

    private final WorkflowInstanceRepository workflowInstanceRepository;

    public FailWorkflowAction(WorkflowInstanceRepository workflowInstanceRepository) {
        this.workflowInstanceRepository = workflowInstanceRepository;
    }

    @Override
    public void execute(StateContext<WorkflowStatus, WorkflowInstanceEvent> context) {
        WorkflowInstance workflowInstance = context.getExtendedState().get("workflowInstance", WorkflowInstance.class);
        if (workflowInstance == null) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        workflowInstance.setCompletedAt(now);
        workflowInstance.setStatus(WorkflowStatus.FAILED);
        workflowInstanceRepository.save(workflowInstance);
    }
}
