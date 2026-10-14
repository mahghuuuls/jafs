package com.mahghuuuls.jafs.api;

/** One modifier of an active food as an addon sees it: the property, the operation, the amount. */
public final class ModifierView {

    private final String propertyId;
    private final PropertyOperation operation;
    private final double amount;

    public ModifierView(String propertyId, PropertyOperation operation, double amount) {
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
        return propertyId + " " + operation + " " + amount;
    }
}
