package com.fractalhive.workflowcore.approval.statemachine.action;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * Action that accepts a delegated task, resetting it to PENDING.
 */
public class AcceptDelegationAction implements Action<TaskStatus, ApprovalTaskEvent> {

    private final ApprovalTaskRepository approvalTaskRepository;

    public AcceptDelegationAction(ApprovalTaskRepository approvalTaskRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
    }

    @Override
    public void execute(StateContext<TaskStatus, ApprovalTaskEvent> context) {
        ApprovalTask task = context.getExtendedState().get("task", ApprovalTask.class);
        if (task == null) {
            return;
        }

        // Reset status to PENDING (approverId already set during delegation)
        task.setStatus(TaskStatus.PENDING);
        approvalTaskRepository.save(task);
    }
}
