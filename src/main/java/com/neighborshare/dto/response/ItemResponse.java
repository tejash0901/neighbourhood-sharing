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
public class ItemResponse {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;
    private BigDecimal depositAmount;
    private Boolean isAvailable;
    private Integer maxConsecutiveDays;
    private String images;
    private String currentCondition;
    private BigDecimal averageRating;
    private Integer totalBookings;
    private UserResponse owner;
    private LocalDateTime createdAt;
}
