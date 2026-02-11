package com.fractalhive.workflowcore.workitem.statemachine.action;

import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import com.fractalhive.workflowcore.workitem.statemachine.enums.WorkItemEvent;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * Action to start review - updates status to IN_REVIEW.
 */
public class StartReviewAction implements Action<WorkItemStatus, WorkItemEvent> {

    private final WorkItemRepository workItemRepository;

    public StartReviewAction(WorkItemRepository workItemRepository) {
        this.workItemRepository = workItemRepository;
    }

    @Override
    public void execute(StateContext<WorkItemStatus, WorkItemEvent> context) {
        WorkItem workItem = context.getExtendedState().get("workItem", WorkItem.class);
        if (workItem == null) {
            return;
        }

        workItem.setStatus(WorkItemStatus.IN_REVIEW);
        workItemRepository.save(workItem);
    }
}
