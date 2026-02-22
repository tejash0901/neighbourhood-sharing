package com.neighborshare.service;

import com.neighborshare.config.JwtProvider;
import com.neighborshare.domain.entity.Apartment;
import com.neighborshare.domain.entity.User;
import com.neighborshare.domain.repository.ApartmentRepository;
import com.neighborshare.domain.repository.UserRepository;
import com.neighborshare.dto.request.LoginRequest;
import com.neighborshare.dto.request.RegisterRequest;
import com.neighborshare.dto.request.SendOtpRequest;
import com.neighborshare.dto.request.VerifyOtpRequest;
import com.neighborshare.dto.response.ApiMessageResponse;
import com.neighborshare.dto.response.AuthResponse;
import com.neighborshare.exception.ResourceNotFoundException;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${jwt.expiration}")
    private long jwtExpirationSeconds;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Apartment apartment = apartmentRepository.findByInviteCode(request.getInviteCode())
            .orElseThrow(() -> new ResourceNotFoundException("Apartment", request.getInviteCode()));

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email is already registered");
        }

        User user = User.builder()
            .email(request.getEmail().trim().toLowerCase())
            .phone(request.getPhone())
            .apartment(apartment)
            .firstName(request.getFirstName().trim())
            .lastName(request.getLastName().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Your account is inactive");
        }

        if (user.isBanned()) {
            throw new UnauthorizedException("Your account is temporarily suspended");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public ApiMessageResponse sendOtp(SendOtpRequest request) {
        return userRepository.findByEmail(request.getEmail().trim().toLowerCase())
            .map(user -> {
                String otpCode = generateOtpCode();
                user.setOtpSecret(otpCode);
                userRepository.save(user);
                return ApiMessageResponse.builder()
                    .message("OTP generated (development mode): " + otpCode)
                    .build();
            })
            .orElseGet(() -> ApiMessageResponse.builder()
                .message("If this email exists, an OTP has been generated.")
                .build());
    }

    @Transactional
    public ApiMessageResponse verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or OTP"));

        if (user.getOtpSecret() == null || !user.getOtpSecret().equals(request.getOtpCode())) {
            throw new UnauthorizedException("Invalid email or OTP");
        }

        user.setVerifiedAt(LocalDateTime.now());
        user.setOtpSecret(null);
        userRepository.save(user);

        return ApiMessageResponse.builder()
            .message("OTP verified successfully")
            .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtProvider.generateAccessToken(user.getId(), user.getApartment().getId(), "ROLE_USER");
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getApartment().getId());

        return AuthResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .token(token)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpirationSeconds)
            .build();
    }

    private String generateOtpCode() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }
}
