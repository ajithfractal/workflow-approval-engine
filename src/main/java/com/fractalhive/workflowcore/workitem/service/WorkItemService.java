package com.fractalhive.workflowcore.workitem.service;

import com.fractalhive.workflowcore.workitem.dto.WorkItemCreateRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemResponse;
import com.fractalhive.workflowcore.workitem.dto.WorkItemSubmitRequest;
import com.fractalhive.workflowcore.workitem.dto.WorkItemVersionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing work items.
 */
public interface WorkItemService {

    /**
     * Creates a new work item in DRAFT status.
     *
     * @param request   the work item creation request
     * @param createdBy the user creating the work item
     * @return the ID of the created work item
     */
    UUID createWorkItem(WorkItemCreateRequest request, String createdBy);

    /**
     * Submits a work item using state machine (DRAFT → SUBMITTED).
     * Creates a new version with the provided content reference.
     *
     * @param workItemId  the work item ID
     * @param request     the submission request
     * @param submittedBy the user submitting the work item
     * @return the ID of the created version
     */
    UUID submitWorkItem(UUID workItemId, WorkItemSubmitRequest request, String submittedBy);

    /**
     * Creates a new version for rework.
     *
     * @param workItemId the work item ID
     * @param contentRef the content reference for the new version
     * @param createdBy  the user creating the version
     * @return the ID of the created version
     */
    UUID createVersion(UUID workItemId, String contentRef, String createdBy);

    /**
     * Retrieves a work item with latest version.
     *
     * @param workItemId the work item ID
     * @return the work item response
     */
    WorkItemResponse getWorkItem(UUID workItemId);

    /**
     * Retrieves all versions for a work item.
     *
     * @param workItemId the work item ID
     * @return list of work item versions
     */
    List<WorkItemVersionResponse> getVersions(UUID workItemId);
}
