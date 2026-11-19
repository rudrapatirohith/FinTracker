package com.fintracker.coreapi.financial.exception;

/** Used for absent and non-owned payment IDs to avoid exposing other tenants' data. */
public class ScheduledPaymentNotFoundException extends RuntimeException {
    public ScheduledPaymentNotFoundException() {
        super("Scheduled payment not found.");
    }
}
