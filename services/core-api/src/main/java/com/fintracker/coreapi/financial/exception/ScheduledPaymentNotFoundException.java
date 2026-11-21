package com.fintracker.coreapi.financial.exception;

/** Used for absent and non-owned payment IDs to avoid exposing other tenants' data. */
public class ScheduledPaymentNotFoundException extends RuntimeException {
    /**
     * Creates this domain exception with the stable, client-safe not-found message used by the global exception handler.
     */
    public ScheduledPaymentNotFoundException() {
        super("Scheduled payment not found.");
    }
}
