package com.maxk.sampleproject.model;

public class InsufficientBalanceForTransferException extends RuntimeException {
    public InsufficientBalanceForTransferException(String message) {
        super(message);
    }
}
