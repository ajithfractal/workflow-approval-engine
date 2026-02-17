# Rule Engine Flow - Complete Explanation

## 📋 Table of Contents
1. [Overview](#overview)
2. [Key Components](#key-components)
3. [Complete Flow](#complete-flow)
4. [Rule Storage](#rule-storage)
5. [Rule Evaluation Process](#rule-evaluation-process)
6. [How Rules Are Triggered](#how-rules-are-triggered)
7. [Examples](#examples)

---

## 🎯 Overview

The Rule Engine allows you to define **dynamic business rules** for workflow steps using **SpEL (Spring Expression Language)**. Rules can:
- Auto-approve tasks based on conditions
- Skip steps entirely
- Route to different approvers
- Handle rejections
- Trigger SLA actions

**Key Concept**: Rules are evaluated **at runtime** against workflow data (variables, tasks, steps, etc.)

---

## 🧩 Key Components

### 1. **WorkflowStepRule** (Entity)
**Location**: `rulesengine/entity/WorkflowStepRule.java`

**What it is**: Database entity storing rule definitions

**Fields**:
- `stepDefinitionId` - Which step this rule belongs to
- `ruleName` - Human-readable name
- `ruleType` - Type of rule (AUTO_APPROVE, SKIP_STEP, etc.)
- `ruleExpression` - **JSON string** containing condition and actions
- `priority` - Higher priority rules evaluated first
- `isActive` - Whether rule is currently active

**Example ruleExpression**:
```json
{
  "condition": "variables['loanAmount'] <= 30000",
  "actions": {
    "autoApprove": true
  }
}
```

---

### 2. **RuleContext** (DTO)
**Location**: `rulesengine/dto/RuleContext.java`

**What it is**: Container for all data available to rule expressions

**Contains**:
- `variables` - Map of custom variables from WorkItemVersion (e.g., loanAmount, creditScore)
- `workItem` - Work item entity
- `workItemVersion` - Active work item version
- `workflowInstance` - Workflow instance entity
- `stepInstance` - Current step instance
- `task` - Current approval task (if applicable)
- `elapsedHours` - Hours since task creation (for SLA)
- `hoursUntilDue` - Hours until due date
- `hasParallelSteps` - Whether step has parallel steps

**Purpose**: Provides all context data that rules can access

---

### 3. **RuleContextBuilder** (Service)
**Location**: `rulesengine/service/RuleContextBuilder.java`

**What it does**: Builds `RuleContext` objects from workflow entities

**Methods**:
- `buildForWorkflowInstance(workflowInstanceId)` - Builds context for workflow-level rules
- `buildForStepInstance(stepInstanceId)` - Builds context for step-level rules
- `buildForTask(taskId)` - Builds context for task-level rules (includes SLA data)

**Flow**:
1. Takes an ID (stepInstanceId, taskId, etc.)
2. Fetches related entities from database
3. Extracts variables from active WorkItemVersion
4. Calculates derived values (elapsedHours, hasParallelSteps, etc.)
5. Returns complete `RuleContext`

---

### 4. **SimpleRuleEngineService** (Service)
**Location**: `rulesengine/service/SimpleRuleEngineService.java`

**What it does**: Evaluates rules using SpEL

**Key Methods**:

#### `evaluateRules(stepDefinitionId, ruleType, context)`
- Fetches all active rules for a step definition
- Sorts by priority (highest first)
- Evaluates each rule until one matches
- Returns `RuleExecutionResult` with actions

#### `evaluateRule(rule, context)` (private)
- Parses `ruleExpression` JSON
- Extracts `condition` string
- Creates SpEL evaluation context
- Evaluates condition using SpEL
- If condition is true, extracts `actions` and builds result

#### `createEvaluationContext(context)` (private)
- Creates SpEL `EvaluationContext`
- Sets up root object map with all accessible properties
- Makes variables available as `variables['key']` or direct access
- Returns both context and root object

#### `buildResult(ruleConfig, ruleType)` (private)
- Extracts actions from rule JSON
- Maps actions to `RuleExecutionResult` based on rule type
- Returns result with appropriate flags set

---

### 5. **RuleExecutionResult** (DTO)
**Location**: `rulesengine/dto/RuleExecutionResult.java`

**What it is**: Result of rule evaluation containing actions to take

**Fields**:
- `ruleMatched` - Whether any rule matched
- `shouldAutoApprove` - Auto-approve all tasks
- `shouldSkipStep` - Skip the step entirely
- `newApproverId` - Route to different approver
- `rejectionStrategy` - How to handle rejections
- `slaAction` - SLA action to take
- `metadata` - Additional custom data

---

## 🔄 Complete Flow

### Phase 1: Rule Creation (Setup)

```
1. Admin creates workflow definition
   └─> WorkflowDefinition created

2. Admin creates step definition
   └─> WorkflowStepDefinition created

3. Admin creates rule for step
   POST /api/workflow-definitions/steps/{stepId}/rules
   {
     "ruleName": "Auto-approve small loans",
     "ruleType": "AUTO_APPROVE",
     "ruleExpression": "{\"condition\": \"variables['loanAmount'] <= 30000\", \"actions\": {\"autoApprove\": true}}",
     "priority": 10
   }
   └─> WorkflowStepRule saved to database
```

---

### Phase 2: Workflow Execution (Runtime)

```
1. User submits work item
   POST /api/work-items/submit
   {
     "variables": {
       "loanAmount": 25000,
       "creditScore": 720
     }
   }
   └─> WorkItemVersion created with variables

2. Workflow starts
   └─> WorkflowInstance created
   └─> WorkflowStepInstance created for each step
   └─> Tasks created for approvers

3. After tasks created, auto-approve rules evaluated
   WorkflowOrchestratorServiceImpl.evaluateAndApplyAutoApproveRules()
   │
   ├─> RuleContextBuilder.buildForStepInstance(stepInstanceId)
   │   │
   │   ├─> Fetch WorkflowStepInstance
   │   ├─> Fetch WorkflowInstance
   │   ├─> Fetch WorkItem
   │   ├─> Fetch active WorkItemVersion
   │   ├─> Extract variables map
   │   └─> Return RuleContext
   │
   ├─> RuleEngineService.evaluateRules(stepDefinitionId, AUTO_APPROVE, context)
   │   │
   │   ├─> Fetch rules from database
   │   │   └─> findByStepDefinitionIdAndRuleTypeAndIsActiveTrue()
   │   │
   │   ├─> Sort by priority (highest first)
   │   │
   │   ├─> For each rule:
   │   │   ├─> Parse ruleExpression JSON
   │   │   │   └─> Extract "condition" and "actions"
   │   │   │
   │   │   ├─> Create SpEL evaluation context
   │   │   │   ├─> Create root object map
   │   │   │   ├─> Add variables to root
   │   │   │   ├─> Add context, task, step properties
   │   │   │   └─> Return EvaluationContext + rootObject
   │   │   │
   │   │   ├─> Evaluate condition using SpEL
   │   │   │   └─> expression.getValue(context, rootObject, Boolean.class)
   │   │   │   └─> Evaluates: variables['loanAmount'] <= 30000
   │   │   │   └─> Returns: true (if condition matches)
   │   │   │
   │   │   └─> If condition is true:
   │   │       ├─> Extract actions
   │   │       ├─> Build RuleExecutionResult
   │   │       └─> Return result (STOP - first match wins)
   │   │
   │   └─> Return RuleExecutionResult
   │
   └─> If shouldAutoApprove == true:
       ├─> Auto-approve all pending tasks
       ├─> Evaluate step completion
       └─> Move to next step
```

---

## 💾 Rule Storage

### Database Table: `workflow_step_rule`

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `step_definition_id` | UUID | Foreign key to workflow_step_definition |
| `rule_name` | VARCHAR(100) | Human-readable name |
| `rule_type` | VARCHAR(50) | AUTO_APPROVE, SKIP_STEP, etc. |
| `rule_expression` | TEXT | JSON string with condition and actions |
| `priority` | INTEGER | Higher = evaluated first |
| `is_active` | BOOLEAN | Whether rule is active |
| `description` | TEXT | Optional description |

### Rule Expression Format

```json
{
  "condition": "SpEL expression that returns boolean",
  "actions": {
    "autoApprove": true,           // For AUTO_APPROVE
    "skipStep": true,              // For SKIP_STEP
    "newApproverId": "user123",   // For ROUTE_APPROVER
    "rejectionStrategy": "REWORK_STEP", // For REJECTION_STRATEGY
    "slaAction": "DELEGATE",       // For SLA_ACTION
    "delegateTo": "senior-manager" // For SLA_ACTION
  }
}
```

---

## 🔍 Rule Evaluation Process

### Step-by-Step Breakdown

#### 1. **Fetch Rules**
```java
List<WorkflowStepRule> rules = ruleRepository
    .findByStepDefinitionIdAndRuleTypeAndIsActiveTrue(stepDefinitionId, ruleType);
```
- Gets all active rules for the step
- Filters by rule type (AUTO_APPROVE, SKIP_STEP, etc.)

#### 2. **Sort by Priority**
```java
rules.sort(Comparator.comparing(WorkflowStepRule::getPriority).reversed());
```
- Higher priority rules evaluated first
- First matching rule wins (stops evaluation)

#### 3. **Parse Rule Expression**
```java
Map<String, Object> ruleConfig = objectMapper.readValue(
    rule.getRuleExpression(),
    new TypeReference<Map<String, Object>>() {}
);
String condition = (String) ruleConfig.get("condition");
Map<String, Object> actions = (Map<String, Object>) ruleConfig.get("actions");
```
- Parses JSON string into map
- Extracts condition (SpEL expression)
- Extracts actions (what to do if condition matches)

#### 4. **Create SpEL Evaluation Context**
```java
Map<String, Object> rootObject = new HashMap<>();
rootObject.put("variables", variables);  // From WorkItemVersion
rootObject.put("context", context);       // RuleContext object
rootObject.put("task", task);            // If available
rootObject.put("step", stepInstance);    // If available
// ... add individual variables for direct access
```
- Creates root object map
- Makes all data accessible in SpEL expressions

#### 5. **Evaluate Condition**
```java
Expression expression = expressionParser.parseExpression(condition);
Boolean matches = expression.getValue(evalContext, rootObject, Boolean.class);
```
- Parses SpEL expression
- Evaluates against root object
- Returns boolean result

#### 6. **Build Result**
```java
if (matches == true) {
    RuleExecutionResult result = buildResult(ruleConfig, ruleType);
    // Sets appropriate flags based on rule type
    return result;
}
```
- If condition matches, extracts actions
- Builds `RuleExecutionResult` with appropriate flags
- Returns result

---

## 🎬 How Rules Are Triggered

### Trigger Points

#### 1. **After Task Creation** (Auto-Approve)
**Location**: `WorkflowOrchestratorServiceImpl.evaluateAndApplyAutoApproveRules()`

**When**: Right after tasks are created for a step

**Flow**:
```
createTasksForStep()
  └─> evaluateAndApplyAutoApproveRules()
      └─> RuleContextBuilder.buildForStepInstance()
      └─> RuleEngineService.evaluateRules(AUTO_APPROVE)
      └─> If shouldAutoApprove == true:
          └─> Auto-approve all tasks
          └─> Evaluate step completion
```

#### 2. **After Manual Approval** (Auto-Approve Remaining)
**Location**: `WorkflowOrchestratorServiceImpl.handleApprovalDecision()`

**When**: After someone manually approves a task

**Flow**:
```
approvalTaskSM.approve()
  └─> RuleEngineService.evaluateRules(AUTO_APPROVE)
  └─> If shouldAutoApprove == true:
      └─> Auto-approve remaining pending tasks
```

#### 3. **Step Start** (Skip Step)
**Location**: `WorkflowOrchestratorServiceImpl.startWorkflow()`

**When**: Before starting a step

**Flow**:
```
stepInstanceSM.start()
  └─> RuleEngineService.evaluateRules(SKIP_STEP)
  └─> If shouldSkipStep == true:
      └─> Skip step (mark as completed)
```

#### 4. **Task Creation** (Route Approver)
**Location**: `TaskManagementServiceImpl.createTasksForStep()`

**When**: When creating tasks for approvers

**Flow**:
```
createTasksForStep()
  └─> RuleEngineService.evaluateRules(ROUTE_APPROVER)
  └─> If newApproverId != null:
      └─> Create task for new approver instead
```

#### 5. **SLA Check** (SLA Actions)
**Location**: Scheduled job or manual trigger

**When**: Periodically or on-demand

**Flow**:
```
checkSLA()
  └─> RuleEngineService.evaluateRules(SLA_ACTION)
  └─> If slaAction != null:
      └─> Execute SLA action (delegate, escalate, etc.)
```

---

## 📝 Examples

### Example 1: Auto-Approve Small Loans

**Rule Definition**:
```json
{
  "ruleName": "Auto-approve loans <= 30k",
  "ruleType": "AUTO_APPROVE",
  "ruleExpression": "{\"condition\": \"variables['loanAmount'] <= 30000\", \"actions\": {\"autoApprove\": true}}",
  "priority": 10
}
```

**Workflow Execution**:
1. User submits work item with `loanAmount: 25000`
2. Tasks created for step
3. Rule evaluated: `25000 <= 30000` → `true`
4. `shouldAutoApprove = true`
5. All tasks auto-approved
6. Step completed automatically

---

### Example 2: Route Large Loans to Senior Manager

**Rule Definition**:
```json
{
  "ruleName": "Route large loans to senior manager",
  "ruleType": "ROUTE_APPROVER",
  "ruleExpression": "{\"condition\": \"variables['loanAmount'] > 100000\", \"actions\": {\"newApproverId\": \"senior-manager-sarah\"}}",
  "priority": 10
}
```

**Workflow Execution**:
1. User submits work item with `loanAmount: 150000`
2. Tasks being created
3. Rule evaluated: `150000 > 100000` → `true`
4. `newApproverId = "senior-manager-sarah"`
5. Task created for senior manager instead of regular manager

---

### Example 3: Skip Step for Pre-Approved Customers

**Rule Definition**:
```json
{
  "ruleName": "Skip step for pre-approved",
  "ruleType": "SKIP_STEP",
  "ruleExpression": "{\"condition\": \"variables['preApproved'] == true\", \"actions\": {\"skipStep\": true}}",
  "priority": 20
}
```

**Workflow Execution**:
1. User submits work item with `preApproved: true`
2. Step about to start
3. Rule evaluated: `true == true` → `true`
4. `shouldSkipStep = true`
5. Step skipped (marked as completed)
6. Workflow moves to next step

---

## 🔧 Common Issues & Debugging

### Issue 1: Variables are null
**Symptom**: `EL1007E: Property or field 'variables' cannot be found on null`

**Cause**: WorkItemVersion doesn't exist or variables field is null

**Fix**: Ensure work item is submitted before workflow starts, or handle null in rule expression:
```json
{
  "condition": "variables != null && variables['loanAmount'] <= 30000"
}
```

### Issue 2: Rule not matching
**Symptom**: Rule exists but never matches

**Debug Steps**:
1. Check rule is active (`isActive = true`)
2. Check rule type matches (`ruleType = AUTO_APPROVE`)
3. Check priority (higher priority rules evaluated first)
4. Log the actual variables being evaluated
5. Test SpEL expression manually

### Issue 3: Wrong rule matching
**Symptom**: Wrong rule is being applied

**Cause**: Priority order - first matching rule wins

**Fix**: Adjust priorities so more specific rules have higher priority

---

## 📚 Key Takeaways

1. **Rules are stored as JSON strings** in the database
2. **SpEL expressions** are evaluated at runtime against `RuleContext`
3. **RuleContext** contains all workflow data (variables, entities, etc.)
4. **Rules are evaluated in priority order** - first match wins
5. **Rule types** determine what actions are available
6. **Rules are triggered** at specific points in workflow execution
7. **Variables come from WorkItemVersion** - always use active version

---

## 🔗 Related Files

- `rulesengine/service/SimpleRuleEngineService.java` - Main evaluation logic
- `rulesengine/service/RuleContextBuilder.java` - Builds context from entities
- `rulesengine/dto/RuleContext.java` - Context data structure
- `rulesengine/dto/RuleExecutionResult.java` - Result structure
- `rulesengine/entity/WorkflowStepRule.java` - Rule entity
- `workflow/service/WorkflowOrchestratorServiceImpl.java` - Triggers rule evaluation

---

This document should help you understand the complete flow and debug issues. If you need clarification on any part, let me know!
