package com.fractalhive.workflowcore.approval.repository;

import com.fractalhive.workflowcore.approval.entity.ApprovalComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ApprovalComment entities.
 */
@Repository
public interface ApprovalCommentRepository extends JpaRepository<ApprovalComment, UUID> {

    /**
     * Find all comments for an approval task, ordered by comment time ascending.
     *
     * @param approvalTaskId the approval task ID
     * @return list of approval comments
     */
    List<ApprovalComment> findByApprovalTaskIdOrderByCommentedAtAsc(UUID approvalTaskId);

    /**
     * Find all comments by commenter.
     *
     * @param commentedBy the commenter ID
     * @return list of approval comments
     */
    List<ApprovalComment> findByCommentedBy(String commentedBy);
}
