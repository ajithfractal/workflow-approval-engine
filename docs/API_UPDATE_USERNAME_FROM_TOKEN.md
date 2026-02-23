# API Update: Username Extraction from JWT Token

## Overview

All APIs that previously required `createdBy`, `updatedBy`, `userId`, or `submittedBy` as request parameters have been updated to automatically extract the username from the JWT token's `preferred_username` claim.

**Date:** 2024  
**Version:** 1.0.0  
**Breaking Change:** Yes - Request parameters removed

---

## What Changed?

### Before
APIs required username/user ID to be passed as a request parameter:
```http
POST /api/workflow-definitions?createdBy=john.doe
Content-Type: application/json

{
  "name": "Purchase Approval",
  "version": 1
}
```

### After
Username is automatically extracted from the JWT token - **no parameter needed**:
```http
POST /api/workflow-definitions
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "Purchase Approval",
  "version": 1
}
```

The username is extracted from the `preferred_username` claim in the JWT token automatically.

---

## Affected APIs

### 1. Workflow Definition APIs

#### Create Workflow
- **Endpoint:** `POST /api/workflow-definitions`
- **Removed:** `createdBy` query parameter
- **Before:**
  ```
  POST /api/workflow-definitions?createdBy=john.doe
  ```
- **After:**
  ```
  POST /api/workflow-definitions
  Authorization: Bearer <token>
  ```

#### Update Workflow
- **Endpoint:** `PUT /api/workflow-definitions/{workflowId}`
- **Removed:** `updatedBy` query parameter
- **Before:**
  ```
  PUT /api/workflow-definitions/{workflowId}?updatedBy=john.doe
  ```
- **After:**
  ```
  PUT /api/workflow-definitions/{workflowId}
  Authorization: Bearer <token>
  ```

#### Create Stage
- **Endpoint:** `POST /api/workflow-definitions/{workflowId}/stages`
- **Removed:** `createdBy` query parameter
- **Before:**
  ```
  POST /api/workflow-definitions/{workflowId}/stages?createdBy=john.doe
  ```
- **After:**
  ```
  POST /api/workflow-definitions/{workflowId}/stages
  Authorization: Bearer <token>
  ```

#### Update Stage
- **Endpoint:** `PUT /api/workflow-definitions/stages/{stageId}`
- **Removed:** `updatedBy` query parameter
- **Before:**
  ```
  PUT /api/workflow-definitions/stages/{stageId}?updatedBy=john.doe
  ```
- **After:**
  ```
  PUT /api/workflow-definitions/stages/{stageId}
  Authorization: Bearer <token>
  ```

#### Add Step to Stage
- **Endpoint:** `POST /api/workflow-definitions/stages/{stageId}/steps`
- **Removed:** `createdBy` query parameter
- **Before:**
  ```
  POST /api/workflow-definitions/stages/{stageId}/steps?createdBy=john.doe
  ```
- **After:**
  ```
  POST /api/workflow-definitions/stages/{stageId}/steps
  Authorization: Bearer <token>
  ```

#### Update Step
- **Endpoint:** `PUT /api/workflow-definitions/steps/{stepId}`
- **Removed:** `updatedBy` query parameter
- **Before:**
  ```
  PUT /api/workflow-definitions/steps/{stepId}?updatedBy=john.doe
  ```
- **After:**
  ```
  PUT /api/workflow-definitions/steps/{stepId}
  Authorization: Bearer <token>
  ```

#### Add Approvers
- **Endpoint:** `POST /api/workflow-definitions/steps/{stepId}/approvers`
- **Removed:** `createdBy` query parameter
- **Before:**
  ```
  POST /api/workflow-definitions/steps/{stepId}/approvers?createdBy=john.doe
  ```
- **After:**
  ```
  POST /api/workflow-definitions/steps/{stepId}/approvers
  Authorization: Bearer <token>
  ```

#### Activate Workflow
- **Endpoint:** `POST /api/workflow-definitions/{workflowId}/activate`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/workflow-definitions/{workflowId}/activate?userId=john.doe
  ```
- **After:**
  ```
  POST /api/workflow-definitions/{workflowId}/activate
  Authorization: Bearer <token>
  ```

#### Deactivate Workflow
- **Endpoint:** `POST /api/workflow-definitions/{workflowId}/deactivate`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/workflow-definitions/{workflowId}/deactivate?userId=john.doe
  ```
- **After:**
  ```
  POST /api/workflow-definitions/{workflowId}/deactivate
  Authorization: Bearer <token>
  ```

---

### 2. Work Item APIs

#### Create Work Item
- **Endpoint:** `POST /api/work-items`
- **Removed:** `createdBy` query parameter
- **Before:**
  ```
  POST /api/work-items?createdBy=john.doe
  Content-Type: application/json
  
  {
    "type": "contract"
  }
  ```
- **After:**
  ```
  POST /api/work-items
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "type": "contract"
  }
  ```

#### Submit Work Item
- **Endpoint:** `POST /api/work-items/{workItemId}/submit`
- **Removed:** `submittedBy` query parameter
- **Before:**
  ```
  POST /api/work-items/{workItemId}/submit?submittedBy=john.doe
  Content-Type: application/json
  
  {
    "contentRef": "https://...",
    "variables": {}
  }
  ```
- **After:**
  ```
  POST /api/work-items/{workItemId}/submit
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "contentRef": "https://...",
    "variables": {}
  }
  ```

#### Submit Work Item (Convenience Endpoint)
- **Endpoint:** `POST /api/work-items/submit`
- **Changed:** Previously used hardcoded "system" user, now extracts from token
- **Before:**
  ```
  POST /api/work-items/submit
  Content-Type: application/json
  
  {
    "type": "contract",
    "contentRef": "https://...",
    "variables": {}
  }
  ```
- **After:**
  ```
  POST /api/work-items/submit
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "type": "contract",
    "contentRef": "https://...",
    "variables": {}
  }
  ```

#### Archive Work Item
- **Endpoint:** `POST /api/work-items/{workItemId}/archive`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/work-items/{workItemId}/archive?userId=john.doe
  ```
- **After:**
  ```
  POST /api/work-items/{workItemId}/archive
  Authorization: Bearer <token>
  ```

---

### 3. Application Registration APIs

#### Register Application
- **Endpoint:** `POST /api/applications/register`
- **Removed:** `createdBy` query parameter
- **Before:**
  ```
  POST /api/applications/register?createdBy=admin
  Content-Type: application/json
  
  {
    "applicationName": "Web App 1",
    "applicationCode": "webapp1",
    "apiEndpoints": {
      "userApi": "https://..."
    }
  }
  ```
- **After:**
  ```
  POST /api/applications/register
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "applicationName": "Web App 1",
    "applicationCode": "webapp1",
    "apiEndpoints": {
      "userApi": "https://..."
    }
  }
  ```

#### Update Application
- **Endpoint:** `PUT /api/applications/{appId}`
- **Removed:** `updatedBy` query parameter
- **Before:**
  ```
  PUT /api/applications/{appId}?updatedBy=admin
  Content-Type: application/json
  
  {
    "applicationName": "Web App 1 Updated",
    "apiEndpoints": {
      "userApi": "https://..."
    }
  }
  ```
- **After:**
  ```
  PUT /api/applications/{appId}
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "applicationName": "Web App 1 Updated",
    "apiEndpoints": {
      "userApi": "https://..."
    }
  }
  ```

#### Deactivate Application
- **Endpoint:** `DELETE /api/applications/{appId}`
- **Removed:** `updatedBy` query parameter
- **Before:**
  ```
  DELETE /api/applications/{appId}?updatedBy=admin
  ```
- **After:**
  ```
  DELETE /api/applications/{appId}
  Authorization: Bearer <token>
  ```

---

### 4. Workflow Step Rule APIs

#### Create Rule
- **Endpoint:** `POST /api/workflow-definitions/steps/rules`
- **Removed:** `createdBy` query parameter
- **Before:**
  ```
  POST /api/workflow-definitions/steps/rules?createdBy=john.doe
  Content-Type: application/json
  
  {
    "stepDefinitionId": "...",
    "ruleName": "Auto Approve Low Amount",
    "ruleType": "AUTO_APPROVE",
    "ruleExpression": "..."
  }
  ```
- **After:**
  ```
  POST /api/workflow-definitions/steps/rules
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "stepDefinitionId": "...",
    "ruleName": "Auto Approve Low Amount",
    "ruleType": "AUTO_APPROVE",
    "ruleExpression": "..."
  }
  ```

#### Update Rule
- **Endpoint:** `PUT /api/workflow-definitions/steps/rules/{ruleId}`
- **Removed:** `updatedBy` query parameter
- **Before:**
  ```
  PUT /api/workflow-definitions/steps/rules/{ruleId}?updatedBy=john.doe
  Content-Type: application/json
  
  {
    "ruleName": "Auto Approve Low Amount Updated",
    "ruleExpression": "..."
  }
  ```
- **After:**
  ```
  PUT /api/workflow-definitions/steps/rules/{ruleId}
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "ruleName": "Auto Approve Low Amount Updated",
    "ruleExpression": "..."
  }
  ```

---

### 5. Workflow Orchestration APIs

#### Start Workflow
- **Endpoint:** `POST /api/workflows/start`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/workflows/start?workItemId={id}&workflowDefId={id}&userId=john.doe
  ```
- **After:**
  ```
  POST /api/workflows/start?workItemId={id}&workflowDefId={id}
  Authorization: Bearer <token>
  ```

#### Approve Task
- **Endpoint:** `POST /api/workflows/tasks/{taskId}/approve`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/workflows/tasks/{taskId}/approve?userId=john.doe&comments=Approved
  ```
- **After:**
  ```
  POST /api/workflows/tasks/{taskId}/approve?comments=Approved
  Authorization: Bearer <token>
  ```

#### Cancel Workflow
- **Endpoint:** `POST /api/workflows/{instanceId}/cancel`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/workflows/{instanceId}/cancel?userId=john.doe
  ```
- **After:**
  ```
  POST /api/workflows/{instanceId}/cancel
  Authorization: Bearer <token>
  ```

---

### 6. Task Management APIs

#### Approve or Reject Task
- **Endpoint:** `POST /api/tasks/{taskId}/actions`
- **Changed:** Previously used hardcoded "system" user, now extracts from token
- **Before:**
  ```
  POST /api/tasks/{taskId}/actions
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "decisionType": "APPROVED",
    "comments": "Looks good"
  }
  ```
- **After:**
  ```
  POST /api/tasks/{taskId}/actions
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "decisionType": "APPROVED",
    "comments": "Looks good"
  }
  ```
  **Note:** Username is now extracted from token instead of using "system"

#### Approve Task
- **Endpoint:** `POST /api/tasks/{taskId}/approve`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/tasks/{taskId}/approve?userId=john.doe
  Content-Type: application/json
  
  {
    "comments": "Approved"
  }
  ```
- **After:**
  ```
  POST /api/tasks/{taskId}/approve
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "comments": "Approved"
  }
  ```

#### Reject Task
- **Endpoint:** `POST /api/tasks/{taskId}/reject`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/tasks/{taskId}/reject?userId=john.doe
  Content-Type: application/json
  
  {
    "comments": "Needs revision"
  }
  ```
- **After:**
  ```
  POST /api/tasks/{taskId}/reject
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "comments": "Needs revision"
  }
  ```

#### Add Comment
- **Endpoint:** `POST /api/tasks/{taskId}/comments`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/tasks/{taskId}/comments?userId=john.doe
  Content-Type: application/json
  
  {
    "comment": "Please review section 3"
  }
  ```
- **After:**
  ```
  POST /api/tasks/{taskId}/comments
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "comment": "Please review section 3"
  }
  ```

#### Delegate Task
- **Endpoint:** `POST /api/tasks/{taskId}/delegate`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/tasks/{taskId}/delegate?userId=john.doe
  Content-Type: application/json
  
  {
    "toUserId": "jane.doe",
    "reason": "Out of office"
  }
  ```
- **After:**
  ```
  POST /api/tasks/{taskId}/delegate
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "toUserId": "jane.doe",
    "reason": "Out of office"
  }
  ```

#### Reassign Task
- **Endpoint:** `POST /api/tasks/{taskId}/reassign`
- **Removed:** `userId` query parameter
- **Before:**
  ```
  POST /api/tasks/{taskId}/reassign?userId=admin
  Content-Type: application/json
  
  {
    "approverId": "jane.doe",
    "approverType": "USER"
  }
  ```
- **After:**
  ```
  POST /api/tasks/{taskId}/reassign
  Authorization: Bearer <token>
  Content-Type: application/json
  
  {
    "approverId": "jane.doe",
    "approverType": "USER"
  }
  ```

---

## Migration Guide

### Step 1: Remove Query Parameters

Remove the following query parameters from all API calls:
- `createdBy`
- `updatedBy`
- `userId`
- `submittedBy`

### Step 2: Ensure JWT Token is Included

Make sure all API requests include the JWT token in the `Authorization` header:
```http
Authorization: Bearer <your-jwt-token>
```

### Step 3: Update API Client Code

#### Example: Before
```javascript
// ❌ OLD WAY - Don't use this anymore
const createWorkflow = async (workflowData) => {
  const response = await fetch('/api/workflow-definitions?createdBy=john.doe', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(workflowData)
  });
  return response.json();
};
```

#### Example: After
```javascript
// ✅ NEW WAY - Use this
const createWorkflow = async (workflowData) => {
  const response = await fetch('/api/workflow-definitions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`  // Username extracted from token
    },
    body: JSON.stringify(workflowData)
  });
  return response.json();
};
```

### Step 4: Update All API Calls

Search your codebase for the following patterns and remove them:
- `?createdBy=`
- `?updatedBy=`
- `?userId=`
- `?submittedBy=`

---

## Complete API List

### Summary Table

| Controller | Endpoints Changed | Removed Parameters |
|------------|------------------|-------------------|
| WorkflowDefinitionController | 9 | `createdBy`, `updatedBy`, `userId` |
| WorkItemController | 4 | `createdBy`, `submittedBy`, `userId` |
| ApplicationRegistrationController | 3 | `createdBy`, `updatedBy` |
| WorkflowStepRuleController | 2 | `createdBy`, `updatedBy` |
| WorkflowController | 3 | `userId` |
| TaskController | 6 | `userId` |
| **Total** | **27 endpoints** | - |

---

## Important Notes

### 1. JWT Token Requirement
- **All affected endpoints now require a valid JWT token** in the `Authorization` header
- The token must contain the `preferred_username` claim (Keycloak standard)
- If `preferred_username` is not available, the system falls back to `username`, `sub`, or authentication name

### 2. Username Extraction
The system extracts username in this priority order:
1. `preferred_username` claim (Keycloak standard)
2. `username` claim
3. `sub` claim
4. Authentication name
5. Falls back to "system" if none available

### 3. Backward Compatibility
- **This is a breaking change** - old API calls with query parameters will fail
- Update all frontend code before deploying the new backend version
- Test all affected endpoints after migration

### 4. Error Handling
If the JWT token is missing or invalid:
- The request will be rejected with `401 Unauthorized`
- Ensure your authentication flow is working correctly

---

## Testing Checklist

After updating your frontend code, test the following:

- [ ] Create workflow definition
- [ ] Update workflow definition
- [ ] Create stage
- [ ] Update stage
- [ ] Add step to stage
- [ ] Update step
- [ ] Add approvers
- [ ] Activate/deactivate workflow
- [ ] Create work item
- [ ] Submit work item
- [ ] Archive work item
- [ ] Register application
- [ ] Update application
- [ ] Deactivate application
- [ ] Create rule
- [ ] Update rule
- [ ] Start workflow
- [ ] Approve/reject task
- [ ] Cancel workflow
- [ ] Add comment to task
- [ ] Delegate task
- [ ] Reassign task

---

## Support

If you encounter any issues during migration:
1. Verify JWT token is being sent in `Authorization` header
2. Check that token contains `preferred_username` claim
3. Review API response error messages
4. Contact backend team for assistance

---

## Quick Reference

### Removed Parameters Summary
- ❌ `createdBy` - Removed from all create endpoints
- ❌ `updatedBy` - Removed from all update endpoints
- ❌ `userId` - Removed from all user action endpoints
- ❌ `submittedBy` - Removed from submit endpoints

### Required Headers
- ✅ `Authorization: Bearer <jwt-token>` - **Required for all endpoints**

---

**Document Version:** 1.0.0  
**Last Updated:** 2024  
**Status:** Breaking Change - Migration Required
