package com.fintracker.coreapi.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintracker.coreapi.identity.dto.request.LoginRequest;
import com.fintracker.coreapi.identity.dto.request.RegisterRequest;
import com.fintracker.coreapi.identity.service.RegistrationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String EMAIL = "login.test@example.com";
    private static final String PASSWORD = "StrongPassword123!";

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM user_sessions");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM app_users");
    }

    private void createTestUser() {
        registrationService.register(
            new RegisterRequest(
                EMAIL,
                PASSWORD,
                "Login Test User"
            )
        );
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {

        createTestUser();

        LoginRequest request = new LoginRequest(
            EMAIL,
            PASSWORD
        );

        mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresInSeconds").value(900))
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.userId").isNotEmpty())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectIncorrectPassword() throws Exception {

        createTestUser();

        LoginRequest request = new LoginRequest(
            EMAIL,
            "WrongPassword123!"
        );

        mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail")
                .value("Invalid email or password."));
    }

    @Test
    void shouldRejectUnknownEmail() throws Exception {

        LoginRequest request = new LoginRequest(
            "unknown@example.com",
            PASSWORD
        );

        mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail")
                .value("Invalid email or password."));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {

        LoginRequest request = new LoginRequest(
            "not-an-email",
            ""
        );

        mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest());
    }
}