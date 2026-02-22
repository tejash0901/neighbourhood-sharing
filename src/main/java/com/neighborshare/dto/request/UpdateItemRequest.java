package com.neighborshare.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItemRequest {

    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    private String description;

    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @DecimalMin(value = "0.0", inclusive = true, message = "pricePerHour must be >= 0")
    private BigDecimal pricePerHour;

    @DecimalMin(value = "0.0", inclusive = true, message = "pricePerDay must be >= 0")
    private BigDecimal pricePerDay;

    @DecimalMin(value = "0.0", inclusive = true, message = "depositAmount must be >= 0")
    private BigDecimal depositAmount;

    private Boolean isAvailable;

    private Integer maxConsecutiveDays;

    private List<String> images;

    private String currentCondition;

    private String damageNotes;
}
