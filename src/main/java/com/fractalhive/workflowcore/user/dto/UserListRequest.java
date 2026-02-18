package com.fractalhive.workflowcore.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for listing users from external API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListRequest {

    private String search;
    private String role;
    private Integer limit;
}
