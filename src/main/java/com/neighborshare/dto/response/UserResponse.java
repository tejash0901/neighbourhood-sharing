package com.neighborshare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String profilePicUrl;
    private String bio;
    private BigDecimal averageRating;
    private Integer totalRatings;
    private Integer trustScore;
    private Integer totalBorrowedItems;
    private Integer totalLentItems;
    private LocalDateTime createdAt;
}
