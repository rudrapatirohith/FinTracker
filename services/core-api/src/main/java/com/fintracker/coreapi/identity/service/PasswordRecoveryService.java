package com.fintracker.coreapi.identity.service;

import com.fintracker.coreapi.identity.dto.request.ForgotPasswordRequest;
import com.fintracker.coreapi.identity.dto.request.ResetPasswordRequest;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.PasswordResetToken;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import com.fintracker.coreapi.identity.repository.PasswordResetTokenRepository;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements single-use, expiring password recovery. Only token hashes are
 * stored; raw tokens exist only long enough to place them in the reset email.
 */
@Service
public class PasswordRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final RefreshSessionService refreshSessionService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String mailFrom;
    private final String resetUrl;
    private final Duration tokenTtl;

    public PasswordRecoveryService(
            AppUserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            RefreshSessionService refreshSessionService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${fintracker.mail.from:no-reply@fintracker.local}") String mailFrom,
            @Value("${fintracker.security.password-reset.frontend-url:http://localhost:3000/auth/reset-password}")
                    String resetUrl,
            @Value("${fintracker.security.password-reset.token-ttl:PT30M}") Duration tokenTtl) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.refreshSessionService = refreshSessionService;
        this.mailSenderProvider = mailSenderProvider;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
        this.resetUrl = resetUrl;
        this.tokenTtl = tokenTtl;
    }

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        if (mailHost == null || mailHost.isBlank() || mailSenderProvider.getIfAvailable() == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Password recovery email is not configured on the server.");
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AppUser user = users.findByEmailNormalized(normalizedEmail).orElse(null);
        if (user == null) {
            // Return the same accepted response for unknown accounts to prevent email enumeration.
            return;
        }

        String rawToken = createRawToken();
        tokens.deleteAllByUserId(user.getId());
        tokens.save(new PasswordResetToken(user.getId(), sha256(rawToken), Instant.now().plus(tokenTtl)));

        try {
            sendResetEmail(user, rawToken);
        } catch (MailException | jakarta.mail.MessagingException exception) {
            tokens.deleteAllByUserId(user.getId());
            // Do not expose whether the email exists or include the token in logs.
            log.error("Password recovery email could not be sent.");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokens.findByTokenHashForUpdate(sha256(request.token()))
                .orElseThrow(PasswordRecoveryService::invalidResetToken);

        if (!token.getExpiresAt().isAfter(Instant.now())) {
            tokens.deleteAllByUserId(token.getUserId());
            throw invalidResetToken();
        }

        AppUser user = users.findById(token.getUserId())
                .orElseThrow(PasswordRecoveryService::invalidResetToken);
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        refreshSessionService.revokeAllSessionsForUser(user.getId());

        // Removing every token for the account makes the link single-use and
        // invalidates any older recovery email that may still be in the inbox.
        tokens.deleteAllByUserId(user.getId());
    }

    private void sendResetEmail(AppUser user, String rawToken) throws jakarta.mail.MessagingException {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Password recovery email is not configured on the server.");
        }

        String link = resetUrl + (resetUrl.contains("?") ? "&" : "?")
                + "token=" + java.net.URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
        helper.setFrom(mailFrom);
        helper.setTo(user.getEmail());
        helper.setSubject("Reset your FinTracker password");
        helper.setText(
                "A password reset was requested for your FinTracker account.\n\n"
                        + "Use this one-time link within " + tokenTtl.toMinutes() + " minutes:\n"
                        + link + "\n\n"
                        + "If you did not request this change, you can ignore this email.",
                false);
        mailSender.send(message);
    }

    private static String createRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private static ResponseStatusException invalidResetToken() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "This password reset link is invalid or has expired.");
    }
}
