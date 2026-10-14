package com.mahghuuuls.jafs.api;

import java.util.Collections;
import java.util.List;

/**
 * What a duration payment did (REQ-048, REQ-049). A failed payment changed nothing; the addon
 * must not perform its paid action then (REQ-051).
 */
public final class PaymentResult {

    public enum Status {
        /** Every active food lost the cost; the listed foods expired. */
        SUCCESS,
        /** No active food had at least the cost left; nothing changed. */
        NOT_AFFORDABLE,
        /** The player has no active food; nothing changed. */
        NO_ACTIVE_FOOD,
        /** Called off the server thread or for a player without food state; nothing changed. */
        NOT_ON_SERVER
    }

    private final Status status;
    private final int requestedSeconds;
    private final List<String> expiredDefinitionIds;

    public PaymentResult(Status status, int requestedSeconds, List<String> expiredDefinitionIds) {
        this.status = status;
        this.requestedSeconds = requestedSeconds;
        this.expiredDefinitionIds = Collections.unmodifiableList(expiredDefinitionIds);
    }

    public static PaymentResult failed(Status status, int requestedSeconds) {
        return new PaymentResult(status, requestedSeconds, Collections.<String>emptyList());
    }

    public boolean success() {
        return status == Status.SUCCESS;
    }

    public Status status() {
        return status;
    }

    public int requestedSeconds() {
        return requestedSeconds;
    }

    /** The foods that reached zero through this payment and expired (with their consequences). */
    public List<String> expiredDefinitionIds() {
        return expiredDefinitionIds;
    }

    @Override
    public String toString() {
        return status + " " + requestedSeconds + "s" + (expiredDefinitionIds.isEmpty() ? "" : " expired=" + expiredDefinitionIds);
    }
}
