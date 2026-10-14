package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.api.ActiveFoodView;
import com.mahghuuuls.jafs.api.IFoodService;
import com.mahghuuuls.jafs.api.ModifierView;
import com.mahghuuuls.jafs.api.PaymentResult;
import com.mahghuuuls.jafs.api.PropertyValue;
import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.core.BonusMath;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.core.Modifier;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The {@link IFoodService} addons see: thin forwarding to the one service and the bonus applier. */
final class FoodServiceApi implements IFoodService {

    private final ActiveFoodService service;
    private final BonusApplier bonuses;

    FoodServiceApi(ActiveFoodService service, BonusApplier bonuses) {
        this.service = service;
        this.bonuses = bonuses;
    }

    @Override
    public List<ActiveFoodView> getActiveFoods(EntityPlayerMP player) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null || state.slots().isEmpty()) {
            return Collections.emptyList();
        }
        List<ActiveFoodView> views = new ArrayList<ActiveFoodView>(state.slots().size());
        for (FoodSlot slot : state.slots()) {
            views.add(view(slot, FoodDefinitionFile.current().get(slot.definitionId())));
        }
        return Collections.unmodifiableList(views);
    }

    static ActiveFoodView view(FoodSlot slot, FoodDefinition definition) {
        List<ModifierView> modifiers = new ArrayList<ModifierView>();
        if (definition != null) {
            for (Modifier modifier : definition.modifiers()) {
                modifiers.add(new ModifierView(modifier.propertyId(), modifier.operation(), modifier.amount()));
            }
        }
        return new ActiveFoodView(slot.definitionId(), slot.remainingTicks(), slot.fullTicks(), ActiveFoodService.isEligible(slot), modifiers);
    }

    @Override
    public PropertyValue getPropertyValue(EntityPlayerMP player, String propertyId) {
        BonusMath.Combined combined = bonuses.combinedFor(player).get(propertyId);
        return combined == null ? PropertyValue.ZERO : new PropertyValue(combined.flat(), combined.percent(), combined.contributions());
    }

    @Override
    public boolean canPay(EntityPlayerMP player, int seconds) {
        return service.canPay(player, seconds);
    }

    @Override
    public PaymentResult pay(EntityPlayerMP player, int seconds, String cause) {
        return service.pay(player, seconds, cause);
    }

    @Override
    public int clearAll(EntityPlayerMP player, String cause) {
        return service.clearAll(player, cause);
    }
}
