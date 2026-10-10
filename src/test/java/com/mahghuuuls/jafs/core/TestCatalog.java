package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;

import java.util.HashMap;
import java.util.Map;

/** The three JAFS properties with their shipped operations and limits, for validator tests. */
final class TestCatalog implements PropertyCatalog {

    private final Map<String, PropertySpec> specs = new HashMap<String, PropertySpec>();

    TestCatalog() {
        add(PropertySpec.builder("jafs:max_health").unit("HP").color("red")
                .operation(PropertyOperation.FLAT, -1000, 1000).operation(PropertyOperation.PERCENT, -100, 1000).build());
        add(PropertySpec.builder("jafs:recovery_rate").color("green").operation(PropertyOperation.PERCENT, -1000, 1000).build());
        add(PropertySpec.builder("jafs:depletion_rate").color("gold").operation(PropertyOperation.PERCENT, -1000, 1000).build());
    }

    TestCatalog add(PropertySpec spec) {
        specs.put(spec.id(), spec);
        return this;
    }

    @Override
    public PropertySpec find(String propertyId) {
        return specs.get(propertyId);
    }
}
