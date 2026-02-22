package com.neighborshare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentResponse {
    private String orderId;
    private String keyId;
    private Long amount;
    private String currency;
    private String status;
    private String bookingStatus;
}
