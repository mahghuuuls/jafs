package com.mahghuuuls.jafs.property;

import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.core.PropertyCatalog;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every property food can modify, JAFS's own and addons', keyed by id. Registration is open
 * during mod loading and closed when loading completes; a later registration throws.
 */
public final class PropertyRegistry implements PropertyCatalog {

    public static final String MAX_HEALTH = "jafs:max_health";
    public static final String RECOVERY_RATE = "jafs:recovery_rate";
    public static final String DEPLETION_RATE = "jafs:depletion_rate";

    private static final PropertyRegistry INSTANCE = new PropertyRegistry();

    private final Map<String, PropertySpec> specs = new LinkedHashMap<String, PropertySpec>();
    private final Map<String, IPropertyApplier> appliers = new LinkedHashMap<String, IPropertyApplier>();
    private final EffectiveRates rates = new EffectiveRates();
    private volatile boolean frozen;

    private PropertyRegistry() {
    }

    public static PropertyRegistry get() {
        return INSTANCE;
    }

    /** Registers JAFS's three properties. Called once in pre-init, before any addon can register. */
    public static void registerCoreProperties() {
        PropertyRegistry registry = get();
        registry.register(PropertySpec.builder(MAX_HEALTH)
                .displayNameKey("property.jafs.max_health").unit("HP").color("red")
                .operation(PropertyOperation.FLAT, -1000, 1000)
                .operation(PropertyOperation.PERCENT, -100, 1000)
                .build(), new MaxHealthApplier());
        registry.register(PropertySpec.builder(RECOVERY_RATE)
                .displayNameKey("property.jafs.recovery_rate").unit("").color("green")
                .operation(PropertyOperation.PERCENT, -1000, 1000)
                .build(), registry.rates.recoveryApplier());
        registry.register(PropertySpec.builder(DEPLETION_RATE)
                .displayNameKey("property.jafs.depletion_rate").unit("").color("gold")
                .operation(PropertyOperation.PERCENT, -1000, 1000)
                .build(), registry.rates.depletionApplier());
    }

    /** The rate holders the two rate properties write into. */
    public EffectiveRates rates() {
        return rates;
    }

    public synchronized void register(PropertySpec spec, IPropertyApplier applier) {
        if (applier == null) {
            throw new IllegalArgumentException("an applier is required for " + spec.id());
        }
        if (frozen) {
            throw new IllegalStateException("JAFS property registration closed when loading completed; register during pre-init, init, or post-init: " + spec.id());
        }
        if (specs.containsKey(spec.id())) {
            throw new IllegalArgumentException("property already registered: " + spec.id());
        }
        specs.put(spec.id(), spec);
        appliers.put(spec.id(), applier);
    }

    /** The applier of a registered property, or null. */
    public IPropertyApplier applier(String propertyId) {
        return appliers.get(propertyId);
    }

    public void freeze() {
        frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public PropertySpec find(String propertyId) {
        return specs.get(propertyId);
    }

    public Collection<PropertySpec> all() {
        return Collections.unmodifiableCollection(specs.values());
    }
}
