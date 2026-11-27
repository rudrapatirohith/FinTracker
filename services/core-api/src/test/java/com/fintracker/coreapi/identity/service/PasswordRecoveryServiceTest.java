package com.fintracker.coreapi.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fintracker.coreapi.identity.dto.request.ForgotPasswordRequest;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import com.fintracker.coreapi.identity.repository.PasswordResetTokenRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class PasswordRecoveryServiceTest {

    @Test
    void refusesRecoveryBeforeLookingUpAnAccountWhenSmtpIsNotConfigured() {
        AppUserRepository users = mock(AppUserRepository.class);
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshSessionService refreshSessionService = mock(RefreshSessionService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        PasswordRecoveryService service = new PasswordRecoveryService(
                users,
                tokens,
                passwordEncoder,
                refreshSessionService,
                mailSenderProvider,
                "",
                "no-reply@example.test",
                "http://localhost:3000/auth/reset-password",
                Duration.ofMinutes(30));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requestReset(new ForgotPasswordRequest("user@example.test")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        verifyNoInteractions(users, tokens, passwordEncoder, refreshSessionService);
    }
}
