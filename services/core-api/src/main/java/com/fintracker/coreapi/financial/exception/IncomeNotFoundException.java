/**
 * Domain exception used when an income record is absent or is not owned by the authenticated user.
 * Treating both cases identically avoids confirming that another tenant's record ID exists; GlobalExceptionHandler converts this exception into HTTP 404.
 */
package com.fintracker.coreapi.financial.exception;

public class IncomeNotFoundException extends RuntimeException {

    public IncomeNotFoundException() {
        super("Income record not found.");
    }
}