package com.neighborshare.domain.repository;

import com.neighborshare.domain.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Optional<Dispute> findByBookingId(UUID bookingId);

    Page<Dispute> findByStatus(String status, Pageable pageable);

    Page<Dispute> findByCreatedById(UUID userId, Pageable pageable);

    List<Dispute> findByStatusOrderByCreatedAtAsc(String status);

    List<Dispute> findByStatusAndAssignedAdminIsNull(String status);

    List<Dispute> findByAssignedAdminIdAndStatusNot(UUID adminId, String status);

    long countByStatus(String status);

    default List<Dispute> findOpenDisputes() {
        return findByStatusOrderByCreatedAtAsc("open");
    }

    default List<Dispute> findUnassignedOpenDisputes() {
        return findByStatusAndAssignedAdminIsNull("open");
    }

    default List<Dispute> findUnresolvedDisputesByAdminId(UUID adminId) {
        return findByAssignedAdminIdAndStatusNot(adminId, "resolved");
    }

    default long countOpenDisputes() {
        return countByStatus("open");
    }
}
