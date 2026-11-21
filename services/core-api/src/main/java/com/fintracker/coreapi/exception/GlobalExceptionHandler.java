package com.fintracker.coreapi.exception;

import com.fintracker.coreapi.financial.exception.IncomeNotFoundException;
import com.fintracker.coreapi.financial.exception.MonthlyExpenseNotFoundException;
import com.fintracker.coreapi.financial.exception.ScheduledPaymentNotFoundException;
import com.fintracker.coreapi.financial.exception.TransactionNotFoundException;
import com.fintracker.coreapi.identity.exception.EmailAlreadyRegisteredException;
import com.fintracker.coreapi.identity.exception.InvalidAuthenticatedUserException;
import com.fintracker.coreapi.identity.exception.InvalidCredentialsException;
import com.fintracker.coreapi.identity.exception.InvalidOAuthHandoffException;
import com.fintracker.coreapi.identity.exception.InvalidRefreshTokenException;
import com.fintracker.coreapi.identity.exception.RefreshTokenReuseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized handling of REST API exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

/**
     * Converts the duplicate-email domain error into HTTP 409 Conflict, allowing clients to distinguish an existing account from an authentication failure.
     */
    @ExceptionHandler(EmailAlreadyRegisteredException.class)

    public ResponseEntity<ProblemDetail> handleDuplicateEmail(EmailAlreadyRegisteredException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());

        problem.setTitle("Account already exists");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

/**
     * Builds a client-safe HTTP 400 ProblemDetail from the first invalid request field; it does not expose the full binding internals.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)

    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed.");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);

        problem.setTitle("Validation failed");

        return ResponseEntity.badRequest().body(problem);
    }

/**
     * Returns HTTP 409 for a database constraint conflict while deliberately hiding SQL, table names, and driver-specific details.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)

    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The request conflicts with an existing database record.");

        problem.setTitle("Database constraint violation");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

/**
     * Maps invalid login credentials to HTTP 401 using the exception's public-facing message.
     */
    @ExceptionHandler(InvalidCredentialsException.class)

    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());

        problem.setTitle("Authentication failed");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

/**
     * Returns a generic HTTP 401 detail for an invalid or expired refresh token, avoiding disclosure of token-validation internals.
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)

    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Refresh token is invalid or expired.");
        return problem;
    }

/**
     * Returns an HTTP 401 session-revoked response when a previously rotated refresh token is presented again.
     */
    @ExceptionHandler(RefreshTokenReuseException.class)

    public ProblemDetail handleRefreshTokenReuse(RefreshTokenReuseException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Session revoked");
        problem.setDetail("Refresh token reuse detected. Please authenticate again.");
        return problem;
    }

/**
     * Maps a rejected one-time OAuth handoff code to HTTP 401.
     */
    @ExceptionHandler(InvalidOAuthHandoffException.class)

    public ResponseEntity<ProblemDetail> handleInvalidOAuthHandoff(InvalidOAuthHandoffException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());

        problem.setTitle("OAuth authentication failed");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

/**
     * Maps a missing or malformed authenticated application identity to HTTP 401.
     */
    @ExceptionHandler(InvalidAuthenticatedUserException.class)

    public ResponseEntity<ProblemDetail> handleInvalidAuthenticatedUser(InvalidAuthenticatedUserException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());

        problem.setTitle("Invalid authenticated identity");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

/**
     * Maps an absent or non-owned scheduled payment to HTTP 404 so record existence is not disclosed across tenants.
     */
    @ExceptionHandler(ScheduledPaymentNotFoundException.class)

    public ResponseEntity<ProblemDetail> handleScheduledPaymentNotFound(ScheduledPaymentNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Scheduled payment not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

/**
     * Maps an absent or non-owned monthly expense to HTTP 404.
     */
    @ExceptionHandler(MonthlyExpenseNotFoundException.class)

    public ResponseEntity<ProblemDetail> handleMonthlyExpenseNotFound(MonthlyExpenseNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Monthly expense record not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

/**
     * Maps an absent or non-owned transaction to HTTP 404.
     */
    @ExceptionHandler(TransactionNotFoundException.class)

    public ResponseEntity<ProblemDetail> handleTransactionNotFound(TransactionNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Transaction record not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

/**
     * Maps an absent or non-owned income record to HTTP 404.
     */
    @ExceptionHandler(IncomeNotFoundException.class)

    public ResponseEntity<ProblemDetail> handleIncomeNotFound(IncomeNotFoundException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());

        problem.setTitle("Income record not found");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
