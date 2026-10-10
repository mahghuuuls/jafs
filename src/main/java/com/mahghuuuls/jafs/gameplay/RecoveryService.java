package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.config.GameplaySettings;
import com.mahghuuuls.jafs.core.RecoveryRules;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.property.EffectiveRates;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Delayed recovery: a hit restarts the delay, the ticker counts it down and then heals through
 * {@code player.heal} so other mods see ordinary healing (REQ-042). The remaining delay lives in
 * the capability (REQ-041); the fractional carry and the last status are per-session only, so a
 * relog loses at most a fraction of one tick's healing. Diagnostic lines are written on status
 * changes and on hits, never per tick (REQ-073).
 */
public final class RecoveryService {

    private static final class Session {
        RecoveryRules.Status status;
        double carry;
    }

    private final EffectiveRates rates;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private RecoveryGate gate = RecoveryGate.NONE;

    public RecoveryService(EffectiveRates rates) {
        this.rates = rates;
    }

    public void setGate(RecoveryGate gate) {
        this.gate = gate == null ? RecoveryGate.NONE : gate;
    }

    /** A hit that counts (REQ-034) restarts the delay from the full configured value (REQ-033). */
    public void onDamage(EntityPlayerMP player, float amount) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null) {
            return;
        }
        int delaySeconds = GameplayConfig.current().recoveryDelaySeconds();
        state.setRecoveryDelayTicks(RecoveryRules.delayTicks(delaySeconds));
        Session session = session(player);
        session.carry = 0;
        session.status = RecoveryRules.Status.DELAY;
        Diag.event("recovery paused for {}: damage {} (delay {}s)", player.getName(), fmt(amount), delaySeconds);
    }

    /** One server tick, after the food timers. */
    public void tick(EntityPlayerMP player) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null) {
            return;
        }
        GameplaySettings settings = GameplayConfig.current();
        Session session = session(player);
        double perTick = RecoveryRules.healPerTick(settings.baseRecoveryRatePercent(), rates.recoveryMultiplier(player),
                player.getMaxHealth());
        RecoveryRules.Step step = RecoveryRules.step(state.recoveryDelayTicks(), player.getHealth(), player.getMaxHealth(),
                player.isSpectator(), gate.blocksRecovery(player), perTick, session.carry);
        if (step.delayTicks != state.recoveryDelayTicks()) {
            state.setRecoveryDelayTicks(step.delayTicks);
        }
        session.carry = step.carry;
        if (step.heal > 0) {
            player.heal(step.heal);
        }
        if (step.status != session.status) {
            logTransition(player, session.status, step.status, perTick);
            session.status = step.status;
        }
    }

    /** The current status for inspect, computed without advancing anything. */
    public RecoveryRules.Status status(EntityPlayerMP player) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null) {
            return null;
        }
        Session session = sessions.get(player.getUniqueID());
        if (session != null && session.status != null) {
            return session.status;
        }
        return RecoveryRules.step(state.recoveryDelayTicks(), player.getHealth(), player.getMaxHealth(), player.isSpectator(),
                gate.blocksRecovery(player), 1, 0).status;
    }

    /** The inspect fragment: {@code recovery=<status>} plus the delay left when counting. */
    public String describe(EntityPlayerMP player) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null) {
            return "recovery=unknown";
        }
        RecoveryRules.Status status = status(player);
        String text = "recovery=" + status.label();
        if (state.recoveryDelayTicks() > 0) {
            text += " delayLeft=" + fmt(state.recoveryDelayTicks() / (float) RecoveryRules.TICKS_PER_SECOND) + "s";
        }
        return text;
    }

    public void forget(EntityPlayerMP player) {
        sessions.remove(player.getUniqueID());
    }

    private Session session(EntityPlayerMP player) {
        Session session = sessions.get(player.getUniqueID());
        if (session == null) {
            session = new Session();
            sessions.put(player.getUniqueID(), session);
        }
        return session;
    }

    private static void logTransition(EntityPlayerMP player, RecoveryRules.Status from, RecoveryRules.Status to, double perTick) {
        if (from == null && to != RecoveryRules.Status.HEALING) {
            // The first tick after login only seeds the status; a healthy player has no transition to report.
            return;
        }
        switch (to) {
            case HEALING:
                if (from == RecoveryRules.Status.DELAY || from == null) {
                    Diag.event("recovery started for {} ({} health/s)", player.getName(), fmt((float) (perTick * RecoveryRules.TICKS_PER_SECOND)));
                } else {
                    Diag.event("recovery resumed for {} ({} health/s)", player.getName(), fmt((float) (perTick * RecoveryRules.TICKS_PER_SECOND)));
                }
                break;
            case DELAY:
                // The hit itself logged the restart; a delay restored on login is silent.
                break;
            default:
                Diag.event("recovery paused for {}: {}", player.getName(), to.label());
                break;
        }
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
