package com.fintech.system.common.exception;

public class MissingParameter extends RuntimeException {

    public MissingParameter() {
    }

    public MissingParameter(String message) {
        super(message);
    }
}
