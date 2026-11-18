
package com.fintracker.coreapi.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests using a real PostgreSQL database.
 *
 * Testcontainers starts a temporary PostgreSQL instance.
 * Flyway applies the actual application migrations to that database.
 * These tests do not connect to the developer's local database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RegistrationPostgresIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fintracker_test")
            .withUsername("fintracker_test")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

        // Override the H2 test configuration with the temporary PostgreSQL database.
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
            "spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver"
        );

        // Run the real Flyway migrations instead of creating tables through Hibernate.
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Each test starts with an empty application-user table.
     * Foreign-key cascades clean up related profiles and sessions.
     */
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM app_users");
    }

    @Test
    void flywayShouldCreateIdentityTablesInPostgreSql() {

        Integer userTableExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_name = 'app_users'",
            Integer.class
        );

        Integer profileTableExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_name = 'user_profiles'",
            Integer.class
        );

        assertThat(userTableExists).isEqualTo(1);
        assertThat(profileTableExists).isEqualTo(1);
    }

    @Test
    void shouldRegisterUserAndPersistProfileInPostgreSql() throws Exception {

        String requestBody = """
            {
                "email": "fintracker.test@example.com",
                "password": "TestPassword123!",
                "fullName": "FinTracker Test"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.email")
                .value("fintracker.test@example.com"))
            .andExpect(jsonPath("$.fullName")
                .value("FinTracker Test"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify the user was actually persisted in PostgreSQL.
        Integer userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_users " +
            "WHERE email_normalized = ?",
            Integer.class,
            "fintracker.test@example.com"
        );

        assertThat(userCount).isEqualTo(1);

        // Verify the profile was persisted.
        Integer profileCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_profiles " +
            "WHERE full_name = ?",
            Integer.class,
            "FinTracker Test"
        );

        assertThat(profileCount).isEqualTo(1);

        // Verify the stored password is a hash, not the original password.
        String storedPasswordHash = jdbcTemplate.queryForObject(
            "SELECT password_hash FROM app_users " +
            "WHERE email_normalized = ?",
            String.class,
            "fintracker.test@example.com"
        );

        assertThat(storedPasswordHash).startsWith("$argon2id$");
        assertThat(passwordEncoder.matches(
            "TestPassword123!",
            storedPasswordHash
        )).isTrue();
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() throws Exception {

        String firstRequest = """
            {
                "email": "FinTracker.Test@example.com",
                "password": "TestPassword123!",
                "fullName": "First User"
            }
            """;

        String secondRequest = """
            {
                "email": "fintracker.test@example.com",
                "password": "AnotherPassword123!",
                "fullName": "Second User"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstRequest))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondRequest))
            .andExpect(status().isConflict());

        Integer userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_users " +
            "WHERE email_normalized = ?",
            Integer.class,
            "fintracker.test@example.com"
        );

        assertThat(userCount).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidRegistrationInput() throws Exception {

        String invalidRequest = """
            {
                "email": "not-an-email",
                "password": "short",
                "fullName": ""
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());

        Integer userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_users",
            Integer.class
        );

        assertThat(userCount).isZero();
    }
}
