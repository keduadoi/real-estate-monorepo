package com.realestate.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.auth.dto.request.LoginRequest;
import com.realestate.auth.dto.request.RegisterRequest;
import com.realestate.auth.dto.response.AuthResponse;
import com.realestate.auth.dto.response.UserResponse;
import com.realestate.auth.service.AuthService;
import com.realestate.auth.service.JwtService;
import com.realestate.auth.service.PasswordResetService;
import com.realestate.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private UserService userService;
    @MockBean private PasswordResetService passwordResetService;
    @MockBean private JwtService jwtService;

    @Test
    void register_validRequest_returnsAuthResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setEmail("test@example.com");
        userResponse.setRoles(List.of("ROLE_USER"));

        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", 900, userResponse);

        when(authService.register(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validRequest_returnsAuthResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setEmail("test@example.com");

        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", 900, userResponse);

        when(authService.login(any(), any(), any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }
}
