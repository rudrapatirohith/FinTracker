package com.fintracker.coreapi.financial;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fintracker.coreapi.financial.entity.Income;
import com.fintracker.coreapi.financial.repository.IncomeRepository;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.repository.AppUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class IncomeRepositoryPostgresIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fintracker_financial_test")
            .withUsername("fintracker_test")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
            "spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver"
        );
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM app_users");
    }

    @Test
    void shouldRestrictIncomeQueriesToTheOwningUser() {
        // Arrange: create two independent application users.
        AppUser userA = appUserRepository.save(
            new AppUser(
                UUID.randomUUID(),
                "income.user.a@example.com",
                null
            )
        );

        AppUser userB = appUserRepository.save(
            new AppUser(
                UUID.randomUUID(),
                "income.user.b@example.com",
                null
            )
        );

        // Give each user two separate income records.
        Income userAIncome1 = incomeRepository.save(
            new Income(
                userA.getId(),
                "Salary",
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 1, 1)
            )
        );

        incomeRepository.save(
            new Income(
                userA.getId(),
                "Freelance",
                new BigDecimal("750.00"),
                LocalDate.of(2026, 1, 5)
            )
        );

        Income userBIncome = incomeRepository.save(
            new Income(
                userB.getId(),
                "Consulting",
                new BigDecimal("2500.00"),
                LocalDate.of(2026, 1, 3)
            )
        );

        // Act: query each user's paginated income collection.
        Page<Income> userAResults = incomeRepository.findAllByUserId(
            userA.getId(),
            PageRequest.of(0, 20)
        );

        Page<Income> userBResults = incomeRepository.findAllByUserId(
            userB.getId(),
            PageRequest.of(0, 20)
        );

        // Assert: each user sees only their own records.
        assertThat(userAResults.getContent())
            .hasSize(2)
            .allMatch(income -> income.getUserId().equals(userA.getId()));

        assertThat(userBResults.getContent())
            .hasSize(1)
            .allMatch(income -> income.getUserId().equals(userB.getId()));

        // Act and assert: the correct owner can retrieve their record.
        assertThat(
            incomeRepository.findByIdAndUserId(
                userAIncome1.getId(),
                userA.getId()
            )
        ).isPresent();

        // A different user cannot retrieve that record using the scoped query.
        assertThat(
            incomeRepository.findByIdAndUserId(
                userAIncome1.getId(),
                userB.getId()
            )
        ).isEmpty();

        // The owner of the second record can retrieve it.
        assertThat(
            incomeRepository.findByIdAndUserId(
                userBIncome.getId(),
                userB.getId()
            )
        ).isPresent();

        // The first user cannot retrieve the second user's record.
        assertThat(
            incomeRepository.findByIdAndUserId(
                userBIncome.getId(),
                userA.getId()
            )
        ).isEmpty();
    }
}