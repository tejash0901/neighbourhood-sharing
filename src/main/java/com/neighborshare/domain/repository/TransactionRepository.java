package com.neighborshare.domain.repository;

import com.neighborshare.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    Page<Transaction> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

    List<Transaction> findByBookingId(UUID bookingId);

    Optional<Transaction> findByStripeTransactionId(String stripeTransactionId);

    List<Transaction> findByUserIdAndStatusAndCreatedAtBetween(
        UUID userId,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = ?1 AND t.status = 'completed'")
    java.math.BigDecimal findTotalCompletedAmountByUserId(UUID userId);

    List<Transaction> findByStatusAndCreatedAtBefore(String status, LocalDateTime expiryTime);

    default List<Transaction> findCompletedTransactionsByUserAndDateRange(
        UUID userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return findByUserIdAndStatusAndCreatedAtBetween(userId, "completed", startDate, endDate);
    }

    default List<Transaction> findExpiredPendingTransactions(LocalDateTime expiryTime) {
        return findByStatusAndCreatedAtBefore("pending", expiryTime);
    }
}
