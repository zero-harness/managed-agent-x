package io.zeroharness.managedagents;

public class ManagedAgentException extends RuntimeException {
    private final int statusCode;
    private final String requestId;

    public ManagedAgentException(String message, int statusCode, String requestId, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.requestId = requestId;
    }

    public int statusCode() { return statusCode; }
    public String requestId() { return requestId; }
}
