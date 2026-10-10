package com.mahghuuuls.jafs.core;

/** One validation finding, precise enough to locate the entry: definition id, field, message. */
public final class Problem {

    public enum Severity {
        /** The definition (or the matcher or modifier it names) is rejected. */
        ERROR,
        /** Kept, but the operator should know. */
        WARNING
    }

    private final Severity severity;
    private final String definitionId;
    private final String field;
    private final String message;

    public Problem(Severity severity, String definitionId, String field, String message) {
        this.severity = severity;
        this.definitionId = definitionId == null ? "?" : definitionId;
        this.field = field == null ? "" : field;
        this.message = message;
    }

    public static Problem error(String definitionId, String field, String message) {
        return new Problem(Severity.ERROR, definitionId, field, message);
    }

    public static Problem warning(String definitionId, String field, String message) {
        return new Problem(Severity.WARNING, definitionId, field, message);
    }

    public Severity severity() {
        return severity;
    }

    public String definitionId() {
        return definitionId;
    }

    public String field() {
        return field;
    }

    public String message() {
        return message;
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    @Override
    public String toString() {
        return severity.name().toLowerCase() + " in definition '" + definitionId + "'" + (field.isEmpty() ? "" : " field '" + field + "'")
                + ": " + message;
    }
}
