package com.mahghuuuls.jafs.api;

/** How a modifier changes a property: an absolute amount, or a percentage of the property's base. */
public enum PropertyOperation {
    FLAT("flat"),
    PERCENT("percent");

    private final String key;

    PropertyOperation(String key) {
        this.key = key;
    }

    /** The word used in food definition files. */
    public String key() {
        return key;
    }

    /** The operation for a definition-file word, or null when the word is unknown. */
    public static PropertyOperation fromKey(String word) {
        if (word == null) {
            return null;
        }
        for (PropertyOperation operation : values()) {
            if (operation.key.equalsIgnoreCase(word)) {
                return operation;
            }
        }
        return null;
    }
}
