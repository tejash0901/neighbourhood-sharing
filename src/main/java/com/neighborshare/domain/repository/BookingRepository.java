package com.neighborshare.domain.repository;

import com.neighborshare.domain.entity.Booking;
import com.neighborshare.domain.valueobject.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByBorrowerId(UUID borrowerId, Pageable pageable);

    Page<Booking> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Booking> findByBorrowerIdAndStatus(UUID borrowerId, BookingStatus status, Pageable pageable);

    Page<Booking> findByOwnerIdAndStatus(UUID ownerId, BookingStatus status, Pageable pageable);

    List<Booking> findByItemId(UUID itemId);

    List<Booking> findByItemIdAndStatusIn(UUID itemId, List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.item.id = :itemId AND " +
           "((b.startDate <= :endDate AND b.endDate >= :startDate) OR " +
           "(b.status = 'REQUESTED' AND b.createdAt > :withinLastHours)) " +
           "ORDER BY b.startDate ASC")
    List<Booking> findConflictingBookings(
        @Param("itemId") UUID itemId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("withinLastHours") LocalDateTime withinLastHours
    );

    List<Booking> findByBorrowerIdAndStatus(UUID userId, BookingStatus status);

    List<Booking> findByOwnerIdAndStatus(UUID userId, BookingStatus status);

    int countByItemIdAndStatus(UUID itemId, BookingStatus status);

    Optional<Booking> findByIdAndBorrowerId(UUID id, UUID borrowerId);

    Optional<Booking> findByIdAndOwnerId(UUID id, UUID ownerId);

    default List<Booking> findActiveBookingsByItemId(UUID itemId) {
        return findByItemIdAndStatusIn(
            itemId,
            List.of(BookingStatus.REQUESTED, BookingStatus.ACCEPTED, BookingStatus.ACTIVE)
        );
    }

    default List<Booking> findCompletedBorrowingsByUserId(UUID userId) {
        return findByBorrowerIdAndStatus(userId, BookingStatus.COMPLETED);
    }

    default List<Booking> findCompletedLendingsByUserId(UUID userId) {
        return findByOwnerIdAndStatus(userId, BookingStatus.COMPLETED);
    }

    default int countCompletedBookings(UUID itemId) {
        return countByItemIdAndStatus(itemId, BookingStatus.COMPLETED);
    }
}
