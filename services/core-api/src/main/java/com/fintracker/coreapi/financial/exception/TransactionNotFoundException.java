/**
 * Domain exception for a transaction that does not exist within the authenticated user's scope.
 * A foreign-owned ID produces the same exception as an unknown ID to prevent insecure direct object reference (IDOR) information disclosure.
 */
package com.fintracker.coreapi.financial.exception;

/** Returned for both missing and non-owned transaction IDs to prevent IDOR disclosure. */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Transaction record not found.");
    }
}
