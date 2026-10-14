package com.mahghuuuls.jafs.api;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * The static facade addons use (ARC-009). JAFS binds the {@link Control} during its pre-init;
 * an addon that loads after JAFS ({@code required-after:jafs}) can register properties and
 * providers from its own pre-init on, until JAFS closes registration when loading completes.
 * {@link #bind} and {@link Control} are reserved for JAFS itself.
 */
public final class JafsApi {

    public static final String API_VERSION = "1.0";

    /** What JAFS provides behind the facade. Reserved for JAFS. */
    public interface Control {
        IFoodService foodService();

        void registerProperty(PropertySpec spec, IPropertyApplier applier);

        void registerRateModifierProvider(String providerId, IRateModifierProvider provider);

        void unregisterRateModifierProvider(String providerId);

        void markDirty(EntityPlayerMP player);

        boolean isRegistrationFrozen();
    }

    private static volatile Control control;

    private JafsApi() {
    }

    /** Reserved for JAFS: installs the implementation once. */
    public static void bind(Control implementation) {
        if (implementation == null) {
            throw new IllegalArgumentException("a control is required");
        }
        if (control != null) {
            throw new IllegalStateException("JAFS API already bound");
        }
        control = implementation;
    }

    private static Control control() {
        Control bound = control;
        if (bound == null) {
            throw new IllegalStateException("JAFS is not loaded yet; declare required-after:jafs and call the API from pre-init or later");
        }
        return bound;
    }

    /** The server-side food service. */
    public static IFoodService getFoodService() {
        return control().foodService();
    }

    /**
     * Registers a property food can modify, with the applier that receives the combined sums
     * (REQ-057). Refused with an exception naming the property once registration is closed.
     */
    public static void registerProperty(PropertySpec spec, IPropertyApplier applier) {
        control().registerProperty(spec, applier);
    }

    /** Registers (or replaces) a rate modifier provider under the addon's id (REQ-060). Allowed at any time. */
    public static void registerRateModifierProvider(String providerId, IRateModifierProvider provider) {
        control().registerRateModifierProvider(providerId, provider);
    }

    /** Withdraws a provider; affected players are recomputed on their next change or when marked dirty. */
    public static void unregisterRateModifierProvider(String providerId) {
        control().unregisterRateModifierProvider(providerId);
    }

    /** Recomputes the player's bonuses now (after a provider's contribution changed) and resyncs the client. */
    public static void markDirty(EntityPlayerMP player) {
        control().markDirty(player);
    }

    /** True once loading completed and {@link #registerProperty} is refused. */
    public static boolean isRegistrationFrozen() {
        return control().isRegistrationFrozen();
    }

    /** Whether JAFS has bound the facade yet (useful for soft dependencies). */
    public static boolean isBound() {
        return control != null;
    }
}
