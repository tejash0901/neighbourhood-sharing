package com.neighborshare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String token;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
