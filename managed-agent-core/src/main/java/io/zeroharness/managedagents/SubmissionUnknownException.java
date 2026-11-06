package io.zeroharness.managedagents;

/** The server may have accepted this write. Inspect the known session before taking action. */
public final class SubmissionUnknownException extends ManagedAgentException {
    private final String sessionId;

    public SubmissionUnknownException(String sessionId, Throwable cause) {
        super("Submission outcome unknown; do not automatically resend", 0, null, cause);
        this.sessionId = sessionId;
    }

    /** Null if the session creation response itself was lost. */
    public String sessionId() { return sessionId; }
}
