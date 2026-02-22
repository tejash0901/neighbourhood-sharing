package com.neighborshare.dto.response;

import com.neighborshare.domain.valueobject.BookingStatus;
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
public class BookingResponse {
    private UUID id;
    private UUID itemId;
    private UUID borrowerId;
    private UUID ownerId;
    private BookingStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer durationDays;
    private BigDecimal basePrice;
    private BigDecimal depositCollected;
    private BigDecimal platformFee;
    private BigDecimal totalAmount;
    private LocalDateTime paidAt;
    private LocalDateTime returnedAt;
    private String returnNotes;
    private String returnImages;
    private LocalDateTime createdAt;
}
