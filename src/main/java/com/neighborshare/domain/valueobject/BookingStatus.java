package com.neighborshare.domain.valueobject;

public enum BookingStatus {
    REQUESTED,      // User created booking, awaiting owner approval
    ACCEPTED,       // Owner accepted, awaiting payment
    REJECTED,       // Owner or system rejected
    ACTIVE,         // Payment done, item is with borrower
    RETURNED,       // Item returned, owner confirming
    COMPLETED,      // Booking completed, ratings pending
    DISPUTED        // Dispute raised
}
