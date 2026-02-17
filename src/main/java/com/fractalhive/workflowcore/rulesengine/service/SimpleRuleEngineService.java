package com.fractalhive.workflowcore.rulesengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fractalhive.workflowcore.rulesengine.dto.RuleContext;
import com.fractalhive.workflowcore.rulesengine.dto.RuleExecutionResult;
import com.fractalhive.workflowcore.rulesengine.entity.WorkflowStepRule;
import com.fractalhive.workflowcore.rulesengine.enums.RuleType;
import com.fractalhive.workflowcore.rulesengine.enums.SlaAction;
import com.fractalhive.workflowcore.rulesengine.repository.WorkflowStepRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rule engine implementation using Spring Expression Language (SpEL).
 * Rules are defined as JSON with SpEL conditions and actions.
 * Example: {"condition": "variables['amount'] < 10000", "actions": {"autoApprove": true}}
 */
@Service
public class SimpleRuleEngineService implements RuleEngineService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleRuleEngineService.class);

    private final WorkflowStepRuleRepository ruleRepository;
    private final ExpressionParser expressionParser;
    private final ObjectMapper objectMapper;

    public SimpleRuleEngineService(WorkflowStepRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
        this.expressionParser = new SpelExpressionParser();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public RuleExecutionResult evaluateRules(UUID stepDefinitionId, RuleType ruleType, RuleContext context) {
        List<WorkflowStepRule> rules = ruleRepository
                .findByStepDefinitionIdAndRuleTypeAndIsActiveTrue(stepDefinitionId, ruleType);

        if (rules.isEmpty()) {
            logger.debug("No active {} rules found for step definition: {}", ruleType, stepDefinitionId);
            return RuleExecutionResult.builder()
                    .ruleMatched(false)
                    .build();
        }

        // Sort by priority (higher priority first)
        rules.sort(Comparator.comparing(WorkflowStepRule::getPriority).reversed());

        // Evaluate rules in priority order
        for (WorkflowStepRule rule : rules) {
            RuleExecutionResult result = evaluateRule(rule, context);
            if (result.getRuleMatched()) {
                logger.debug("Rule matched: {} (ID: {})", rule.getRuleName(), rule.getId());
                return result;
            }
        }

        return RuleExecutionResult.builder()
                .ruleMatched(false)
                .build();
    }

    @Override
    public RuleExecutionResult evaluateRule(UUID ruleId, RuleContext context) {
        WorkflowStepRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        return evaluateRule(rule, context);
    }

    private RuleExecutionResult evaluateRule(WorkflowStepRule rule, RuleContext context) {
        try {
            // Parse rule expression as JSON
            Map<String, Object> ruleConfig = objectMapper.readValue(
                    rule.getRuleExpression(),
                    new TypeReference<Map<String, Object>>() {});

            // Extract condition
            String condition = (String) ruleConfig.get("condition");
            if (condition == null || condition.isBlank()) {
                logger.warn("Rule {} has no condition, skipping", rule.getId());
                return RuleExecutionResult.builder().ruleMatched(false).build();
            }

            // Evaluate condition using SpEL
            Expression expression = expressionParser.parseExpression(condition);
            EvaluationContextResult contextResult = createEvaluationContext(context);
            // Pass root object explicitly to ensure SpEL evaluates against the intended context
            // This ensures variables['loanAmount'] correctly resolves as a map entry
            Boolean matches = expression.getValue(contextResult.getEvalContext(), contextResult.getRootObject(), Boolean.class);

            if (Boolean.TRUE.equals(matches)) {
                // Rule matched, extract actions
                return buildResult(ruleConfig, rule.getRuleType());
            }

            return RuleExecutionResult.builder().ruleMatched(false).build();

        } catch (Exception e) {
            logger.error("Error evaluating rule {}: {}", rule.getId(), e.getMessage(), e);
            return RuleExecutionResult.builder().ruleMatched(false).build();
        }
    }

    /**
     * Helper class to hold both EvaluationContext and root object.
     */
    private static class EvaluationContextResult {
        private final EvaluationContext evalContext;
        private final Object rootObject;
        
        public EvaluationContextResult(EvaluationContext evalContext, Object rootObject) {
            this.evalContext = evalContext;
            this.rootObject = rootObject;
        }
        
        public EvaluationContext getEvalContext() {
            return evalContext;
        }
        
        public Object getRootObject() {
            return rootObject;
        }
    }
    
    /**
     * Wrapper class for SpEL root object that exposes variables and other properties.
     * This allows expressions like variables['loanAmount'] to work properly.
     * Exposes only flattened, primitive data - no JPA entities.
     * Uses dynamic property access via get(String) method instead of individual getters.
     */
    private static class RuleContextRoot {
        private final Map<String, Object> variables;
        private final RuleContext context;
        
        public RuleContextRoot(Map<String, Object> variables, RuleContext context) {
            this.variables = variables != null ? variables : Collections.emptyMap();
            this.context = context;
        }
        
        /**
         * Get variables map - accessible as variables['key'] in SpEL.
         */
        public Map<String, Object> getVariables() {
            return variables;
        }
        
        /**
         * Get context object - accessible as context.property in SpEL.
         */
        public RuleContext getContext() {
            return context;
        }
        
        /**
         * Dynamic property access for all context fields.
         * SpEL can call this method directly: get('workItemId')
         * First checks variables, then uses reflection to access RuleContext getters.
         * 
         * @param name the property name (e.g., "workItemId", "stepStatus", "loanAmount")
         * @return the property value or null if not found
         */
        public Object get(String name) {
            // First check variables
            if (variables.containsKey(name)) {
                return variables.get(name);
            }
            
            // Use reflection to access RuleContext getter
            if (context != null) {
                try {
                    String getterName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
                    Method getter = context.getClass().getMethod(getterName);
                    return getter.invoke(context);
                } catch (Exception e) {
                    // Property doesn't exist or reflection error
                    return null;
                }
            }
            
            return null;
        }
    }
    
    private EvaluationContextResult createEvaluationContext(RuleContext context) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        
        // PRIMARY: Variables - this is what rules are written around
        Map<String, Object> variables = context.getVariables() != null 
                ? context.getVariables() 
                : Collections.emptyMap();
        
        // Create wrapper class that exposes variables as a property
        // This allows expressions like variables['loanAmount'] to work properly
        RuleContextRoot rootObject = new RuleContextRoot(variables, context);
        
        // Set root object so expressions can access variables directly
        evalContext.setRootObject(rootObject);
        
        // PRIMARY: Set variables map - this is the main data for rules
        evalContext.setVariable("variables", variables);
        
        // Add context object for accessing other fields if needed
        evalContext.setVariable("context", context);
        
        // Allow direct access to variables (e.g., loanAmount instead of variables['loanAmount'])
        // This makes rule writing easier: loanAmount <= 30000 instead of variables['loanAmount'] <= 30000
        variables.forEach((key, value) -> {
            evalContext.setVariable(key, value);
        });
        
        // OPTIONAL: Add commonly used fields for SLA/status rules (only if present)
        // Most rules only need variables, but some may need these
        if (context.getElapsedHours() != null) {
            evalContext.setVariable("elapsedHours", context.getElapsedHours());
        }
        if (context.getHoursUntilDue() != null) {
            evalContext.setVariable("hoursUntilDue", context.getHoursUntilDue());
        }
        if (context.getHasParallelSteps() != null) {
            evalContext.setVariable("hasParallelSteps", context.getHasParallelSteps());
        }
        if (context.getStepStatus() != null) {
            evalContext.setVariable("stepStatus", context.getStepStatus());
        }
        if (context.getTaskStatus() != null) {
            evalContext.setVariable("taskStatus", context.getTaskStatus());
        }
        
        return new EvaluationContextResult(evalContext, rootObject);
    }

    @SuppressWarnings("unchecked")
    private RuleExecutionResult buildResult(Map<String, Object> ruleConfig, RuleType ruleType) {
        RuleExecutionResult.RuleExecutionResultBuilder builder = RuleExecutionResult.builder()
                .ruleMatched(true);

        Map<String, Object> actions = (Map<String, Object>) ruleConfig.get("actions");
        if (actions == null) {
            return builder.build();
        }

        switch (ruleType) {
            case AUTO_APPROVE:
                if (Boolean.TRUE.equals(actions.get("autoApprove"))) {
                    builder.shouldAutoApprove(true);
                }
                break;

            case SKIP_STEP:
                if (Boolean.TRUE.equals(actions.get("skipStep"))) {
                    builder.shouldSkipStep(true);
                }
                break;

            case ROUTE_APPROVER:
            case CONDITIONAL_ROUTING:
                String newApproverId = (String) actions.get("newApproverId");
                if (newApproverId != null) {
                    builder.newApproverId(newApproverId);
                }
                break;

            case SLA_ACTION:
                String slaAction = (String) actions.get("slaAction");
                if (slaAction != null) {
                    try {
                        builder.slaAction(SlaAction.valueOf(slaAction));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid SLA action: {}", slaAction);
                    }
                }
                String delegateTo = (String) actions.get("delegateTo");
                if (delegateTo != null) {
                    builder.newApproverId(delegateTo);
                }
                break;
        }

        // Add metadata if present
        Map<String, Object> metadata = (Map<String, Object>) actions.get("metadata");
        if (metadata != null) {
            builder.metadata(metadata);
        }

        return builder.build();
    }
}
