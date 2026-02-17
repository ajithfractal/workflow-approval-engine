package com.fractalhive.workflowcore.rulesengine.repository;

import com.fractalhive.workflowcore.rulesengine.entity.WorkflowStepRule;
import com.fractalhive.workflowcore.rulesengine.enums.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkflowStepRule entities.
 */
@Repository
public interface WorkflowStepRuleRepository extends JpaRepository<WorkflowStepRule, UUID> {

    /**
     * Find all rules for a specific step definition.
     *
     * @param stepDefinitionId the step definition ID
     * @return list of rules for the step
     */
    List<WorkflowStepRule> findByStepDefinitionId(UUID stepDefinitionId);

    /**
     * Find all active rules of a specific type for a step definition.
     *
     * @param stepDefinitionId the step definition ID
     * @param ruleType         the rule type
     * @return list of active rules matching the type
     */
    List<WorkflowStepRule> findByStepDefinitionIdAndRuleTypeAndIsActiveTrue(
            UUID stepDefinitionId, RuleType ruleType);
}
