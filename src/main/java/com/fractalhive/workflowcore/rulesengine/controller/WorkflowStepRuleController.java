package com.fractalhive.workflowcore.rulesengine.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fractalhive.keycloak.util.SecurityUtils;
import com.fractalhive.workflowcore.rulesengine.dto.CreateRuleResponse;
import com.fractalhive.workflowcore.rulesengine.dto.RuleCreateRequest;
import com.fractalhive.workflowcore.rulesengine.dto.RuleResponse;
import com.fractalhive.workflowcore.rulesengine.service.WorkflowStepRuleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for managing workflow step rules.
 */
@RestController
@RequestMapping("/api/workflow-definitions/steps")
@Tag(name = "Workflow Step Rules", description = "APIs for creating and managing rules for workflow steps")
public class WorkflowStepRuleController {

    private final WorkflowStepRuleService ruleService;

    public WorkflowStepRuleController(WorkflowStepRuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * Creates a new rule for a workflow step.
     *
     * @param request          the rule creation request
     * @return the created rule ID
     */
    @PostMapping("/rules")
    @Operation(
            summary = "Create a rule for a workflow step",
            description = "Creates a new rule (auto-approve, skip step, route approver, etc.) for a workflow step. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rule created successfully",
                    content = @Content(schema = @Schema(implementation = CreateRuleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Step definition not found")
    })
    public ResponseEntity<CreateRuleResponse> createRule(
            @Parameter(description = "Rule creation request")
            @Valid @RequestBody RuleCreateRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID ruleId = ruleService.createRule(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateRuleResponse.builder().ruleId(ruleId).build());
    }

    /**
     * Gets all rules for a workflow step.
     *
     * @param stepDefinitionId the step definition ID
     * @return list of rules
     */
    @GetMapping("/{stepDefinitionId}/rules")
    @Operation(
            summary = "Get all rules for a workflow step",
            description = "Retrieves all rules (active and inactive) for a specific workflow step"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved rules",
                    content = @Content(schema = @Schema(implementation = RuleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Step definition not found")
    })
    public ResponseEntity<List<RuleResponse>> getRules(
            @Parameter(description = "The step definition ID", required = true)
            @PathVariable UUID stepDefinitionId) {
        List<RuleResponse> rules = ruleService.getRulesByStepDefinition(stepDefinitionId).stream()
                .map(RuleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }

    /**
     * Gets a rule by ID.
     *
     * @param ruleId the rule ID
     * @return the rule
     */
    @GetMapping("/rules/{ruleId}")
    @Operation(
            summary = "Get a rule by ID",
            description = "Retrieves a specific rule by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved rule",
                    content = @Content(schema = @Schema(implementation = RuleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<RuleResponse> getRule(
            @Parameter(description = "The rule ID", required = true)
            @PathVariable UUID ruleId) {
        RuleResponse rule = RuleResponse.fromEntity(ruleService.getRule(ruleId));
        return ResponseEntity.ok(rule);
    }

    /**
     * Updates a rule.
     *
     * @param ruleId    the rule ID
     * @param request   the update request
     * @return no content
     */
    @PutMapping("/rules/{ruleId}")
    @Operation(
            summary = "Update a rule",
            description = "Updates an existing rule. Note: stepDefinitionId cannot be changed. Username is extracted from JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<Void> updateRule(
            @Parameter(description = "The rule ID", required = true)
            @PathVariable UUID ruleId,
            @Parameter(description = "Rule update request")
            @Valid @RequestBody RuleCreateRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        ruleService.updateRule(ruleId, request, updatedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a rule.
     *
     * @param ruleId the rule ID
     * @return no content
     */
    @DeleteMapping("/rules/{ruleId}")
    @Operation(
            summary = "Delete a rule",
            description = "Deletes a rule from a workflow step"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "The rule ID", required = true)
            @PathVariable UUID ruleId) {
        ruleService.deleteRule(ruleId);
        return ResponseEntity.ok().build();
    }
}
