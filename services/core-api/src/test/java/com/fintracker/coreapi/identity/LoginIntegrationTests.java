package com.fintracker.coreapi.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintracker.coreapi.identity.dto.request.LoginRequest;
import com.fintracker.coreapi.identity.dto.request.RegisterRequest;
import com.fintracker.coreapi.identity.entity.UserSession;
import com.fintracker.coreapi.identity.repository.UserSessionRepository;
import com.fintracker.coreapi.identity.service.RegistrationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    private UserSessionRepository userSessionRepository;

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
        registrationService.register(new RegisterRequest(EMAIL, PASSWORD, "Login Test User"));
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {

        createTestUser();

        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void shouldCreatePersistentSessionAndSetHttpOnlyCookieOnLogin() throws Exception {

        createTestUser();

        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("fintracker_refresh=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andReturn();

        // 1. Verify exactly one session row was persisted
        assertThat(userSessionRepository.count()).isEqualTo(1);

        List<UserSession> sessions = userSessionRepository.findAll();
        UserSession savedSession = sessions.get(0);

        // 2. Verify token hash format (64-char SHA-256 hex) & that raw cookie token is NOT stored
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(savedSession.getRefreshTokenHash()).isNotNull().hasSize(64).matches("^[0-9a-f]{64}$");
        assertThat(setCookieHeader).doesNotContain(savedSession.getRefreshTokenHash());

        // 3. Verify token family ID and ~30-day expiration
        assertThat(savedSession.getTokenFamilyId()).isNotNull();
        assertThat(savedSession.getExpiresAt())
                .isAfter(Instant.now().plus(Duration.ofDays(29)))
                .isBefore(Instant.now().plus(Duration.ofDays(31)));
    }

    @Test
    void shouldRejectIncorrectPassword() throws Exception {

        createTestUser();

        LoginRequest request = new LoginRequest(EMAIL, "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void shouldRejectUnknownEmail() throws Exception {

        LoginRequest request = new LoginRequest("unknown@example.com", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {

        LoginRequest request = new LoginRequest("not-an-email", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
