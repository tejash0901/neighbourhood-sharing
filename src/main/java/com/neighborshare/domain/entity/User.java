package com.neighborshare.domain.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_apartment_id", columnList = "apartment_id"),
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(columnDefinition = "TEXT")
    private String profilePicUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 255)
    private String passwordHash;

    @Column(length = 100)
    private String otpSecret;

    @Column
    private LocalDateTime verifiedAt;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column
    @Builder.Default
    private Integer totalRatings = 0;

    @Column
    @Builder.Default
    private Integer totalBorrowedItems = 0;

    @Column
    @Builder.Default
    private Integer totalLentItems = 0;

    @Column
    @Builder.Default
    private Integer totalItemsReturnedOnTime = 0;

    @Column
    @Builder.Default
    private Integer trustScore = 50;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column
    @Builder.Default
    private Boolean isActive = true;

    @Column
    private LocalDateTime bannedUntil;

    @Column(columnDefinition = "TEXT")
    private String banReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    // Relationships
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Item> ownedItems = new HashSet<>();

    @OneToMany(mappedBy = "borrower", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Booking> borrowedBookings = new HashSet<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Booking> lentBookings = new HashSet<>();

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isBanned() {
        if (bannedUntil == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(bannedUntil);
    }
}
