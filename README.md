# Workflow Core Starter

A production-ready, domain-agnostic Spring Boot starter library that provides a complete foundation for building approval workflow engines. This library includes entities, repositories, DTOs, state machines, and services for workflow and approval management systems.

## Overview

`workflow-core-starter` is designed to be a reusable foundation library that can be integrated into any Spring Boot application requiring workflow and approval capabilities. It provides:

- **Complete State Machine Implementation**: Spring State Machine-based state management for workflows, steps, tasks, and work items
- **Service Layer**: Ready-to-use services for workflow definition, work item management, and task management
- **Comprehensive Data Model**: Complete workflow, approval, and work item entities with relationships
- **Auto-Configuration**: Automatically configures when added as a dependency

## Features

- **Spring State Machine Integration**: State machines for Work Item, Workflow Instance, Workflow Step Instance, and Approval Task lifecycle management
- **Workflow Definition Service**: Create and manage workflow definitions with steps, approvers, and approval rules
- **Work Item Service**: Manage work items with version control and state machine-driven status transitions
- **Task Management Service**: Create, fetch, and reassign approval tasks
- **Approval Rule Evaluator**: Evaluate step-level approval rules (ALL, ANY, N_OF_M)
- **Domain-Agnostic Design**: Flexible and adaptable to various use cases
- **Modular Structure**: Organized by domain (workflow, approval, workitem, taskmanagement)
- **PostgreSQL Compatible**: Uses `TIMESTAMP WITH TIME ZONE` for all timestamp fields
- **UUID Primary Keys**: All entities use UUID identifiers
- **JPA Auditing Ready**: Base entity with `createdAt` and `createdBy` fields

## Technology Stack

- **Java**: 17+
- **Spring Boot**: 3.2.0+
- **Spring State Machine**: 4.0.0
- **Spring Data JPA**: For repository abstraction
- **Hibernate**: JPA implementation
- **PostgreSQL**: Database (compatible)
- **Lombok**: For reducing boilerplate code
- **Maven**: Build tool

## Project Structure

```
src/main/java/com/fractalhive/workflowcore/
├── workflow/              # Workflow domain
│   ├── entity/           # WorkflowDefinition, WorkflowInstance, WorkflowStepDefinition, etc.
│   ├── enums/            # WorkflowStatus, StepStatus
│   ├── repository/       # JPA repositories for workflow entities
│   ├── dto/              # WorkflowStartRequest, WorkflowDefinitionCreateRequest, etc.
│   ├── service/          # WorkflowDefinitionService
│   └── statemachine/     # Workflow Instance & Step Instance State Machines
│       ├── config/       # State machine configurations
│       ├── action/       # State transition actions
│       ├── enums/        # State machine events
│       └── service/      # State machine services
├── approval/             # Approval domain
│   ├── entity/           # ApprovalTask, ApprovalDecision, ApprovalComment
│   ├── enums/            # TaskStatus, DecisionType, ApprovalType, ApproverType, etc.
│   ├── repository/       # JPA repositories for approval entities
│   ├── dto/              # ApprovalTaskCreateRequest, ApprovalDecisionRequest, etc.
│   ├── service/          # ApprovalTaskStateMachineService, ApprovalRuleEvaluator
│   └── statemachine/     # Approval Task State Machine
│       ├── config/       # State machine configuration
│       ├── action/       # State transition actions
│       ├── guard/        # State transition guards
│       └── enums/        # ApprovalTaskEvent
├── workitem/             # Work Item domain
│   ├── entity/           # WorkItem, WorkItemVersion
│   ├── enums/            # WorkItemStatus
│   ├── repository/       # JPA repositories for work item entities
│   ├── dto/              # WorkItemCreateRequest, WorkItemSubmitRequest, etc.
│   ├── service/          # WorkItemService
│   └── statemachine/     # Work Item State Machine
│       ├── config/       # State machine configuration
│       ├── action/       # State transition actions
│       ├── guard/        # State transition guards
│       ├── enums/        # WorkItemEvent
│       └── service/      # WorkItemStateMachineService
├── taskmanagement/       # Task Management domain
│   ├── dto/              # TaskResponse, TaskReassignRequest
│   ├── resolver/         # ApproverResolver interface
│   └── service/          # TaskManagementService
├── common/               # Shared components
│   └── entity/           # BaseEntity (UUID, createdAt, createdBy)
└── config/               # Auto-configuration
    └── WorkflowCoreAutoConfiguration.java
```

## Core Entities

### Workflow Domain

- **WorkflowDefinition**: Template for workflows (versioned, immutable)
- **WorkflowStepDefinition**: Steps within a workflow with approval rules (ALL, ANY, N_OF_M)
- **WorkflowStepApprover**: Approvers assigned to workflow steps (USER, ROLE, MANAGER)
- **WorkflowInstance**: Runtime instance of a workflow execution
- **WorkflowStepInstance**: Runtime instance of a workflow step

### Approval Domain

- **ApprovalTask**: Individual task assigned to an approver
- **ApprovalDecision**: Immutable record of approval/rejection decision
- **ApprovalComment**: Additional comments/feedback on approval tasks

### Work Item Domain

- **WorkItem**: The item being approved (contract, purchase request, etc.)
- **WorkItemVersion**: Version history of work items

## State Machines

The library includes four state machines for managing entity lifecycles:

### 1. Approval Task State Machine

**States**: `PENDING`, `APPROVED`, `REJECTED`, `DELEGATED`, `EXPIRED`, `CANCELLED`

**Events**: `APPROVE`, `REJECT`, `DELEGATE`, `SLA_BREACH`, `WORKFLOW_CANCELLED`, `ACCEPT`

**Service**: `ApprovalTaskStateMachineService`

```java
@Autowired
private ApprovalTaskStateMachineService approvalTaskStateMachineService;

// Approve a task
approvalTaskStateMachineService.approve(taskId, userId, "Looks good!");

// Reject a task
approvalTaskStateMachineService.reject(taskId, userId, "Needs revision");

// Delegate a task
approvalTaskStateMachineService.delegate(taskId, fromUserId, toUserId);
```

### 2. Work Item State Machine

**States**: `DRAFT`, `SUBMITTED`, `IN_REVIEW`, `APPROVED`, `REJECTED`, `REWORK`, `ARCHIVED`, `CANCELLED`

**Events**: `SUBMIT`, `START_REVIEW`, `APPROVE`, `REJECT`, `SEND_TO_REWORK`, `ARCHIVE`, `CANCEL`

**Service**: `WorkItemStateMachineService`

```java
@Autowired
private WorkItemStateMachineService workItemStateMachineService;

// Submit work item
workItemStateMachineService.submit(workItemId, contentRef, submittedBy);

// Start review
workItemStateMachineService.startReview(workItemId, userId);

// Approve work item
workItemStateMachineService.approve(workItemId, userId);
```

### 3. Workflow Instance State Machine

**States**: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `CANCELLED`

**Events**: `START`, `COMPLETE`, `FAIL`, `CANCEL`

**Service**: `WorkflowInstanceStateMachineService`

```java
@Autowired
private WorkflowInstanceStateMachineService workflowInstanceStateMachineService;

// Start workflow
workflowInstanceStateMachineService.start(workflowInstanceId, userId);

// Complete workflow
workflowInstanceStateMachineService.complete(workflowInstanceId, userId);
```

### 4. Workflow Step Instance State Machine

**States**: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`

**Events**: `START`, `COMPLETE`, `FAIL`

**Service**: `WorkflowStepInstanceStateMachineService`

```java
@Autowired
private WorkflowStepInstanceStateMachineService stepInstanceStateMachineService;

// Start step
stepInstanceStateMachineService.start(stepInstanceId, userId);

// Complete step
stepInstanceStateMachineService.complete(stepInstanceId, userId);
```

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.fractalhive</groupId>
    <artifactId>workflow-core-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.fractalhive:workflow-core-starter:1.0.0-SNAPSHOT'
```

## Usage

### Auto-Configuration

The library automatically configures itself when added to a Spring Boot application. All state machines, services, and repositories are auto-configured.

### Database Configuration

Configure your PostgreSQL datasource in your application's `application.properties` or `application.yml`:

**application.properties:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/workflow_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

**application.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_db
    username: your_username
    password: your_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Creating a Workflow Definition

```java
@Autowired
private WorkflowDefinitionService workflowDefinitionService;

// 1. Create workflow definition
WorkflowDefinitionCreateRequest workflowRequest = WorkflowDefinitionCreateRequest.builder()
    .name("contract-approval-v1")
    .version(1)
    .build();

UUID workflowId = workflowDefinitionService.createWorkflow(workflowRequest, "admin");

// 2. Create steps
StepDefinitionRequest legalStep = StepDefinitionRequest.builder()
    .stepName("legal-review")
    .approvalType(ApprovalType.ALL)
    .stepOrder(1)
    .slaHours(24)
    .build();

UUID stepId = workflowDefinitionService.createStep(workflowId, legalStep, "admin");

// 3. Add approvers
ApproverRequest approver = ApproverRequest.builder()
    .approverType(ApproverType.USER)
    .approverValue("legal_user_1")
    .build();

workflowDefinitionService.addApprover(stepId, approver, "admin");
```

### Managing Work Items

```java
@Autowired
private WorkItemService workItemService;

// Create work item
WorkItemCreateRequest workItemRequest = WorkItemCreateRequest.builder()
    .type("CONTRACT")
    .build();

UUID workItemId = workItemService.createWorkItem(workItemRequest, "user1");

// Submit work item (uses state machine: DRAFT → SUBMITTED)
WorkItemSubmitRequest submitRequest = WorkItemSubmitRequest.builder()
    .contentRef("s3://bucket/contract-v1.pdf")
    .build();

UUID versionId = workItemService.submitWorkItem(workItemId, submitRequest, "user1");

// Get work item details
WorkItemResponse workItem = workItemService.getWorkItem(workItemId);
```

### Managing Approval Tasks

```java
@Autowired
private TaskManagementService taskManagementService;

@Autowired
private ApprovalTaskStateMachineService approvalTaskStateMachineService;

// Create tasks for a step instance
List<UUID> taskIds = taskManagementService.createTasksForStep(stepInstanceId, "system");

// Get tasks for an approver
List<TaskResponse> tasks = taskManagementService.getTasksByApprover("user123", TaskStatus.PENDING);

// Approve a task (uses state machine: PENDING → APPROVED)
approvalTaskStateMachineService.approve(taskIds.get(0), "user123", "Approved!");

// Reject a task (uses state machine: PENDING → REJECTED)
approvalTaskStateMachineService.reject(taskIds.get(1), "user123", "Needs revision");
```

### Evaluating Approval Rules

```java
@Autowired
private ApprovalRuleEvaluator approvalRuleEvaluator;

// Evaluate if a step's approval criteria are met
RuleEvaluationResult result = approvalRuleEvaluator.evaluate(stepInstanceId);

switch (result) {
    case COMPLETE:
        // All approval criteria met, proceed to next step
        break;
    case REJECTED:
        // Step rejected, handle rejection
        break;
    case PENDING:
        // Still waiting for approvals
        break;
}
```

### Implementing ApproverResolver

For dynamic approver resolution (ROLE, MANAGER types), implement the `ApproverResolver` interface:

```java
@Component
public class CustomApproverResolver implements ApproverResolver {
    
    @Override
    public List<String> resolveRoleToUserIds(String role) {
        // Your logic to resolve role to user IDs
        return List.of("user1", "user2", "user3");
    }
    
    @Override
    public List<String> resolveManagerChain(String userId) {
        // Your logic to resolve manager chain
        return List.of("manager1", "manager2");
    }
}
```

## Status Management

### State Machine-Controlled Statuses

These statuses are managed by state machines and should be transitioned using the state machine services:

- **WorkflowInstance.status**: `WorkflowStatus` (NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED)
- **WorkflowStepInstance.status**: `StepStatus` (NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED)
- **ApprovalTask.status**: `TaskStatus` (PENDING, APPROVED, REJECTED, DELEGATED, EXPIRED, CANCELLED)
- **WorkItem.status**: `WorkItemStatus` (DRAFT, SUBMITTED, IN_REVIEW, APPROVED, REJECTED, REWORK, ARCHIVED, CANCELLED)

### Approval Types

- **ALL**: All assigned approvers must approve
- **ANY**: Any one approval is sufficient
- **N_OF_M**: Requires minimum number of approvals (specified in `minApprovals`)

### Approver Types

- **USER**: Specific user ID
- **ROLE**: Role name (resolved via `ApproverResolver`)
- **MANAGER**: Manager chain (resolved via `ApproverResolver`)

## Database Schema

All entities use:
- **UUID** primary keys
- **TIMESTAMP WITH TIME ZONE** for all timestamp fields
- Proper foreign key relationships
- Unique constraints where applicable

### Key Tables

- `workflow_definition` - Workflow templates
- `workflow_step_definition` - Step definitions with approval rules
- `workflow_step_approver` - Approvers for workflow steps
- `workflow_instance` - Runtime workflow executions
- `workflow_step_instance` - Runtime step executions
- `approval_task` - Approval tasks for approvers
- `approval_decision` - Approval/rejection decisions
- `approval_comment` - Comments on approval tasks
- `work_item` - Items being approved
- `work_item_version` - Version history

## Example: Complete Workflow Flow

```java
@Autowired
private WorkflowDefinitionService workflowDefinitionService;
@Autowired
private WorkItemService workItemService;
@Autowired
private TaskManagementService taskManagementService;
@Autowired
private ApprovalTaskStateMachineService approvalTaskStateMachineService;
@Autowired
private ApprovalRuleEvaluator approvalRuleEvaluator;
@Autowired
private WorkflowInstanceStateMachineService workflowInstanceStateMachineService;
@Autowired
private WorkflowStepInstanceStateMachineService stepInstanceStateMachineService;

// 1. Create workflow definition
UUID workflowId = workflowDefinitionService.createWorkflow(
    WorkflowDefinitionCreateRequest.builder()
        .name("purchase-approval")
        .version(1)
        .build(),
    "admin"
);

// 2. Create steps and approvers
UUID stepId = workflowDefinitionService.createStep(workflowId, stepRequest, "admin");
workflowDefinitionService.addApprover(stepId, approverRequest, "admin");

// 3. Create work item
UUID workItemId = workItemService.createWorkItem(
    WorkItemCreateRequest.builder().type("PURCHASE_ORDER").build(),
    "user1"
);

// 4. Submit work item
workItemService.submitWorkItem(workItemId, submitRequest, "user1");

// 5. Create workflow instance (your orchestration logic)
WorkflowInstance instance = new WorkflowInstance();
instance.setWorkflowId(workflowId);
instance.setWorkItemId(workItemId);
// ... save instance

// 6. Start workflow
workflowInstanceStateMachineService.start(instance.getId(), "system");

// 7. Create step instance and start it
WorkflowStepInstance stepInstance = new WorkflowStepInstance();
stepInstance.setWorkflowInstanceId(instance.getId());
stepInstance.setStepId(stepId);
// ... save step instance

stepInstanceStateMachineService.start(stepInstance.getId(), "system");

// 8. Create tasks for approvers
List<UUID> taskIds = taskManagementService.createTasksForStep(stepInstance.getId(), "system");

// 9. Approvers approve/reject tasks
approvalTaskStateMachineService.approve(taskIds.get(0), "approver1", "Approved");

// 10. Evaluate step completion
RuleEvaluationResult result = approvalRuleEvaluator.evaluate(stepInstance.getId());
if (result == RuleEvaluationResult.COMPLETE) {
    stepInstanceStateMachineService.complete(stepInstance.getId(), "system");
}
```

## Extension Points

The library is designed to be extended. You can:

1. **Implement Business Logic**: Create orchestration services that coordinate state machines
2. **Add Custom Entities**: Extend BaseEntity for domain-specific entities
3. **Customize Auto-Configuration**: Override beans as needed
4. **Implement ApproverResolver**: Provide custom logic for role/manager resolution
5. **Add Event Listeners**: Listen to state machine events for custom processing

## Building the Project

```bash
mvn clean install
```

## Requirements

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or compatible database)
- Spring Boot 3.2.0+

## License

[Specify your license here]

## Contributing

[Contributing guidelines]

## Support

[Support information]
