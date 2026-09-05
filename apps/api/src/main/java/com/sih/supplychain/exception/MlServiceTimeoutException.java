package com.sih.supplychain.exception;

public class MlServiceTimeoutException extends MlServiceUnavailableException {

    public MlServiceTimeoutException(String message) {
        super(message);
    }

    public MlServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
