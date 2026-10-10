package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.property.PropertyRegistry;

/** The server-side object graph: one service, one gate, one ticker, one sync, one bonus applier. Built in pre-init. */
public final class JafsGameplay {

    private static JafsGameplay instance;

    private final ActiveFoodService service;
    private final SyncService sync;
    private final MealGate gate;
    private final FoodTicker ticker;
    private final BonusApplier bonuses;

    private JafsGameplay() {
        this.service = new ActiveFoodService();
        this.sync = new SyncService();
        this.service.setSyncService(sync);
        this.bonuses = new BonusApplier(PropertyRegistry.get());
        this.service.setBonusHook(bonuses);
        this.gate = new MealGate(service);
        this.ticker = new FoodTicker(service, sync, PropertyRegistry.get().rates());
    }

    public static JafsGameplay create() {
        if (instance == null) {
            instance = new JafsGameplay();
        }
        return instance;
    }

    public static JafsGameplay get() {
        if (instance == null) {
            throw new IllegalStateException("JAFS gameplay has not been created");
        }
        return instance;
    }

    public ActiveFoodService service() {
        return service;
    }

    public SyncService sync() {
        return sync;
    }

    public MealGate gate() {
        return gate;
    }

    public FoodTicker ticker() {
        return ticker;
    }

    public BonusApplier bonuses() {
        return bonuses;
    }
}
