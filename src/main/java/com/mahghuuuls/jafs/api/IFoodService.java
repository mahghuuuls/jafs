package com.mahghuuuls.jafs.api;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

/**
 * The server-side food service for addons (REQ-059): read a player's active foods and combined
 * property values, check and request duration payments, clear. Every call is for the server
 * thread; a payment made elsewhere is refused with a result, never an exception.
 */
public interface IFoodService {

    /** The player's active foods in slot order; empty when none. */
    List<ActiveFoodView> getActiveFoods(EntityPlayerMP player);

    /** The combined flat and percent sums of a property over the player's active foods; zero when none. */
    PropertyValue getPropertyValue(EntityPlayerMP player, String propertyId);

    /** Whether a payment of the given seconds would succeed now (REQ-048). */
    boolean canPay(EntityPlayerMP player, int seconds);

    /**
     * Deducts the seconds from every active food at once when at least one food can cover the
     * whole cost (REQ-047, REQ-048); foods that reach zero expire with their consequences
     * (REQ-050). The cause names the addon's action in the diagnostic line.
     */
    PaymentResult pay(EntityPlayerMP player, int seconds, String cause);

    /** Removes every active food; returns how many were removed. */
    int clearAll(EntityPlayerMP player, String cause);
}
