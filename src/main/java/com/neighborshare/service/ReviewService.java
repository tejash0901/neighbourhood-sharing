package com.neighborshare.service;

import com.neighborshare.domain.entity.Booking;
import com.neighborshare.domain.entity.Review;
import com.neighborshare.domain.entity.User;
import com.neighborshare.domain.repository.BookingRepository;
import com.neighborshare.domain.repository.ReviewRepository;
import com.neighborshare.domain.repository.UserRepository;
import com.neighborshare.domain.valueobject.BookingStatus;
import com.neighborshare.dto.request.CreateReviewRequest;
import com.neighborshare.dto.response.ReviewResponse;
import com.neighborshare.exception.InvalidStateException;
import com.neighborshare.exception.ResourceNotFoundException;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createReview(UUID reviewerId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
            .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId().toString()));

        boolean isBorrower = booking.getBorrower().getId().equals(reviewerId);
        boolean isOwner = booking.getOwner().getId().equals(reviewerId);
        if (!isBorrower && !isOwner) {
            throw new UnauthorizedException("You are not allowed to review this booking");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvalidStateException("Only completed bookings can be reviewed");
        }
        if (reviewRepository.existsByBookingIdAndReviewerId(booking.getId(), reviewerId)) {
            throw new ValidationException("You have already reviewed this booking");
        }

        User reviewedUser = isBorrower ? booking.getOwner() : booking.getBorrower();

        Review review = Review.builder()
            .booking(booking)
            .reviewer(isBorrower ? booking.getBorrower() : booking.getOwner())
            .reviewedUser(reviewedUser)
            .item(booking.getItem())
            .rating(request.getRating())
            .title(request.getTitle())
            .content(request.getContent())
            .build();

        Review saved = reviewRepository.save(review);

        if (isBorrower) {
            booking.setBorrowerRatingGiven(true);
            booking.setBorrowerRating(request.getRating());
            booking.setBorrowerReview(request.getContent());
        } else {
            booking.setOwnerRatingGiven(true);
            booking.setOwnerRating(request.getRating());
            booking.setOwnerReview(request.getContent());
        }
        bookingRepository.save(booking);

        refreshUserRating(reviewedUser.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listMyReviews(UUID userId, Pageable pageable) {
        return reviewRepository.findByReviewedUserIdOrderByHelpful(userId, pageable).map(this::toResponse);
    }

    private void refreshUserRating(UUID userId) {
        Double average = reviewRepository.findAverageRatingByUserId(userId);
        long count = reviewRepository.countByReviewedUserId(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        BigDecimal averageValue = average == null
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);

        user.setAverageRating(averageValue);
        user.setTotalRatings((int) count);
        userRepository.save(user);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
            .id(review.getId())
            .bookingId(review.getBooking().getId())
            .reviewerId(review.getReviewer().getId())
            .reviewedUserId(review.getReviewedUser().getId())
            .itemId(review.getItem().getId())
            .rating(review.getRating())
            .title(review.getTitle())
            .content(review.getContent())
            .helpfulCount(review.getHelpfulCount())
            .createdAt(review.getCreatedAt())
            .build();
    }
}
