package com.fintracker.coreapi.financial.exception;

/** Returned for both missing and non-owned transaction IDs to prevent IDOR disclosure. */
public class TransactionNotFoundException extends RuntimeException {
    /**
     * Creates this domain exception with the stable, client-safe not-found message used by the global exception handler.
     */
    public TransactionNotFoundException() {
        super("Transaction record not found.");
    }
}
