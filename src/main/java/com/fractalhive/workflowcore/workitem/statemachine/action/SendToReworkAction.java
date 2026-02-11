package com.fractalhive.workflowcore.workitem.statemachine.action;

import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import com.fractalhive.workflowcore.workitem.statemachine.enums.WorkItemEvent;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * Action to send work item to rework - updates status to REWORK and increments version.
 */
public class SendToReworkAction implements Action<WorkItemStatus, WorkItemEvent> {

    private final WorkItemRepository workItemRepository;

    public SendToReworkAction(WorkItemRepository workItemRepository) {
        this.workItemRepository = workItemRepository;
    }

    @Override
    public void execute(StateContext<WorkItemStatus, WorkItemEvent> context) {
        WorkItem workItem = context.getExtendedState().get("workItem", WorkItem.class);
        if (workItem == null) {
            return;
        }

        // Increment version for rework
        workItem.setCurrentVersion(workItem.getCurrentVersion() + 1);
        workItem.setStatus(WorkItemStatus.REWORK);
        workItemRepository.save(workItem);
    }
}
