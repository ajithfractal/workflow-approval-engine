# Workflow Core Starter

A production-ready, domain-agnostic Spring Boot starter library that provides the core foundation for building approval workflow engines. This library contains entities, enums, repositories, DTOs, and auto-configuration for workflow and approval management systems.

## Overview

`workflow-core-starter` is designed to be a reusable foundation library that can be integrated into any Spring Boot application requiring workflow and approval capabilities. It provides a complete data model and persistence layer without any business logic, making it flexible and adaptable to various use cases.

## Features

- **Domain-Agnostic Design**: No business logic, only data models and persistence
- **Modular Structure**: Organized by domain (workflow, approval, workitem)
- **Spring Boot Auto-Configuration**: Automatically configures when added as a dependency
- **PostgreSQL Compatible**: Uses `TIMESTAMP WITH TIME ZONE` for all timestamp fields
- **UUID Primary Keys**: All entities use UUID identifiers
- **JPA Auditing Ready**: Base entity with `createdAt` and `createdBy` fields
- **Comprehensive Entity Model**: Complete workflow, approval, and work item entities

## Technology Stack

- **Java**: 17+
- **Spring Boot**: 3.2.0+
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
│   ├── enums/            # WorkflowStatus, StepStatus, ApprovalType, ApproverType
│   ├── repository/       # JPA repositories for workflow entities
│   └── dto/              # WorkflowStartRequest
├── approval/             # Approval domain
│   ├── entity/           # ApprovalTask, ApprovalDecision, ApprovalComment
│   ├── enums/            # TaskStatus, DecisionType, ApprovalType, ApproverType
│   ├── repository/        # JPA repositories for approval entities
│   └── dto/              # ApprovalTaskCreateRequest, ApprovalDecisionRequest, etc.
├── workitem/             # Work Item domain
│   ├── entity/           # WorkItem, WorkItemVersion
│   ├── enums/            # WorkItemStatus
│   ├── repository/       # JPA repositories for work item entities
│   └── dto/              # WorkItemCreateRequest
├── common/               # Shared components
│   └── entity/           # BaseEntity (UUID, createdAt, createdBy)
└── config/               # Auto-configuration
    └── WorkflowCoreAutoConfiguration.java
```

## Core Entities

### Workflow Domain

- **WorkflowDefinition**: Template for workflows (versioned, immutable)
- **WorkflowStepDefinition**: Steps within a workflow with approval rules
- **WorkflowStepApprover**: Approvers assigned to workflow steps
- **WorkflowInstance**: Runtime instance of a workflow execution
- **WorkflowStepInstance**: Runtime instance of a workflow step

### Approval Domain

- **ApprovalTask**: Individual task assigned to an approver
- **ApprovalDecision**: Immutable record of approval/rejection decision
- **ApprovalComment**: Additional comments/feedback on approval tasks

### Work Item Domain

- **WorkItem**: The item being approved (contract, purchase request, etc.)
- **WorkItemVersion**: Version history of work items

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

The library automatically configures itself when added to a Spring Boot application. No additional configuration is required.

### Database Configuration

Configure your PostgreSQL datasource in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_db
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Using Repositories

Inject repositories directly into your services:

```java
@Service
public class WorkItemService {
    
    @Autowired
    private WorkItemRepository workItemRepository;
    
    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    
    @Autowired
    private ApprovalTaskRepository approvalTaskRepository;
    
    // Your business logic here
}
```

### Entity Usage

```java
// Create a work item
WorkItem workItem = new WorkItem();
workItem.setType("CONTRACT");
workItem.setStatus(WorkItemStatus.DRAFT);
workItemRepository.save(workItem);

// Create a workflow instance
WorkflowInstance instance = new WorkflowInstance();
instance.setWorkflowId(workflowDefinitionId);
instance.setWorkItemId(workItem.getId());
instance.setStatus(WorkflowStatus.IN_PROGRESS);
workflowInstanceRepository.save(instance);
```

## Status Management

### Engine-Controlled Statuses (Enums)

These statuses are controlled by the workflow/approval engines:

- **WorkflowInstance.status**: `WorkflowStatus` enum (NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED)
- **WorkflowStepInstance.status**: `StepStatus` enum (NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED)
- **ApprovalTask.status**: `TaskStatus` enum (PENDING, APPROVED, REJECTED, DELEGATED, EXPIRED, CANCELLED)

### Product-Controlled Statuses (Enums)

- **WorkItem.status**: `WorkItemStatus` enum (DRAFT, SUBMITTED, IN_REVIEW, REWORK, APPROVED, REJECTED, CANCELLED, ARCHIVED)

## Database Schema

All entities use:
- **UUID** primary keys
- **TIMESTAMP WITH TIME ZONE** for all timestamp fields
- Proper foreign key relationships
- Unique constraints where applicable

### Key Tables

- `workflow_definition` - Workflow templates
- `workflow_step_definition` - Step definitions with approval rules
- `workflow_instance` - Runtime workflow executions
- `workflow_step_instance` - Runtime step executions
- `approval_task` - Approval tasks for approvers
- `approval_decision` - Approval/rejection decisions
- `work_item` - Items being approved
- `work_item_version` - Version history

## Extension Points

The library is designed to be extended. You can:

1. **Implement Business Logic**: Create services that use the repositories
2. **Add Custom Entities**: Extend BaseEntity for domain-specific entities
3. **Customize Auto-Configuration**: Override beans as needed
4. **Add Validation**: Implement validation logic in your services

## Building the Project

```bash
mvn clean install
```

## Requirements

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or compatible database)

## License

[Specify your license here]

## Contributing

[Contributing guidelines]

## Support

[Support information]
