/**
 * Domain exception representing either a missing monthly expense or an expense outside the caller's ownership scope.
 * Returning the same not-found outcome prevents the API from leaking cross-user record existence.
 */
package com.fintracker.coreapi.financial.exception;

/** Also used when an ID belongs to another user, avoiding ownership disclosure. */
public class MonthlyExpenseNotFoundException extends RuntimeException {
    public MonthlyExpenseNotFoundException() {
        super("Monthly expense record not found.");
    }
}
