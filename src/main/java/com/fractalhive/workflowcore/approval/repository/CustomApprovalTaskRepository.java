package com.fractalhive.workflowcore.approval.repository;

import com.fractalhive.workflowcore.approval.entity.ApprovalTask;
import com.fractalhive.workflowcore.approval.enums.ApproverType;
import com.fractalhive.workflowcore.approval.enums.TaskStatus;
import com.fractalhive.workflowcore.taskmanagement.dto.TaskSearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of CustomApprovalTaskRepository using JPA Criteria Builder.
 * Provides dynamic query building for task search with filters.
 */
@Repository
public class CustomApprovalTaskRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<ApprovalTask> searchTasks(TaskSearchRequest request, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Build count query
        Long totalElements = getTotalElements(request, cb);

        // Build data query with pagination
        List<ApprovalTask> content = getTaskResults(request, pageable, cb);

        return new PageImpl<>(content, pageable, totalElements);
    }

    private Long getTotalElements(TaskSearchRequest request, CriteriaBuilder cb) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ApprovalTask> root = countQuery.from(ApprovalTask.class);
        countQuery.select(cb.count(root));
        countQuery.where(buildPredicates(request, root, cb));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<ApprovalTask> getTaskResults(TaskSearchRequest request, Pageable pageable, CriteriaBuilder cb) {
        CriteriaQuery<ApprovalTask> query = cb.createQuery(ApprovalTask.class);
        Root<ApprovalTask> root = query.from(ApprovalTask.class);
        query.select(root);
        query.where(buildPredicates(request, root, cb));

        // Apply sorting
        applySorting(query, root, cb, pageable);

        TypedQuery<ApprovalTask> typedQuery = entityManager.createQuery(query);

        // Apply pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return typedQuery.getResultList();
    }

    private Predicate[] buildPredicates(TaskSearchRequest request, Root<ApprovalTask> root, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Required: approverId filter
        if (StringUtils.hasText(request.getApproverId())) {
            predicates.add(cb.equal(root.get("approverId"), request.getApproverId()));
        }

        // Optional: status filter
        if (request.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), request.getStatus()));
        }

        // Optional: search filter (matches approverId or approverType)
        if (StringUtils.hasText(request.getSearch())) {
            String searchPattern = "%" + request.getSearch().toLowerCase() + "%";
            Predicate approverIdMatch = cb.like(cb.lower(root.get("approverId")), searchPattern);
            
            // Try to match approverType enum
            Predicate approverTypeMatch = null;
            try {
                ApproverType approverType = ApproverType.valueOf(request.getSearch().toUpperCase());
                approverTypeMatch = cb.equal(root.get("approverType"), approverType);
            } catch (IllegalArgumentException e) {
                // Not a valid enum value, ignore
            }
            
            if (approverTypeMatch != null) {
                predicates.add(cb.or(approverIdMatch, approverTypeMatch));
            } else {
                predicates.add(approverIdMatch);
            }
        }

        // Optional: time range filter
        String timeCheckIn = StringUtils.hasText(request.getTimeCheckIn()) 
                ? request.getTimeCheckIn() 
                : "createdAt";
        
        if (request.getStartTime() != null || request.getEndTime() != null) {
            Path<Timestamp> timePath = root.get(timeCheckIn);
            
            if (request.getStartTime() != null) {
                Timestamp startTimestamp = new Timestamp(request.getStartTime());
                predicates.add(cb.greaterThanOrEqualTo(timePath, startTimestamp));
            }
            
            if (request.getEndTime() != null) {
                Timestamp endTimestamp = new Timestamp(request.getEndTime());
                predicates.add(cb.lessThanOrEqualTo(timePath, endTimestamp));
            }
        }

        return predicates.toArray(new Predicate[0]);
    }

    private void applySorting(CriteriaQuery<ApprovalTask> query, Root<ApprovalTask> root, 
                              CriteriaBuilder cb, Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                Path<?> path = root.get(sortOrder.getProperty());
                if (sortOrder.getDirection().isAscending()) {
                    orders.add(cb.asc(path));
                } else {
                    orders.add(cb.desc(path));
                }
            }
            query.orderBy(orders);
        }
    }
}
