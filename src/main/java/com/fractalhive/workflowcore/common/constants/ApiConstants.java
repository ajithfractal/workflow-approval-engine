package com.fractalhive.workflowcore.common.constants;

/**
 * Centralized constants for API documentation (Swagger/OpenAPI).
 * Contains summaries, descriptions, response codes, and parameter descriptions
 * to keep controllers clean and maintainable.
 */
public final class ApiConstants {

    private ApiConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * Common descriptions used across multiple endpoints.
     */
    public static final class Common {
        private Common() {}

        public static final String JWT_USERNAME_EXTRACTED = "Username is extracted from JWT token.";
        public static final String INVALID_REQUEST = "Invalid request";
        public static final String NOT_FOUND = "Not found";
        public static final String SUCCESS = "Success";
    }

    /**
     * HTTP Response code descriptions.
     */
    public static final class ResponseCodes {
        private ResponseCodes() {}

        // Success codes
        public static final String OK_200 = "200";
        public static final String CREATED_201 = "201";
        public static final String NO_CONTENT_204 = "204";

        // Client error codes
        public static final String BAD_REQUEST_400 = "400";
        public static final String FORBIDDEN_403 = "403";
        public static final String NOT_FOUND_404 = "404";

        // Server error codes
        public static final String INTERNAL_SERVER_ERROR_500 = "500";

        // Descriptions
        public static final String SUCCESSFULLY_RETRIEVED = "Successfully retrieved";
        public static final String CREATED_SUCCESSFULLY = "Created successfully";
        public static final String UPDATED_SUCCESSFULLY = "Updated successfully";
        public static final String DELETED_SUCCESSFULLY = "Deleted successfully";
        public static final String FOUND = "Found";
        public static final String INVALID_REQUEST_OR_DUPLICATE = "Invalid request or duplicate";
        public static final String INVALID_REQUEST_PARAMETERS = "Invalid request parameters";
        public static final String CANNOT_PERFORM_ACTION = "Cannot perform action";
        public static final String SCHEMA_NOT_FOUND = "Schema not found or invalid";
        public static final String ERROR_CALLING_EXTERNAL_API = "Error calling external API";
    }

    /**
     * Constants for Application Registration Controller.
     */
    public static final class ApplicationRegistration {
        private ApplicationRegistration() {}

        // Summaries
        public static final String SUMMARY_REGISTER = "Register new application";
        public static final String SUMMARY_LIST_ALL = "List all applications";
        public static final String SUMMARY_LIST_ACTIVE = "List active applications";
        public static final String SUMMARY_GET_BY_ID = "Get application by ID";
        public static final String SUMMARY_UPDATE = "Update application";
        public static final String SUMMARY_DEACTIVATE = "Deactivate application";

        // Descriptions
        public static final String DESC_REGISTER = "Registers a new application and creates a dedicated schema for data isolation. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_LIST_ALL = "Retrieves all registered applications";
        public static final String DESC_LIST_ACTIVE = "Retrieves all active registered applications";
        public static final String DESC_GET_BY_ID = "Retrieves detailed information about a specific application";
        public static final String DESC_UPDATE = "Updates an existing application's configuration. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_DEACTIVATE = "Deactivates an application (does not delete schema or data). " + Common.JWT_USERNAME_EXTRACTED;

        // Response descriptions
        public static final String RESP_REGISTERED_SUCCESS = "Application registered successfully";
        public static final String RESP_UPDATED_SUCCESS = "Application updated successfully";
        public static final String RESP_DEACTIVATED_SUCCESS = "Application deactivated successfully";
        public static final String RESP_RETRIEVED_APPLICATIONS = "Successfully retrieved applications";
        public static final String RESP_RETRIEVED_ACTIVE_APPLICATIONS = "Successfully retrieved active applications";
        public static final String RESP_APPLICATION_FOUND = "Application found";
        public static final String RESP_INVALID_OR_DUPLICATE = "Invalid request or duplicate application code";

        // Parameter descriptions
        public static final String PARAM_APP_ID = "The application ID";
        public static final String PARAM_REGISTRATION_REQUEST = "Application registration request";
        public static final String PARAM_UPDATE_REQUEST = "Application update request";
    }

    /**
     * Constants for Workflow Definition Controller.
     */
    public static final class WorkflowDefinition {
        private WorkflowDefinition() {}

        // Summaries
        public static final String SUMMARY_LIST_ALL = "List all workflow definitions";
        public static final String SUMMARY_CREATE = "Create workflow definition";
        public static final String SUMMARY_GET_BY_ID = "Get workflow definition by ID";
        public static final String SUMMARY_UPDATE = "Update workflow definition";
        public static final String SUMMARY_DELETE = "Delete workflow definition";
        public static final String SUMMARY_CREATE_STAGE = "Create stage for workflow";
        public static final String SUMMARY_UPDATE_STAGE = "Update stage definition";
        public static final String SUMMARY_DELETE_STAGE = "Delete stage definition";
        public static final String SUMMARY_ADD_STEP = "Add step to stage";
        public static final String SUMMARY_UPDATE_STEP = "Update step definition";
        public static final String SUMMARY_DELETE_STEP = "Delete step definition";
        public static final String SUMMARY_ADD_APPROVERS = "Add approvers to step";
        public static final String SUMMARY_REMOVE_APPROVER = "Remove approver from step";
        public static final String SUMMARY_ACTIVATE = "Activate workflow version";
        public static final String SUMMARY_DEACTIVATE = "Deactivate workflow version";

        // Descriptions
        public static final String DESC_LIST_ALL = "Retrieves all workflow definitions in the system";
        public static final String DESC_CREATE = "Creates a new workflow definition with a name and version. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_GET_BY_ID = "Retrieves detailed information about a specific workflow definition including steps and approvers";
        public static final String DESC_UPDATE = "Updates a workflow definition. If workflow instances exist, creates a new version instead of updating the existing one. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_DELETE = "Deletes a workflow definition. Note: This will fail if workflow instances exist";
        public static final String DESC_CREATE_STAGE = "Creates a new stage for a workflow definition. Optionally includes steps during creation. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_UPDATE_STAGE = "Updates an existing stage definition. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_DELETE_STAGE = "Deletes a stage definition and all its steps";
        public static final String DESC_ADD_STEP = "Adds a new step to a stage definition. Optionally includes approvers during creation. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_UPDATE_STEP = "Updates an existing step definition. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_DELETE_STEP = "Deletes a step definition from a workflow";
        public static final String DESC_ADD_APPROVERS = "Adds one or more approvers to a workflow step. Validates N_OF_M rule constraints. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_REMOVE_APPROVER = "Removes an approver from a workflow step. Validates N_OF_M rule constraints after removal";
        public static final String DESC_ACTIVATE = "Activates a workflow version, making it available for use in new workflow instances. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_DEACTIVATE = "Deactivates a workflow version, preventing it from being used in new workflow instances. " + Common.JWT_USERNAME_EXTRACTED;

        // Response descriptions
        public static final String RESP_CREATED_SUCCESS = "Workflow created successfully";
        public static final String RESP_UPDATED_SUCCESS = "Workflow updated successfully";
        public static final String RESP_VERSION_CREATED = "New workflow version created (instances exist for previous version)";
        public static final String RESP_DELETED_SUCCESS = "Workflow deleted successfully";
        public static final String RESP_RETRIEVED_WORKFLOWS = "Successfully retrieved workflows";
        public static final String RESP_WORKFLOW_FOUND = "Workflow found";
        public static final String RESP_STAGE_CREATED = "Stage created successfully";
        public static final String RESP_STAGE_UPDATED = "Stage updated successfully";
        public static final String RESP_STAGE_DELETED = "Stage deleted successfully";
        public static final String RESP_STEP_CREATED = "Step created successfully";
        public static final String RESP_STEP_UPDATED = "Step updated successfully";
        public static final String RESP_STEP_DELETED = "Step deleted successfully";
        public static final String RESP_APPROVERS_ADDED = "Approvers added successfully";
        public static final String RESP_APPROVER_REMOVED = "Approver removed successfully";
        public static final String RESP_WORKFLOW_ACTIVATED = "Workflow activated successfully";
        public static final String RESP_WORKFLOW_DEACTIVATED = "Workflow deactivated successfully";
        public static final String RESP_INVALID_OR_DUPLICATE = "Invalid request or duplicate workflow name+version";
        public static final String RESP_CANNOT_DELETE_WITH_INSTANCES = "Cannot delete workflow with existing instances";
        public static final String RESP_N_OF_M_VALIDATION_FAILED = "Invalid request or N_OF_M validation failed";
        public static final String RESP_CANNOT_REMOVE_APPROVER = "Cannot remove approver (N_OF_M validation would fail)";

        // Parameter descriptions
        public static final String PARAM_WORKFLOW_ID = "The workflow ID";
        public static final String PARAM_STAGE_ID = "The stage ID";
        public static final String PARAM_STEP_ID = "The step ID";
        public static final String PARAM_APPROVER_ID = "The approver ID";
        public static final String PARAM_CREATE_REQUEST = "Workflow definition creation request";
        public static final String PARAM_UPDATE_REQUEST = "Workflow update request";
        public static final String PARAM_STAGE_REQUEST = "Stage definition request with optional steps";
        public static final String PARAM_STEP_REQUEST = "Step definition request with optional approvers";
        public static final String PARAM_APPROVER_REQUEST = "List of approver requests";
    }

    /**
     * Constants for Work Item Controller.
     */
    public static final class WorkItem {
        private WorkItem() {}

        // Summaries
        public static final String SUMMARY_LIST = "List work items";
        public static final String SUMMARY_CREATE = "Create a new work item";
        public static final String SUMMARY_GET_BY_ID = "Get work item by ID";
        public static final String SUMMARY_SUBMIT = "Submit work item for approval";
        public static final String SUMMARY_SUBMIT_CONVENIENCE = "Submit or create and submit work item";
        public static final String SUMMARY_GET_VERSIONS = "Get work item versions";
        public static final String SUMMARY_GET_PROGRESS = "Get workflow progress";
        public static final String SUMMARY_ARCHIVE = "Archive work item";
        public static final String SUMMARY_GET_BY_WORKFLOW = "Get work items by workflow definition ID";

        // Descriptions
        public static final String DESC_LIST = "Retrieves all work items, optionally filtered by status and/or type. Results are ordered by creation date (newest first)";
        public static final String DESC_CREATE = "Creates a new work item that can be submitted for approval workflow. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_GET_BY_ID = "Retrieves detailed information about a specific work item";
        public static final String DESC_SUBMIT = "Submits an existing work item to start the approval workflow process. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_SUBMIT_CONVENIENCE = "Submits a work item for approval. If workItemId is provided, submits existing work item. " +
                "If workItemId is not provided but type is provided, creates a new work item and submits it.";
        public static final String DESC_GET_VERSIONS = "Retrieves all versions of a work item";
        public static final String DESC_GET_PROGRESS = "Retrieves detailed workflow progress information including current step, completed steps, and overall progress";
        public static final String DESC_ARCHIVE = "Manually archives a work item, transitioning it from APPROVED or REJECTED status to ARCHIVED. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_GET_BY_WORKFLOW = "Retrieves all work items that have workflow instances using the specified workflow definition";

        // Response descriptions
        public static final String RESP_RETRIEVED_WORK_ITEMS = "Successfully retrieved work items";
        public static final String RESP_CREATED_SUCCESS = "Work item created successfully";
        public static final String RESP_FOUND = "Work item found";
        public static final String RESP_SUBMITTED_SUCCESS = "Work item submitted successfully";
        public static final String RESP_RETRIEVED_VERSIONS = "Successfully retrieved versions";
        public static final String RESP_RETRIEVED_PROGRESS = "Successfully retrieved progress";
        public static final String RESP_ARCHIVED_SUCCESS = "Work item archived successfully";
        public static final String RESP_CANNOT_SUBMIT = "Invalid request or work item cannot be submitted";
        public static final String RESP_CANNOT_ARCHIVE = "Invalid request or work item cannot be archived";

        // Parameter descriptions
        public static final String PARAM_WORK_ITEM_ID = "The work item ID";
        public static final String PARAM_WORKFLOW_DEFINITION_ID = "The workflow definition ID";
        public static final String PARAM_STATUS_FILTER = "Optional status filter";
        public static final String PARAM_TYPE_FILTER = "Optional type filter";
        public static final String PARAM_CREATE_REQUEST = "Work item creation request";
        public static final String PARAM_SUBMIT_REQUEST = "Submission request with content reference and variables";
        public static final String PARAM_SUBMIT_CONVENIENCE_REQUEST = "Submission request with optional workItemId, type, contentRef, and variables";
    }

    /**
     * Constants for Task Controller.
     */
    public static final class Task {
        private Task() {}

        // Summaries
        public static final String SUMMARY_LIST = "List tasks for an approver";
        public static final String SUMMARY_GET_BY_ID = "Get task by ID";
        public static final String SUMMARY_APPROVE_OR_REJECT = "Approve or reject a task";
        public static final String SUMMARY_APPROVE = "Approve a task";
        public static final String SUMMARY_REJECT = "Reject a task";
        public static final String SUMMARY_ADD_COMMENT = "Add comment to a task";
        public static final String SUMMARY_DELEGATE = "Delegate a task";
        public static final String SUMMARY_REASSIGN = "Reassign a task";
        public static final String SUMMARY_SEARCH = "Search tasks with filters";

        // Descriptions
        public static final String DESC_LIST = "Retrieves all tasks assigned to a specific approver, optionally filtered by status";
        public static final String DESC_GET_BY_ID = "Retrieves detailed information about a specific approval task";
        public static final String DESC_APPROVE_OR_REJECT = "Records an approval decision for a task and advances the workflow if step completion criteria is met";
        public static final String DESC_APPROVE = "Records an approval decision for a task and advances the workflow if step completion criteria is met";
        public static final String DESC_REJECT = "Records a rejection decision for a task and fails the workflow if step completion criteria requires all approvals";
        public static final String DESC_ADD_COMMENT = "Adds a comment to an approval task";
        public static final String DESC_DELEGATE = "Delegates a task to another approver. This is a task-level operation and does not advance the workflow";
        public static final String DESC_REASSIGN = "Reassigns a task to a new approver";
        public static final String DESC_SEARCH = "Searches approval tasks with advanced filters, pagination, and sorting. Supports filtering by approverId, status, search text, and time ranges on dueAt or createdAt fields.";

        // Response descriptions
        public static final String RESP_RETRIEVED_TASKS = "Successfully retrieved tasks";
        public static final String RESP_FOUND = "Task found";
        public static final String RESP_APPROVED_SUCCESS = "Task approved successfully";
        public static final String RESP_REJECTED_SUCCESS = "Task rejected successfully";
        public static final String RESP_COMMENT_CREATED = "Comment created successfully";
        public static final String RESP_DELEGATED_SUCCESS = "Task delegated successfully";
        public static final String RESP_REASSIGNED_SUCCESS = "Task reassigned successfully";
        public static final String RESP_CANNOT_APPROVE = "Invalid request or task cannot be approved";
        public static final String RESP_CANNOT_REJECT = "Invalid request or task cannot be rejected";
        public static final String RESP_CANNOT_DELEGATE = "Invalid request or task cannot be delegated";

        // Parameter descriptions
        public static final String PARAM_TASK_ID = "The task ID";
        public static final String PARAM_APPROVER_ID = "The approver user ID";
        public static final String PARAM_STATUS_FILTER = "Optional task status filter";
        public static final String PARAM_APPROVE_REQUEST = "Approval request with optional comments";
        public static final String PARAM_REJECT_REQUEST = "Rejection request with optional comments";
        public static final String PARAM_COMMENT_REQUEST = "Comment request";
        public static final String PARAM_DELEGATE_REQUEST = "Delegation request with target user ID and optional reason";
        public static final String PARAM_REASSIGN_REQUEST = "Reassignment request";
        public static final String PARAM_SEARCH_REQUEST = "Search request with filter criteria";
        public static final String PARAM_PAGE = "Page number (0-indexed)";
        public static final String PARAM_SIZE = "Page size";
        public static final String PARAM_SORT_BY = "Field name to sort by";
        public static final String PARAM_SORT_DIRECTION = "Sort direction (ASC or DESC)";
    }

    /**
     * Constants for Workflow Step Rule Controller.
     */
    public static final class WorkflowStepRule {
        private WorkflowStepRule() {}

        // Summaries
        public static final String SUMMARY_CREATE = "Create a rule for a workflow step";
        public static final String SUMMARY_GET_ALL = "Get all rules for a workflow step";
        public static final String SUMMARY_GET_BY_ID = "Get a rule by ID";
        public static final String SUMMARY_UPDATE = "Update a rule";
        public static final String SUMMARY_DELETE = "Delete a rule";

        // Descriptions
        public static final String DESC_CREATE = "Creates a new rule (auto-approve, skip step, route approver, etc.) for a workflow step. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_GET_ALL = "Retrieves all rules (active and inactive) for a specific workflow step";
        public static final String DESC_GET_BY_ID = "Retrieves a specific rule by its ID";
        public static final String DESC_UPDATE = "Updates an existing rule. Note: stepDefinitionId cannot be changed. " + Common.JWT_USERNAME_EXTRACTED;
        public static final String DESC_DELETE = "Deletes a rule from a workflow step";

        // Response descriptions
        public static final String RESP_CREATED_SUCCESS = "Rule created successfully";
        public static final String RESP_RETRIEVED_RULES = "Successfully retrieved rules";
        public static final String RESP_RETRIEVED_RULE = "Successfully retrieved rule";
        public static final String RESP_UPDATED_SUCCESS = "Rule updated successfully";
        public static final String RESP_DELETED_SUCCESS = "Rule deleted successfully";

        // Parameter descriptions
        public static final String PARAM_STEP_DEFINITION_ID = "The step definition ID";
        public static final String PARAM_RULE_ID = "The rule ID";
        public static final String PARAM_CREATE_REQUEST = "Rule creation request";
        public static final String PARAM_UPDATE_REQUEST = "Rule update request";
    }

    /**
     * Constants for User Controller.
     */
    public static final class User {
        private User() {}

        // Summaries
        public static final String SUMMARY_LIST = "List users";

        // Descriptions
        public static final String DESC_LIST = "Fetches users from external application API based on current tenant schema";

        // Response descriptions
        public static final String RESP_RETRIEVED_USERS = "Successfully retrieved users";
        public static final String RESP_SCHEMA_NOT_FOUND = ResponseCodes.SCHEMA_NOT_FOUND;
        public static final String RESP_EXTERNAL_API_ERROR = ResponseCodes.ERROR_CALLING_EXTERNAL_API;

        // Parameter descriptions
        public static final String PARAM_SEARCH = "Search query (name or email)";
        public static final String PARAM_ROLE = "Role filter";
        public static final String PARAM_LIMIT = "Maximum number of results";
    }
}
