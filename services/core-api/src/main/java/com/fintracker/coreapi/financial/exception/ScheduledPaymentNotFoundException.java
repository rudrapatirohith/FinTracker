/**
 * Domain exception for missing or non-owned scheduled payments.
 * The API deliberately does not distinguish these cases, preventing callers from probing IDs belonging to other users.
 */
package com.fintracker.coreapi.financial.exception;

/** Used for absent and non-owned payment IDs to avoid exposing other tenants' data. */
public class ScheduledPaymentNotFoundException extends RuntimeException {
    public ScheduledPaymentNotFoundException() {
        super("Scheduled payment not found.");
    }
}
