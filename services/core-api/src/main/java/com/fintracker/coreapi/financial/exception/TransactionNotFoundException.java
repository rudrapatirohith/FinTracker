package com.fintracker.coreapi.financial.exception;

/** Returned for both missing and non-owned transaction IDs to prevent IDOR disclosure. */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Transaction record not found.");
    }
}
