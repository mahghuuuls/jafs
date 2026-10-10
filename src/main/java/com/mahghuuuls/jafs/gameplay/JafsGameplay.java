package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.property.PropertyRegistry;

/** The server-side object graph: one service, one gate, one ticker, one sync, one bonus applier, one recovery service. Built in pre-init. */
public final class JafsGameplay {

    private static JafsGameplay instance;

    private final ActiveFoodService service;
    private final SyncService sync;
    private final MealGate gate;
    private final FoodTicker ticker;
    private final BonusApplier bonuses;
    private final RecoveryService recovery;
    private final OperatorNotices notices = new OperatorNotices();

    private JafsGameplay() {
        this.service = new ActiveFoodService();
        this.sync = new SyncService();
        this.service.setSyncService(sync);
        this.bonuses = new BonusApplier(PropertyRegistry.get());
        this.service.setBonusHook(bonuses);
        this.gate = new MealGate(service);
        this.recovery = new RecoveryService(PropertyRegistry.get().rates());
        this.ticker = new FoodTicker(service, sync, PropertyRegistry.get().rates(), recovery);
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

    public RecoveryService recovery() {
        return recovery;
    }

    public OperatorNotices notices() {
        return notices;
    }
}
