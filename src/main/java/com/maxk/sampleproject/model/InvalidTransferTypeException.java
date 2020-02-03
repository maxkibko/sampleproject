package com.maxk.sampleproject.model;

public class InvalidTransferTypeException extends RuntimeException {
    public InvalidTransferTypeException(String message) {
        super(message);
    }
}
