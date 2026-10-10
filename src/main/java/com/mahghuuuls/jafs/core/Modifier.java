package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;

/** One property change inside a food definition. */
public final class Modifier {

    private final String propertyId;
    private final PropertyOperation operation;
    private final double amount;

    public Modifier(String propertyId, PropertyOperation operation, double amount) {
        if (propertyId == null || propertyId.isEmpty() || operation == null) {
            throw new IllegalArgumentException("propertyId and operation are required");
        }
        this.propertyId = propertyId;
        this.operation = operation;
        this.amount = amount;
    }

    public String propertyId() {
        return propertyId;
    }

    public PropertyOperation operation() {
        return operation;
    }

    public double amount() {
        return amount;
    }

    @Override
    public String toString() {
        return propertyId + " " + operation.key() + " " + amount;
    }
}
