/**
 * Centralizes translation of application exceptions into stable HTTP ProblemDetail responses.
 *  * Keeping this mapping in one advice class prevents controllers from duplicating status-code logic and keeps internal exception types out of the public API contract.
 *  * Validation and persistence errors are deliberately returned without exposing SQL, stack traces, or sensitive implementation details.
 */
package com.fintracker.coreapi.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fintracker.coreapi.identity.exception.EmailAlreadyRegisteredException;
import com.fintracker.coreapi.identity.exception.InvalidAuthenticatedUserException;
import com.fintracker.coreapi.identity.exception.InvalidCredentialsException;
import com.fintracker.coreapi.identity.exception.InvalidOAuthHandoffException;
import com.fintracker.coreapi.identity.exception.InvalidRefreshTokenException;
import com.fintracker.coreapi.identity.exception.RefreshTokenReuseException;
import com.fintracker.coreapi.financial.exception.IncomeNotFoundException;
import com.fintracker.coreapi.financial.exception.TransactionNotFoundException;
import com.fintracker.coreapi.financial.exception.MonthlyExpenseNotFoundException;
import com.fintracker.coreapi.financial.exception.ScheduledPaymentNotFoundException;

/**
 * Centralized handling of REST API exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(
            EmailAlreadyRegisteredException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            exception.getMessage()
        );

        problem.setTitle("Account already exists");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Request validation failed.");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            message
        );

        problem.setTitle("Validation failed");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The request conflicts with an existing database record."
        );

        problem.setTitle("Database constraint violation");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }


    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            exception.getMessage()
        );

        problem.setTitle("Authentication failed");

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(problem);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(
        InvalidRefreshTokenException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Refresh token is invalid or expired.");
        return problem;
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    public ProblemDetail handleRefreshTokenReuse(
        RefreshTokenReuseException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Session revoked");
        problem.setDetail(
            "Refresh token reuse detected. Please authenticate again."
        );
        return problem;
    }

    @ExceptionHandler(InvalidOAuthHandoffException.class)
    public ResponseEntity<ProblemDetail> handleInvalidOAuthHandoff(
            InvalidOAuthHandoffException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage());

        problem.setTitle("OAuth authentication failed");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(InvalidAuthenticatedUserException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAuthenticatedUser(
            InvalidAuthenticatedUserException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            exception.getMessage()
        );

        problem.setTitle("Invalid authenticated identity");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    @ExceptionHandler(ScheduledPaymentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleScheduledPaymentNotFound(
            ScheduledPaymentNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Scheduled payment not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(MonthlyExpenseNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMonthlyExpenseNotFound(
            MonthlyExpenseNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Monthly expense record not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleTransactionNotFound(
            TransactionNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Transaction record not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(IncomeNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleIncomeNotFound(
            IncomeNotFoundException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.getMessage()
        );

        problem.setTitle("Income record not found");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
