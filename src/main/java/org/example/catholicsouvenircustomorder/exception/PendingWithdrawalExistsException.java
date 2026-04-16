package org.example.catholicsouvenircustomorder.exception;

public class PendingWithdrawalExistsException extends RuntimeException {
    public PendingWithdrawalExistsException(String message) {
        super(message);
    }
}
