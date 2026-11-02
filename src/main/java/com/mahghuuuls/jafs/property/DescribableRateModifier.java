package com.mahghuuuls.jafs.property;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

/**
 * A rate-modifier provider that can name the parts of its contribution for operators, one text
 * per part (for example a Setworks instance id with its property and percent). Internal: a
 * provider registered through the public API may implement it, and {@link ExternalModifierBook}
 * adds the parts to its description when it does; the API itself does not require it.
 */
public interface DescribableRateModifier {

    /** The parts of this provider's contribution for the player, in a stable order; empty when none. */
    List<String> describeContributions(EntityPlayerMP player);
}
