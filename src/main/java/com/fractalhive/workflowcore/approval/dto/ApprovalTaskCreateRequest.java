package com.fractalhive.workflowcore.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

import com.fractalhive.workflowcore.approval.enums.ApproverType;

/**
 * DTO for creating a new approval task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskCreateRequest {

    private UUID stepInstanceId;

    private String approverId;

    private ApproverType approverType;

    private Timestamp dueAt;
}
