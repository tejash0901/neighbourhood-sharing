package com.neighborshare.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnBookingRequest {
    private String returnNotes;
    private List<String> returnImages;
}
