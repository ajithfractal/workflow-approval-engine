package com.fractalhive.workflowcore.approval.statemachine.action;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Action that expires a task due to SLA breach.
 */
public class ExpireTaskAction implements Action<TaskStatus, ApprovalTaskEvent> {

    private final ApprovalTaskRepository approvalTaskRepository;

    public ExpireTaskAction(ApprovalTaskRepository approvalTaskRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
    }

    @Override
    public void execute(StateContext<TaskStatus, ApprovalTaskEvent> context) {
        ApprovalTask task = context.getExtendedState().get("task", ApprovalTask.class);
        if (task == null) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        task.setStatus(TaskStatus.EXPIRED);
        task.setActedAt(now);
        approvalTaskRepository.save(task);
    }
}
