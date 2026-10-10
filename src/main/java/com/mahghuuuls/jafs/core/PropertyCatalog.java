package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertySpec;

/** What the validator needs to know about registered properties, without the registry itself. */
public interface PropertyCatalog {

    /** The spec for a property id, or null when no provider registered it. */
    PropertySpec find(String propertyId);
}
