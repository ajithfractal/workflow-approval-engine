-- ============================================================================
-- Master Schema Database Migration
-- Description: Creates the workflow_master schema and registered_application table
-- Usage: This file is executed on application startup to ensure the master schema
--        and its tables exist.
-- ============================================================================

-- Create workflow_master schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS workflow_master;

-- Set search path to workflow_master
SET search_path TO workflow_master;

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

-- Create index on application_code for faster lookups
CREATE INDEX IF NOT EXISTS idx_registered_application_application_code 
    ON workflow_master.registered_application(application_code);

-- ============================================================================
-- End of Master Schema Migration
-- ============================================================================
