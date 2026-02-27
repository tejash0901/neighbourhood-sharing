package com.neighborshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.config.JwtProvider;
import com.neighborshare.dto.request.LoginRequest;
import com.neighborshare.dto.request.RegisterRequest;
import com.neighborshare.dto.response.AuthResponse;
import com.neighborshare.exception.GlobalExceptionHandler;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    void register_returns200_whenServiceSucceeds() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "newuser@example.com",
            "Pass@12345",
            "New",
            "User",
            "9999999999",
            "DEMO123"
        );

        AuthResponse response = AuthResponse.builder()
            .userId(UUID.randomUUID())
            .email("newuser@example.com")
            .firstName("New")
            .lastName("User")
            .token("access-token")
            .refreshToken("refresh-token")
            .tokenType("Bearer")
            .expiresIn(900L)
            .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_returns400_whenValidationFails() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void login_returns401_whenServiceRejectsCredentials() throws Exception {
        LoginRequest request = new LoginRequest("newuser@example.com", "wrong-password");
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
