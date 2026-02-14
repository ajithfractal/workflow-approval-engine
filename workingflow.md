

## 1. Project Overview

**What it is:** A Spring Boot starter library for building approval workflow engines. It's domain-agnostic and reusable.

**Purpose:** Provides a foundation for approval workflows (contracts, purchase requests, expense claims, etc.) with state machines, REST APIs, and workflow orchestration.

---

## 2. What's Implemented ✅

### Core Architecture

- **4 State Machines** (Spring State Machine):
  1. **WorkItem State Machine** - Manages work item lifecycle
  2. **WorkflowInstance State Machine** - Manages workflow execution
  3. **WorkflowStepInstance State Machine** - Manages step execution
  4. **ApprovalTask State Machine** - Manages individual task approvals

- **3 Main Domains:**
  1. **Workflow Domain** - Definitions, instances, steps
  2. **Approval Domain** - Tasks, decisions, comments
  3. **Work Item Domain** - Items being approved with versioning

### REST APIs

#### Workflow Definition APIs (`/api/workflow-definitions`)
- ✅ Create/List/Get/Update/Delete workflow definitions
- ✅ Create/Update/Delete steps
- ✅ Add/Remove approvers
- ✅ Activate/Deactivate workflows
- ✅ Versioning (creates new version if instances exist)

#### Work Item APIs (`/api/work-items`)
- ✅ Create/List/Get work items
- ✅ Submit work item (creates version)
- ✅ Get workflow progress
- ✅ Get work items by workflow definition
- ✅ Archive work items

#### Task APIs (`/api/tasks`)
- ✅ List tasks by approver
- ✅ Get task details
- ✅ Approve/Reject tasks
- ✅ Add comments
- ✅ Delegate tasks
- ✅ Reassign tasks

### Key Features

- ✅ **Parallel Approvals**: Steps with same `stepOrder` run concurrently
- ✅ **Approval Rules**: ALL, ANY, N_OF_M
- ✅ **Workflow Versioning**: Updates create new versions if instances exist
- ✅ **Work Item Versioning**: Tracks submission history
- ✅ **Approver Types**: USER, ROLE, MANAGER (via `ApproverResolver`)
- ✅ **State Machine Orchestration**: Coordinates all state machines
- ✅ **Swagger Documentation**: OpenAPI/Swagger UI
- ✅ **Global Exception Handling**: Centralized error handling

---

## 3. How State Machines Work 🔄

### Architecture Pattern

Each entity has its own state machine that manages lifecycle transitions. The `WorkflowOrchestratorService` coordinates them.

### State Machine Flow Example

```
WorkItem: DRAFT → SUBMITTED → IN_REVIEW → APPROVED/REJECTED → ARCHIVED
WorkflowInstance: NOT_STARTED → IN_PROGRESS → COMPLETED/FAILED/CANCELLED
StepInstance: NOT_STARTED → IN_PROGRESS → COMPLETED/FAILED
Task: PENDING → APPROVED/REJECTED/DELEGATED/EXPIRED/CANCELLED
```

### Approval Flow (Step-by-Step)

**1. Work Item Submission:**
- WorkItem: `DRAFT` → `SUBMITTED`
- Creates `WorkItemVersion` with `contentRef`

**2. Workflow Start:**
- Creates `WorkflowInstance` (status: `NOT_STARTED`)
- Creates `WorkflowStepInstance` for each step (status: `NOT_STARTED`)
- WorkflowInstance: `NOT_STARTED` → `IN_PROGRESS`
- WorkItem: `SUBMITTED` → `IN_REVIEW`
- Starts first step(s) (order = 1) in parallel
- StepInstance: `NOT_STARTED` → `IN_PROGRESS`
- Creates `ApprovalTask` for each approver (status: `PENDING`)

**3. Approver Approves:**
- Task: `PENDING` → `APPROVED`
- Creates `ApprovalDecision` record
- `ApprovalRuleEvaluator` checks step completion:
  - Gets all tasks for the step
  - Checks approval rule (ALL/ANY/N_OF_M)
  - Returns: `COMPLETE`, `PENDING`, or `REJECTED`

**4. Step Completion (if rule = `COMPLETE`):**
- StepInstance: `IN_PROGRESS` → `COMPLETED`
- Checks for parallel steps (same order) still in progress
- If all parallel steps complete → starts next step(s)
- If no more steps → completes workflow

**5. Workflow Completion:**
- WorkflowInstance: `IN_PROGRESS` → `COMPLETED`
- WorkItem: `IN_REVIEW` → `APPROVED`

**6. Rejection Flow (if rule = `REJECTED`):**
- StepInstance: `IN_PROGRESS` → `FAILED`
- Cancels remaining pending tasks
- WorkflowInstance: `IN_PROGRESS` → `FAILED`
- WorkItem: `IN_REVIEW` → `REJECTED`

### Parallel Step Execution

Steps with the same `stepOrder` run concurrently:
- Step 1A (order=1) and Step 1B (order=1) start together
- Both must complete before Step 2 (order=2) starts
- Implemented in `handleStepCompletion()` - checks for parallel steps before advancing

---

## 4. Key Entities & Relationships

```
WorkflowDefinition (Template)
    ├── WorkflowStepDefinition (Steps with approval rules)
    │       └── WorkflowStepApprover (Approvers: USER/ROLE/MANAGER)
    │
    └── WorkflowInstance (Runtime execution)
            └── WorkflowStepInstance (Runtime step)
                    └── ApprovalTask (Individual task)
                            ├── ApprovalDecision (Approval/Rejection record)
                            └── ApprovalComment (Comments)

WorkItem (Business entity)
    ├── WorkItemVersion (Version history)
    └── WorkflowInstance (Linked workflow)
```

---

## 5. Technology Stack

- **Java 17+**
- **Spring Boot 3.2.0**
- **Spring State Machine 4.0.0**
- **Spring Data JPA / Hibernate**
- **PostgreSQL** (UUID primary keys, TIMESTAMP WITH TIME ZONE)
- **Lombok**
- **SpringDoc OpenAPI** (Swagger)
- **Maven**

---

## 6. Pending / Future Enhancements 🔜

- ⏳ Step completion status in workflow definition API (in progress)
- ⏳ Conditional routing (if-then-else based on data)
- ⏳ Rework loops (send back to previous step)
- ⏳ SLA monitoring and automatic task expiration
- ⏳ Notifications (email/SMS/WhatsApp integration points exist)
- ⏳ Workflow designer UI (React + Vite project exists, needs integration)
- ⏳ Audit logging (base entity has `createdAt`/`createdBy`, needs `updatedAt`/`updatedBy`)

---

## 7. Interview Talking Points 💡

### Architecture Decisions

**1. Why State Machines?**
- Enforces valid transitions
- Clear lifecycle management
- Easier to extend

**2. Why Separate WorkItem and WorkflowInstance?**
- WorkItem = business entity (contract, purchase request)
- WorkflowInstance = runtime execution
- Supports rework (new workflow instance, same work item)

**3. Why Versioning?**
- Workflow definitions: immutability for running instances
- Work items: track submission history

**4. Parallel Approvals:**
- Steps with same `stepOrder` run concurrently
- All must complete before next order starts

### Code Highlights

- `WorkflowOrchestratorService`: Coordinates all state machines
- `ApprovalRuleEvaluator`: Evaluates ALL/ANY/N_OF_M rules
- State Machine Services: Handle transitions with guards/actions
- Repository Pattern: JPA repositories for data access
- DTO Pattern: Separate request/response objects

### Design Patterns

- **State Machine Pattern**: Lifecycle management
- **Repository Pattern**: Data access abstraction
- **Service Layer Pattern**: Business logic separation
- **DTO Pattern**: API contract separation
- **Orchestrator Pattern**: Coordinates multiple services

---

## 8. Quick Demo Flow

```
1. Create Workflow Definition
   POST /api/workflow-definitions
   → Creates workflow template

2. Add Steps & Approvers
   POST /api/workflow-definitions/{id}/steps
   POST /api/workflow-definitions/steps/{id}/approvers
   → Defines approval flow

3. Create Work Item
   POST /api/work-items
   → Creates draft work item

4. Submit Work Item
   POST /api/work-items/{id}/submit
   → Starts workflow, creates tasks

5. Approver Approves
   POST /api/tasks/{taskId}/approve
   → Evaluates rules, advances workflow

6. Check Progress
   GET /api/work-items/{id}/workflow-progress
   → Shows current step, completed steps
```

---

## 9. Key Files to Mention

- `WorkflowOrchestratorServiceImpl.java` - Main orchestration logic
- `ApprovalRuleEvaluator.java` - Rule evaluation logic
- `WorkflowDefinitionServiceImpl.java` - Workflow CRUD
- State Machine Configs - Define valid transitions
- Controllers - REST API endpoints

---
