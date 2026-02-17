package com.fractalhive.workflowcore.rulesengine.service;

import com.fractalhive.workflowcore.rulesengine.dto.RuleContext;
import com.fractalhive.workflowcore.rulesengine.dto.RuleExecutionResult;
import com.fractalhive.workflowcore.rulesengine.enums.RuleType;

import java.util.UUID;

/**
 * Service interface for evaluating rules against workflow contexts.
 */
public interface RuleEngineService {

    /**
     * Evaluates rules for a step definition.
     *
     * @param stepDefinitionId the step definition ID
     * @param ruleType         the type of rules to evaluate
     * @param context          the rule context containing workflow data
     * @return the execution result with actions to take
     */
    RuleExecutionResult evaluateRules(UUID stepDefinitionId, RuleType ruleType, RuleContext context);

    /**
     * Evaluates a specific rule.
     *
     * @param ruleId  the rule ID
     * @param context the rule context containing workflow data
     * @return the execution result with actions to take
     */
    RuleExecutionResult evaluateRule(UUID ruleId, RuleContext context);
}
