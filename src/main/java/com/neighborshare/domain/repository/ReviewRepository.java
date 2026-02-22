package com.neighborshare.domain.repository;

import com.neighborshare.domain.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByItemId(UUID itemId);

    Page<Review> findByItemId(UUID itemId, Pageable pageable);

    Page<Review> findByReviewedUserId(UUID userId, Pageable pageable);

    List<Review> findByBookingId(UUID bookingId);

    boolean existsByBookingIdAndReviewerId(UUID bookingId, UUID reviewerId);

    Page<Review> findByReviewedUserIdOrderByHelpfulCountDesc(UUID userId, Pageable pageable);

    Double findAverageRatingByReviewedUserId(UUID userId);

    long countByReviewedUserId(UUID userId);

    default Page<Review> findByReviewedUserIdOrderByHelpful(UUID userId, Pageable pageable) {
        return findByReviewedUserIdOrderByHelpfulCountDesc(userId, pageable);
    }

    default Double findAverageRatingByUserId(UUID userId) {
        return findAverageRatingByReviewedUserId(userId);
    }
}
