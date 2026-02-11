package com.fractalhive.workflowcore.approval.statemachine.guard;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

/**
 * Guard that ensures the approval task is in PENDING state before allowing transitions.
 * Prevents double-approve, double-reject, or acting on already-decided tasks.
 */
public class TaskPendingGuard implements Guard<TaskStatus, ApprovalTaskEvent> {

    @Override
    public boolean evaluate(StateContext<TaskStatus, ApprovalTaskEvent> context) {
        ApprovalTask task = context.getExtendedState().get("task", ApprovalTask.class);
        if (task == null) {
            return false;
        }
        return task.getStatus() == TaskStatus.PENDING;
    }
}
