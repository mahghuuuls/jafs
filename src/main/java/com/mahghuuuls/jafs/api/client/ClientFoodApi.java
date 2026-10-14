package com.mahghuuuls.jafs.api.client;

import com.mahghuuuls.jafs.api.ActiveFoodView;

import java.util.Collections;
import java.util.List;

/**
 * The client's view of its own active foods, as last sent by the server and counted down
 * locally, for addon HUDs. {@link #bind} and {@link Source} are reserved for JAFS.
 */
public final class ClientFoodApi {

    /** Reserved for JAFS. */
    public interface Source {
        List<ActiveFoodView> snapshot();
    }

    private static volatile Source source;

    private ClientFoodApi() {
    }

    /** Reserved for JAFS: installs the client source once. */
    public static void bind(Source implementation) {
        if (implementation == null) {
            throw new IllegalArgumentException("a source is required");
        }
        if (source != null) {
            throw new IllegalStateException("JAFS client API already bound");
        }
        source = implementation;
    }

    /** The local player's active foods with their current remaining ticks; empty before the first snapshot. */
    public static List<ActiveFoodView> getSnapshot() {
        Source bound = source;
        return bound == null ? Collections.<ActiveFoodView>emptyList() : bound.snapshot();
    }
}
