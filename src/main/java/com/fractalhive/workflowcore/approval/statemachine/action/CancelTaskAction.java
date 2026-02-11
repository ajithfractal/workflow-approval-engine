package com.fractalhive.workflowcore.approval.statemachine.action;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * Action that cancels a task when the parent workflow is cancelled.
 */
public class CancelTaskAction implements Action<TaskStatus, ApprovalTaskEvent> {

    private final ApprovalTaskRepository approvalTaskRepository;

    public CancelTaskAction(ApprovalTaskRepository approvalTaskRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
    }

    @Override
    public void execute(StateContext<TaskStatus, ApprovalTaskEvent> context) {
        ApprovalTask task = context.getExtendedState().get("task", ApprovalTask.class);
        if (task == null) {
            return;
        }

        task.setStatus(TaskStatus.CANCELLED);
        approvalTaskRepository.save(task);
    }
}
