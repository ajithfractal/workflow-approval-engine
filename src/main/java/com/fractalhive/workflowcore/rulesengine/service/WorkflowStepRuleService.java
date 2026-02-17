package com.fractalhive.workflowcore.rulesengine.service;

import com.fractalhive.workflowcore.rulesengine.dto.RuleCreateRequest;
import com.fractalhive.workflowcore.rulesengine.entity.WorkflowStepRule;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing workflow step rules.
 */
public interface WorkflowStepRuleService {

    /**
     * Creates a new rule for a workflow step.
     *
     * @param request the rule creation request
     * @param createdBy the user creating the rule
     * @return the created rule ID
     */
    UUID createRule(RuleCreateRequest request, String createdBy);

    /**
     * Gets all rules for a step definition.
     *
     * @param stepDefinitionId the step definition ID
     * @return list of rules
     */
    List<WorkflowStepRule> getRulesByStepDefinition(UUID stepDefinitionId);

    /**
     * Gets a rule by ID.
     *
     * @param ruleId the rule ID
     * @return the rule
     */
    WorkflowStepRule getRule(UUID ruleId);

    /**
     * Updates a rule.
     *
     * @param ruleId the rule ID
     * @param request the update request (same as create request)
     * @param updatedBy the user updating the rule
     */
    void updateRule(UUID ruleId, RuleCreateRequest request, String updatedBy);

    /**
     * Deletes a rule.
     *
     * @param ruleId the rule ID
     */
    void deleteRule(UUID ruleId);
}
