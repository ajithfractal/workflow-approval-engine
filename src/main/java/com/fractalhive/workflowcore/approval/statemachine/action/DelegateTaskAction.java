package com.fractalhive.workflowcore.approval.statemachine.action;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * Action that delegates a task to another user.
 */
public class DelegateTaskAction implements Action<TaskStatus, ApprovalTaskEvent> {

    private static final String TO_USER_ID_HEADER = "toUserId";

    private final ApprovalTaskRepository approvalTaskRepository;

    public DelegateTaskAction(ApprovalTaskRepository approvalTaskRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
    }

    @Override
    public void execute(StateContext<TaskStatus, ApprovalTaskEvent> context) {
        ApprovalTask task = context.getExtendedState().get("task", ApprovalTask.class);
        if (task == null) {
            return;
        }

        String toUserId = (String) context.getMessageHeaders().get(TO_USER_ID_HEADER);
        if (toUserId == null) {
            return;
        }

        // Update approver to the delegate
        task.setApproverId(toUserId);
        task.setStatus(TaskStatus.DELEGATED);
        approvalTaskRepository.save(task);
    }
}
