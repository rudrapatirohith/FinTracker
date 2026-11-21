package com.fintracker.coreapi.financial.exception;

/**
 * Represents a missing or non-owned income record; the API intentionally uses the same outcome for both cases to avoid leaking another user's data.
 */
public class IncomeNotFoundException extends RuntimeException {

    /**
     * Creates this domain exception with the stable, client-safe not-found message used by the global exception handler.
     */
    public IncomeNotFoundException() {
        super("Income record not found.");
    }
}
