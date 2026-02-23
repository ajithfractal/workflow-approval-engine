package com.fractalhive.workflowcore.rulesengine.controller;

import static com.fractalhive.workflowcore.common.constants.ApiConstants.Common.INVALID_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.Common.NOT_FOUND;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.BAD_REQUEST_400;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.CREATED_201;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.NOT_FOUND_404;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.ResponseCodes.OK_200;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.DESC_CREATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.DESC_DELETE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.DESC_GET_BY_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.DESC_UPDATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.PARAM_CREATE_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.PARAM_UPDATE_REQUEST;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.RESP_CREATED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.RESP_DELETED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.RESP_UPDATED_SUCCESS;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.SUMMARY_CREATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.SUMMARY_DELETE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.SUMMARY_GET_BY_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowDefinition.SUMMARY_UPDATE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowStepRule.DESC_GET_ALL;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowStepRule.PARAM_RULE_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowStepRule.PARAM_STEP_DEFINITION_ID;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowStepRule.RESP_RETRIEVED_RULE;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowStepRule.RESP_RETRIEVED_RULES;
import static com.fractalhive.workflowcore.common.constants.ApiConstants.WorkflowStepRule.SUMMARY_GET_ALL;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = SUMMARY_CREATE,
            description = DESC_CREATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = RESP_CREATED_SUCCESS,
                    content = @Content(schema = @Schema(implementation = CreateRuleResponse.class))),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public CreateRuleResponse createRule(
            @Parameter(description = PARAM_CREATE_REQUEST)
            @Valid @RequestBody RuleCreateRequest request) {
        String createdBy = SecurityUtils.getCurrentUsername();
        UUID ruleId = ruleService.createRule(request, createdBy);
        return CreateRuleResponse.builder().ruleId(ruleId).build();
    }

    @GetMapping("/{stepDefinitionId}/rules")
    @Operation(
            summary = SUMMARY_GET_ALL,
            description = DESC_GET_ALL
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_RULES,
                    content = @Content(schema = @Schema(implementation = RuleResponse.class))),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public List<RuleResponse> getRules(
            @Parameter(description = PARAM_STEP_DEFINITION_ID, required = true)
            @PathVariable UUID stepDefinitionId) {
        return ruleService.getRulesByStepDefinition(stepDefinitionId).stream()
                .map(RuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/rules/{ruleId}")
    @Operation(
            summary = SUMMARY_GET_BY_ID,
            description = DESC_GET_BY_ID
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_RETRIEVED_RULE,
                    content = @Content(schema = @Schema(implementation = RuleResponse.class))),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public RuleResponse getRule(
            @Parameter(description = PARAM_RULE_ID, required = true)
            @PathVariable UUID ruleId) {
        return RuleResponse.fromEntity(ruleService.getRule(ruleId));
    }

    @PutMapping("/rules/{ruleId}")
    @Operation(
            summary = SUMMARY_UPDATE,
            description = DESC_UPDATE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_UPDATED_SUCCESS),
            @ApiResponse(responseCode = BAD_REQUEST_400, description = INVALID_REQUEST),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void updateRule(
            @Parameter(description = PARAM_RULE_ID, required = true)
            @PathVariable UUID ruleId,
            @Parameter(description = PARAM_UPDATE_REQUEST)
            @Valid @RequestBody RuleCreateRequest request) {
        String updatedBy = SecurityUtils.getCurrentUsername();
        ruleService.updateRule(ruleId, request, updatedBy);
    }

    @DeleteMapping("/rules/{ruleId}")
    @Operation(
            summary = SUMMARY_DELETE,
            description = DESC_DELETE
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = RESP_DELETED_SUCCESS),
            @ApiResponse(responseCode = NOT_FOUND_404, description = NOT_FOUND)
    })
    public void deleteRule(
            @Parameter(description = PARAM_RULE_ID, required = true)
            @PathVariable UUID ruleId) {
        ruleService.deleteRule(ruleId);
    }
}
