package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.api.IFoodService;
import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.api.IRateModifierProvider;
import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.property.ExternalModifierBook;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import net.minecraft.entity.player.EntityPlayerMP;

/** JAFS's side of the facade (ARC-009): forwards every call to the owning component; holds no state. */
final class ApiControl implements JafsApi.Control {

    private final IFoodService foodService;
    private final PropertyRegistry registry;
    private final ExternalModifierBook book;
    private final BonusApplier bonuses;
    private final SyncService sync;

    ApiControl(IFoodService foodService, PropertyRegistry registry, ExternalModifierBook book, BonusApplier bonuses, SyncService sync) {
        this.foodService = foodService;
        this.registry = registry;
        this.book = book;
        this.bonuses = bonuses;
        this.sync = sync;
    }

    @Override
    public IFoodService foodService() {
        return foodService;
    }

    @Override
    public void registerProperty(PropertySpec spec, IPropertyApplier applier) {
        registry.register(spec, applier);
    }

    @Override
    public void registerRateModifierProvider(String providerId, IRateModifierProvider provider) {
        book.register(providerId, provider);
    }

    @Override
    public void unregisterRateModifierProvider(String providerId) {
        book.unregister(providerId);
    }

    @Override
    public void markDirty(EntityPlayerMP player) {
        bonuses.recompute(player, "api");
        sync.markDirty(player);
    }

    @Override
    public boolean isRegistrationFrozen() {
        return registry.isFrozen();
    }
}
