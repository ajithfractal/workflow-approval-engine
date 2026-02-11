package com.fractalhive.workflowcore.workitem.statemachine.guard;

import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workitem.statemachine.enums.WorkItemEvent;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

/**
 * Guard to ensure a work item is in REWORK status before allowing SUBMIT transition.
 */
public class WorkItemReworkGuard implements Guard<WorkItemStatus, WorkItemEvent> {

    @Override
    public boolean evaluate(StateContext<WorkItemStatus, WorkItemEvent> context) {
        WorkItem workItem = context.getExtendedState().get("workItem", WorkItem.class);
        if (workItem == null) {
            return false;
        }
        return workItem.getStatus() == WorkItemStatus.REWORK;
    }
}
