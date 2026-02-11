package com.fractalhive.workflowcore.approval.statemachine.guard;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

/**
 * Guard that validates the user accepting a delegation matches the designated delegate.
 * Prevents unauthorized users from accepting a delegation.
 */
public class DelegateAcceptGuard implements Guard<TaskStatus, ApprovalTaskEvent> {

    private static final String USER_ID_HEADER = "userId";

    @Override
    public boolean evaluate(StateContext<TaskStatus, ApprovalTaskEvent> context) {
        ApprovalTask task = context.getExtendedState().get("task", ApprovalTask.class);
        String userId = (String) context.getMessageHeaders().get(USER_ID_HEADER);
        
        if (task == null || userId == null) {
            return false;
        }
        
        // Validate that the accepting user matches the current approver (delegate)
        return userId.equals(task.getApproverId());
    }
}
