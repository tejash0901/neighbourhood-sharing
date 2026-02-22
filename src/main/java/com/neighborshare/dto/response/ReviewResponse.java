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
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private UUID reviewerId;
    private UUID reviewedUserId;
    private UUID itemId;
    private BigDecimal rating;
    private String title;
    private String content;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
}
