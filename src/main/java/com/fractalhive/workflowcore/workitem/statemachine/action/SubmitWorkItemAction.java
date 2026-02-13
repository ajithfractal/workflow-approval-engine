package com.fractalhive.workflowcore.workitem.statemachine.action;

import com.fractalhive.workflowcore.workitem.entity.WorkItem;
import com.fractalhive.workflowcore.workitem.entity.WorkItemVersion;
import com.fractalhive.workflowcore.workitem.enums.WorkItemStatus;
import com.fractalhive.workflowcore.workitem.repository.WorkItemRepository;
import com.fractalhive.workflowcore.workitem.repository.WorkItemVersionRepository;
import com.fractalhive.workflowcore.workitem.statemachine.enums.WorkItemEvent;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Action to submit a work item - creates a version and updates status to SUBMITTED.
 */
public class SubmitWorkItemAction implements Action<WorkItemStatus, WorkItemEvent> {

    private static final String CONTENT_REF_HEADER = "contentRef";
    private static final String SUBMITTED_BY_HEADER = "submittedBy";

    private final WorkItemRepository workItemRepository;
    private final WorkItemVersionRepository workItemVersionRepository;

    public SubmitWorkItemAction(WorkItemRepository workItemRepository,
                                WorkItemVersionRepository workItemVersionRepository) {
        this.workItemRepository = workItemRepository;
        this.workItemVersionRepository = workItemVersionRepository;
    }

    @Override
    public void execute(StateContext<WorkItemStatus, WorkItemEvent> context) {
        WorkItem workItem = context.getExtendedState().get("workItem", WorkItem.class);
        if (workItem == null) {
            return;
        }

        String contentRef = (String) context.getMessageHeaders().get(CONTENT_REF_HEADER);
        String submittedBy = (String) context.getMessageHeaders().get(SUBMITTED_BY_HEADER);

        if (contentRef == null || submittedBy == null) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());

        // Create new version
        WorkItemVersion version = new WorkItemVersion();
        version.setWorkItemId(workItem.getId());
        version.setVersion(workItem.getCurrentVersion());
        version.setContentRef(contentRef);
        version.setSubmittedBy(submittedBy);
        version.setSubmittedAt(now);
        version.setCreatedAt(now);
        version.setCreatedBy(submittedBy);
        workItemVersionRepository.save(version);
        workItem.setStatus(WorkItemStatus.SUBMITTED);
        workItemRepository.save(workItem);
    }
}
