package com.fintracker.coreapi.financial.exception;

/** Also used when an ID belongs to another user, avoiding ownership disclosure. */
public class MonthlyExpenseNotFoundException extends RuntimeException {
    public MonthlyExpenseNotFoundException() {
        super("Monthly expense record not found.");
    }
}
