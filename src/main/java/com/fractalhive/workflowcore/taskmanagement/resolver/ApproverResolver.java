package com.fractalhive.workflowcore.taskmanagement.resolver;

import java.util.List;

/**
 * Interface for resolving approvers based on role or manager chain.
 * Host applications should provide an implementation of this interface
 * to enable ROLE and MANAGER approver type resolution.
 */
public interface ApproverResolver {

    /**
     * Resolves a role to a list of user IDs.
     * 
     * @param role the role name
     * @return list of user IDs who have this role
     */
    List<String> resolveRoleToUserIds(String role);

    /**
     * Resolves manager chain for a user.
     * Returns the manager hierarchy starting from the direct manager.
     * 
     * @param userId the user ID
     * @return list of manager user IDs (direct manager, manager's manager, etc.)
     */
    List<String> resolveManagerChain(String userId);
}
