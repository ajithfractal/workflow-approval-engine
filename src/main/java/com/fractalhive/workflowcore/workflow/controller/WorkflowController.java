package com.fractalhive.workflowcore.workflow.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fractalhive.workflowcore.approval.enums.DecisionType;
import com.fractalhive.keycloak.util.SecurityUtils;
import com.fractalhive.workflowcore.workflow.service.WorkflowOrchestratorService;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    @Autowired
    private WorkflowOrchestratorService orchestrator;

    @PostMapping("/start")
    public UUID startWorkflow(@RequestParam UUID workItemId, 
                              @RequestParam UUID workflowDefId) {
        String userId = SecurityUtils.getCurrentUsername();
        return orchestrator.startWorkflow(workItemId, workflowDefId, userId);
    }

    @PostMapping("/tasks/{taskId}/approve")
    public void approveTask(@PathVariable UUID taskId,
                           @RequestParam(required = false) String comments) {
        String userId = SecurityUtils.getCurrentUsername();
        orchestrator.handleApprovalDecision(taskId, userId, DecisionType.APPROVED, comments);
    }

    @PostMapping("/{instanceId}/cancel")
    public void cancelWorkflow(@PathVariable UUID instanceId) {
        String userId = SecurityUtils.getCurrentUsername();
        orchestrator.cancelWorkflow(instanceId, userId);
    }
}