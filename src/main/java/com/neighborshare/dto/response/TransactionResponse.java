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
public class TransactionResponse {
    private UUID id;
    private UUID bookingId;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String stripeTransactionId;
    private String status;
    private String description;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
