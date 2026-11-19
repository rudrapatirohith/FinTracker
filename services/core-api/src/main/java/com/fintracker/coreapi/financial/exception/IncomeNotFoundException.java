package com.fintracker.coreapi.financial.exception;

public class IncomeNotFoundException extends RuntimeException {

    public IncomeNotFoundException() {
        super("Income record not found.");
    }
}