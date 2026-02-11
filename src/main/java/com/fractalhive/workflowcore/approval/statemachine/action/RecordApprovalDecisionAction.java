package com.fractalhive.workflowcore.approval.statemachine.action;

import com.fractalhive.workflowcore.approval.entity.ApprovalDecision;
import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApprovalTaskEvent;
import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.approval.repository.ApprovalDecisionRepository;
import com.fractalhive.workflowcore.approval.repository.ApprovalTaskRepository;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Action that records an approval decision and updates the task status.
 */
public class RecordApprovalDecisionAction implements Action<TaskStatus, ApprovalTaskEvent> {

    private static final String USER_ID_HEADER = "userId";
    private static final String COMMENTS_HEADER = "comments";
    private static final String DECISION_TYPE_HEADER = "decisionType";

    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalDecisionRepository approvalDecisionRepository;

    public RecordApprovalDecisionAction(ApprovalTaskRepository approvalTaskRepository,
                                        ApprovalDecisionRepository approvalDecisionRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalDecisionRepository = approvalDecisionRepository;
    }

    @Override
    public void execute(StateContext<TaskStatus, ApprovalTaskEvent> context) {
        ApprovalTask task = context.getExtendedState().get("task", ApprovalTask.class);
        if (task == null) {
            return;
        }

        String userId = (String) context.getMessageHeaders().get(USER_ID_HEADER);
        String comments = (String) context.getMessageHeaders().get(COMMENTS_HEADER);
        DecisionType decisionType = (DecisionType) context.getMessageHeaders().get(DECISION_TYPE_HEADER);

        if (userId == null || decisionType == null) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());

        // Create approval decision record
        ApprovalDecision decision = new ApprovalDecision();
        decision.setApprovalTaskId(task.getId());
        decision.setDecision(decisionType);
        decision.setComments(comments);
        decision.setDecidedBy(userId);
        decision.setDecidedAt(now);
        decision.setCreatedAt(now);
        decision.setCreatedBy(userId);
        approvalDecisionRepository.save(decision);

        // Update task
        task.setActedAt(now);
        task.setStatus(decisionType == DecisionType.APPROVED ? TaskStatus.APPROVED : TaskStatus.REJECTED);
        approvalTaskRepository.save(task);
    }
}
