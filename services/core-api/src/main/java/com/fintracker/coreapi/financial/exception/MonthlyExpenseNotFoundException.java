package com.fintracker.coreapi.financial.exception;

/** Also used when an ID belongs to another user, avoiding ownership disclosure. */
public class MonthlyExpenseNotFoundException extends RuntimeException {
    /**
     * Creates this domain exception with the stable, client-safe not-found message used by the global exception handler.
     */
    public MonthlyExpenseNotFoundException() {
        super("Monthly expense record not found.");
    }
}
