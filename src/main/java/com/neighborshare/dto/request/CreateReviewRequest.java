package com.neighborshare.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "bookingId is required")
    private UUID bookingId;

    @NotNull(message = "rating is required")
    @DecimalMin(value = "1.0", message = "rating must be >= 1")
    @DecimalMax(value = "5.0", message = "rating must be <= 5")
    private BigDecimal rating;

    private String title;
    private String content;
}
