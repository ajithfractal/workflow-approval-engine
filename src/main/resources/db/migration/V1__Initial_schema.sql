-- ============================================================================
-- Workflow Engine Database Migration
-- Version: 1.0.0
-- Description: Initial schema creation for all workflow engine tables
-- ============================================================================

-- ============================================================================
-- Schema: workflow_master
-- Description: Master schema for application registration (not tenant-specific)
-- ============================================================================

-- Create workflow_master schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS workflow_master;

-- ============================================================================
-- Table: workflow_master.registered_application
-- Description: Registered applications in the workflow engine
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_master.registered_application (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_name VARCHAR(100) NOT NULL UNIQUE,
    application_code VARCHAR(50) NOT NULL UNIQUE,
    api_endpoints JSONB NOT NULL,
    api_key VARCHAR(255),
    schema_name VARCHAR(50) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

-- Create index on schema_name for faster lookups
CREATE INDEX IF NOT EXISTS idx_registered_application_schema_name 
    ON workflow_master.registered_application(schema_name);

-- ============================================================================
-- Tenant Schema Tables (created in each tenant schema)
-- These tables are created per tenant schema (e.g., app_webapp1, app_webapp2)
-- ============================================================================

-- ============================================================================
-- Table: work_item
-- Description: Represents a work item (contract, purchase request, etc.) being approved
-- ============================================================================
CREATE TABLE IF NOT EXISTS work_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    current_version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_work_item_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'IN_REVIEW', 'REWORK', 
        'APPROVED', 'REJECTED', 'CANCELLED', 'ARCHIVED'
    ))
);

-- Create index on status for filtering
CREATE INDEX IF NOT EXISTS idx_work_item_status ON work_item(status);
CREATE INDEX IF NOT EXISTS idx_work_item_type ON work_item(type);

-- ============================================================================
-- Table: work_item_version
-- Description: Version history of work items
-- ============================================================================
CREATE TABLE IF NOT EXISTS work_item_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_item_id UUID NOT NULL,
    version INTEGER NOT NULL,
    content_ref TEXT,
    submitted_by VARCHAR(50),
    submitted_at TIMESTAMP WITH TIME ZONE,
    variables JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT uk_work_item_version UNIQUE (work_item_id, version),
    CONSTRAINT fk_work_item_version_work_item 
        FOREIGN KEY (work_item_id) REFERENCES work_item(id)
);

-- Create index on work_item_id and is_active for faster queries
CREATE INDEX IF NOT EXISTS idx_work_item_version_work_item_id 
    ON work_item_version(work_item_id);
CREATE INDEX IF NOT EXISTS idx_work_item_version_is_active 
    ON work_item_version(work_item_id, is_active) WHERE is_active = TRUE;

-- ============================================================================
-- Table: workflow_definition
-- Description: Workflow definition templates (versioned and immutable)
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT uk_workflow_definition_name_version UNIQUE (name, version)
);

-- Create index on name for faster lookups
CREATE INDEX IF NOT EXISTS idx_workflow_definition_name 
    ON workflow_definition(name);
CREATE INDEX IF NOT EXISTS idx_workflow_definition_is_active 
    ON workflow_definition(is_active) WHERE is_active = TRUE;

-- ============================================================================
-- Table: workflow_stage_definition
-- Description: Stage definitions within a workflow
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_stage_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    stage_order INTEGER NOT NULL,
    stage_name VARCHAR(100) NOT NULL,
    step_completion_type VARCHAR(20) NOT NULL,
    min_step_completions INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_workflow_stage_step_completion_type CHECK (step_completion_type IN ('ALL', 'ANY', 'N_OF_M')),
    CONSTRAINT fk_workflow_stage_definition_workflow 
        FOREIGN KEY (workflow_id) REFERENCES workflow_definition(id)
);

-- Create index on workflow_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_workflow_stage_definition_workflow_id 
    ON workflow_stage_definition(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_stage_definition_workflow_order 
    ON workflow_stage_definition(workflow_id, stage_order);

-- ============================================================================
-- Table: workflow_step_definition
-- Description: Step definitions within a workflow stage
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_step_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stage_id UUID NOT NULL,
    step_order INTEGER NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    approval_type VARCHAR(20) NOT NULL,
    min_approvals INTEGER,
    sla_hours INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_workflow_step_approval_type CHECK (approval_type IN ('ALL', 'ANY', 'N_OF_M')),
    CONSTRAINT fk_workflow_step_definition_stage 
        FOREIGN KEY (stage_id) REFERENCES workflow_stage_definition(id)
);

-- Create index on stage_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_workflow_step_definition_stage_id 
    ON workflow_step_definition(stage_id);
CREATE INDEX IF NOT EXISTS idx_workflow_step_definition_stage_order 
    ON workflow_step_definition(stage_id, step_order);

-- ============================================================================
-- Table: workflow_step_approver
-- Description: Approvers for workflow steps
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_step_approver (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    step_id UUID NOT NULL,
    approver_type VARCHAR(20) NOT NULL,
    approver_value VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_workflow_step_approver_type CHECK (approver_type IN ('USER', 'ROLE', 'MANAGER')),
    CONSTRAINT fk_workflow_step_approver_step 
        FOREIGN KEY (step_id) REFERENCES workflow_step_definition(id)
);

-- Create index on step_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_workflow_step_approver_step_id 
    ON workflow_step_approver(step_id);

-- ============================================================================
-- Table: workflow_step_rule
-- Description: Rules for workflow steps (auto-approval, skipping, routing, etc.)
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_step_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    step_definition_id UUID NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    rule_expression TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_workflow_step_rule_type CHECK (rule_type IN (
        'AUTO_APPROVE', 'SKIP_STEP', 'ROUTE_APPROVER', 'SLA_ACTION', 'CONDITIONAL_ROUTING'
    )),
    CONSTRAINT fk_workflow_step_rule_step_definition 
        FOREIGN KEY (step_definition_id) REFERENCES workflow_step_definition(id)
);

-- Create index on step_definition_id and priority for rule evaluation
CREATE INDEX IF NOT EXISTS idx_workflow_step_rule_step_definition_id 
    ON workflow_step_rule(step_definition_id);
CREATE INDEX IF NOT EXISTS idx_workflow_step_rule_priority_active 
    ON workflow_step_rule(step_definition_id, priority DESC, is_active) 
    WHERE is_active = TRUE;

-- ============================================================================
-- Table: workflow_instance
-- Description: Runtime instances of workflow executions
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_instance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    workflow_version INTEGER NOT NULL,
    work_item_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_workflow_instance_status CHECK (status IN (
        'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT fk_workflow_instance_work_item 
        FOREIGN KEY (work_item_id) REFERENCES work_item(id)
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_workflow_instance_work_item_id 
    ON workflow_instance(work_item_id);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_status 
    ON workflow_instance(status);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_workflow_id_version 
    ON workflow_instance(workflow_id, workflow_version);

-- ============================================================================
-- Table: workflow_stage_instance
-- Description: Runtime instances of workflow stage executions
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_stage_instance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL,
    stage_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_workflow_stage_instance_status CHECK (status IN (
        'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED'
    )),
    CONSTRAINT fk_workflow_stage_instance_workflow_instance 
        FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instance(id)
);

-- Create index on workflow_instance_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_workflow_stage_instance_workflow_instance_id 
    ON workflow_stage_instance(workflow_instance_id);
CREATE INDEX IF NOT EXISTS idx_workflow_stage_instance_status 
    ON workflow_stage_instance(status);

-- ============================================================================
-- Table: workflow_step_instance
-- Description: Runtime instances of workflow step executions
-- ============================================================================
CREATE TABLE IF NOT EXISTS workflow_step_instance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL,
    step_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_workflow_step_instance_status CHECK (status IN (
        'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED'
    )),
    CONSTRAINT fk_workflow_step_instance_workflow_instance 
        FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instance(id)
);

-- Create index on workflow_instance_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_workflow_step_instance_workflow_instance_id 
    ON workflow_step_instance(workflow_instance_id);
CREATE INDEX IF NOT EXISTS idx_workflow_step_instance_status 
    ON workflow_step_instance(status);

-- ============================================================================
-- Table: approval_task
-- Description: Approval tasks assigned to approvers
-- ============================================================================
CREATE TABLE IF NOT EXISTS approval_task (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    step_instance_id UUID NOT NULL,
    approver_id VARCHAR(100) NOT NULL,
    approver_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    due_at TIMESTAMP WITH TIME ZONE,
    acted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_approval_task_approver_type CHECK (approver_type IN ('USER', 'ROLE', 'MANAGER')),
    CONSTRAINT chk_approval_task_status CHECK (status IN (
        'PENDING', 'APPROVED', 'REJECTED', 'DELEGATED', 'EXPIRED', 'CANCELLED'
    )),
    CONSTRAINT fk_approval_task_step_instance 
        FOREIGN KEY (step_instance_id) REFERENCES workflow_step_instance(id)
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_approval_task_step_instance_id 
    ON approval_task(step_instance_id);
CREATE INDEX IF NOT EXISTS idx_approval_task_approver_id 
    ON approval_task(approver_id);
CREATE INDEX IF NOT EXISTS idx_approval_task_status 
    ON approval_task(status);
CREATE INDEX IF NOT EXISTS idx_approval_task_due_at 
    ON approval_task(due_at) WHERE status = 'PENDING';

-- ============================================================================
-- Table: approval_decision
-- Description: Immutable records of approval decisions
-- ============================================================================
CREATE TABLE IF NOT EXISTS approval_decision (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_task_id UUID NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comments TEXT,
    decided_by VARCHAR(100) NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT chk_approval_decision_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT fk_approval_decision_approval_task 
        FOREIGN KEY (approval_task_id) REFERENCES approval_task(id)
);

-- Create index on approval_task_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_approval_decision_approval_task_id 
    ON approval_decision(approval_task_id);
CREATE INDEX IF NOT EXISTS idx_approval_decision_decided_by 
    ON approval_decision(decided_by);

-- ============================================================================
-- Table: approval_comment
-- Description: Comments on approval tasks
-- ============================================================================
CREATE TABLE IF NOT EXISTS approval_comment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_task_id UUID NOT NULL,
    comment TEXT NOT NULL,
    commented_by VARCHAR(100) NOT NULL,
    commented_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    CONSTRAINT fk_approval_comment_approval_task 
        FOREIGN KEY (approval_task_id) REFERENCES approval_task(id)
);

-- Create index on approval_task_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_approval_comment_approval_task_id 
    ON approval_comment(approval_task_id);
CREATE INDEX IF NOT EXISTS idx_approval_comment_commented_at 
    ON approval_comment(approval_task_id, commented_at DESC);

-- ============================================================================
-- End of Migration
-- ============================================================================
