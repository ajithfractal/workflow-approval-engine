package com.fractalhive.workflowcore.rulesengine.service;

import com.fractalhive.workflowcore.rulesengine.dto.RuleCreateRequest;
import com.fractalhive.workflowcore.rulesengine.entity.WorkflowStepRule;
import com.fractalhive.workflowcore.rulesengine.repository.WorkflowStepRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of WorkflowStepRuleService.
 */
@Service
public class WorkflowStepRuleServiceImpl implements WorkflowStepRuleService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStepRuleServiceImpl.class);

    private final WorkflowStepRuleRepository ruleRepository;

    public WorkflowStepRuleServiceImpl(WorkflowStepRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional
    public UUID createRule(RuleCreateRequest request, String createdBy) {
        WorkflowStepRule rule = new WorkflowStepRule();
        rule.setStepDefinitionId(request.getStepDefinitionId());
        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setRuleExpression(request.getRuleExpression());
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        rule.setDescription(request.getDescription());

        Timestamp now = Timestamp.from(Instant.now());
        rule.setCreatedAt(now);
        rule.setCreatedBy(createdBy);

        WorkflowStepRule saved = ruleRepository.save(rule);
        logger.info("Created rule: {} (ID: {}) for step definition: {}", 
                rule.getRuleName(), saved.getId(), request.getStepDefinitionId());
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowStepRule> getRulesByStepDefinition(UUID stepDefinitionId) {
        return ruleRepository.findByStepDefinitionId(stepDefinitionId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowStepRule getRule(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
    }

    @Override
    @Transactional
    public void updateRule(UUID ruleId, RuleCreateRequest request, String updatedBy) {
        WorkflowStepRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        // Update fields (stepDefinitionId cannot be changed)
        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setRuleExpression(request.getRuleExpression());
        rule.setPriority(request.getPriority() != null ? request.getPriority() : rule.getPriority());
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }
        rule.setDescription(request.getDescription());

        Timestamp now = Timestamp.from(Instant.now());
        rule.setUpdatedAt(now);
        rule.setUpdatedBy(updatedBy);

        ruleRepository.save(rule);
        logger.info("Updated rule: {} (ID: {})", rule.getRuleName(), ruleId);
    }

    @Override
    @Transactional
    public void deleteRule(UUID ruleId) {
        WorkflowStepRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        ruleRepository.delete(rule);
        logger.info("Deleted rule: {} (ID: {})", rule.getRuleName(), ruleId);
    }
}
