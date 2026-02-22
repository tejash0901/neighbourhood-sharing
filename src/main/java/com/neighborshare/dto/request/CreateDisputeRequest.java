package com.neighborshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDisputeRequest {

    @NotNull(message = "bookingId is required")
    private UUID bookingId;

    @NotBlank(message = "disputeReason is required")
    private String disputeReason;

    @NotBlank(message = "description is required")
    private String description;

    private List<String> evidence;
}
