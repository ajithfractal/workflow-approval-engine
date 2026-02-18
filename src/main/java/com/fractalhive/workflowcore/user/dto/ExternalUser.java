package com.fractalhive.workflowcore.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a user from external application API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalUser {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("role")
    private String role;

    @JsonProperty("manager_id")
    private String managerId;
}
