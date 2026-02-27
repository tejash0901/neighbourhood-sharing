package com.neighborshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.config.JwtProvider;
import com.neighborshare.dto.request.CreateItemRequest;
import com.neighborshare.dto.request.UpdateItemRequest;
import com.neighborshare.dto.response.ItemResponse;
import com.neighborshare.exception.GlobalExceptionHandler;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    void createItem_returns200_whenServiceSucceeds() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID apartmentId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(
            "Drill",
            "Cordless drill",
            "Tools",
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(50),
            true,
            3,
            java.util.List.of("https://example.com/i.jpg"),
            "good",
            ""
        );

        ItemResponse response = ItemResponse.builder()
            .id(itemId)
            .name("Drill")
            .category("Tools")
            .pricePerHour(BigDecimal.valueOf(10))
            .pricePerDay(BigDecimal.valueOf(100))
            .build();

        when(itemService.createItem(eq(userId), eq(apartmentId), any(CreateItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/items")
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .requestAttr("apartmentId", apartmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(itemId.toString()))
            .andExpect(jsonPath("$.name").value("Drill"));
    }

    @Test
    void createItem_returns400_whenValidationFails() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID apartmentId = UUID.randomUUID();

        mockMvc.perform(post("/v1/items")
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .requestAttr("apartmentId", apartmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createItem_returns401_whenApartmentContextMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateItemRequest request = new CreateItemRequest(
            "Drill",
            "Cordless drill",
            "Tools",
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(50),
            true,
            3,
            java.util.List.of("https://example.com/i.jpg"),
            "good",
            ""
        );

        mockMvc.perform(post("/v1/items")
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void patchItem_returns401_whenServiceThrowsUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID apartmentId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UpdateItemRequest request = new UpdateItemRequest();
        request.setPricePerDay(BigDecimal.valueOf(120));

        when(itemService.updateItem(eq(userId), eq(apartmentId), eq(itemId), any(UpdateItemRequest.class)))
            .thenThrow(new UnauthorizedException("You are not allowed to update this item"));

        mockMvc.perform(patch("/v1/items/{itemId}", itemId)
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .requestAttr("apartmentId", apartmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
