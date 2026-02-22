package com.neighborshare.domain.entity;

import com.neighborshare.domain.valueobject.BookingStatus;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bookings_item_id", columnList = "item_id"),
    @Index(name = "idx_bookings_borrower_id", columnList = "borrower_id"),
    @Index(name = "idx_bookings_owner_id", columnList = "owner_id"),
    @Index(name = "idx_bookings_status", columnList = "status"),
    @Index(name = "idx_bookings_dates", columnList = "start_date, end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column
    private LocalDateTime statusUpdatedAt;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    // Generated column (SQL)
    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal depositCollected = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column
    private LocalDateTime paidAt;

    @Column(length = 255)
    private String paymentIntentId;

    @Column
    private LocalDateTime returnedAt;

    @Column
    @Builder.Default
    private Boolean damageReported = false;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal damageAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String returnNotes;

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    @Builder.Default
    private String returnImages = "[]";

    @Column
    @Builder.Default
    private Boolean borrowerRatingGiven = false;

    @Column(precision = 3, scale = 2)
    private BigDecimal borrowerRating;

    @Column(columnDefinition = "TEXT")
    private String borrowerReview;

    @Column
    @Builder.Default
    private Boolean ownerRatingGiven = false;

    @Column(precision = 3, scale = 2)
    private BigDecimal ownerRating;

    @Column(columnDefinition = "TEXT")
    private String ownerReview;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private Dispute dispute;

    // Helper methods
    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == BookingStatus.ACTIVE;
    }

    public boolean isPaid() {
        return paidAt != null;
    }

    public boolean canBeCancelled() {
        return status == BookingStatus.REQUESTED || status == BookingStatus.ACCEPTED;
    }

    public boolean canBeMarkedAsActive() {
        return status == BookingStatus.ACCEPTED && isPaid();
    }

    public boolean canBeReturned() {
        return status == BookingStatus.ACTIVE && LocalDateTime.now().isAfter(startDate);
    }

    public BigDecimal getTotalWithoutFees() {
        return basePrice.add(depositCollected);
    }
}
