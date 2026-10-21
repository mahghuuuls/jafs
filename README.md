# Just Another Food System

A Minecraft 1.12.2 Forge mod that replaces hunger with timed, configurable food bonuses in the style of Valheim, plus delayed health recovery. Hunger no longer matters; eating a configured food activates its property bonuses (maximum health, recovery rate, food depletion rate, or addon properties) for a set duration in one of a few food slots; health returns on its own after a damage-free delay. The mod ships no food definitions: pack makers and addons supply them.

Player-facing description: [MOD-PAGE.md](MOD-PAGE.md). Release history: [CHANGELOG.md](CHANGELOG.md). License: [MIT](LICENSE). Build template notice: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Install side

- Mod id `jafs`, version 1.0.0. Needed on the server and on every client. Requires Forge 14.23.5.2847 or newer (minimum, no upper bound; built against 2847, tested on 2847 and on Cleanroom 0.6.13). No other mod is required.
- Gameplay runs on the server: one service per player decides meals, timers, bonuses, and recovery. Clients receive the definitions at login and a snapshot of their own foods, which they count down locally; the server corrects them.
- Optional [Inhibited](https://www.curseforge.com/minecraft/mc-mods/inhibited): recovery pauses while the `inhibited:inhibited` effect is active (`inhibitedBlocksRecovery`). Detected at runtime; nothing from it is referenced.
- With [Recovery and Hunger Tweaks](https://www.curseforge.com/minecraft/mc-mods/recovery-and-hunger-tweaks) loaded, a startup warning and an operator notice at login say that both mods heal and one should be removed; the mod keeps running.
- Incompatible by design, not blocked at load: other mods that remove or pin hunger, and hunger overhauls that depend on [AppleCore](https://www.curseforge.com/minecraft/mc-mods/applecore). AppleCore itself coexists. RPG-Hud users disable its hunger element themselves; AppleSkin and LemonSkin food values and the hearts preview are meaningless here and are turned off in their config.
- Removing the mod from an existing world leaves the last maximum-health bonus on each player as a saved attribute modifier until something edits it, as with other attribute-modifying mods.

## How food works

- Hunger is held at a fixed level on the server (10 of 20): no starvation, no sprint block, no eating restriction, no hunger regeneration, exhaustion and saturation inert, the hunger bar hidden. Peaceful healing and other mods' healing are untouched. Unconfigured consumables keep their vanilla behavior.
- A definition names one or more matchers (an item id with optional metadata and NBT fields, or a cake-like block id), a full duration in seconds, a priority, and modifiers (property, `flat` or `percent`, amount). Several definitions matching one item: the higher priority wins; equal priorities are a reported conflict and both are disabled.
- Eating a matched consumable on the server: a new definition takes an empty slot; an active one is refreshed to its full duration when eligible; with every slot full, the eligible active food with the least time left is replaced; otherwise the meal is refused with a message and the item is not consumed (the client predicts the refusal and does not start the eating gesture). Eligible means strictly below `eligibilityThreshold` percent of the full duration. Vanilla refuses `ItemFood` in creative before the mod sees it; drinks and cake blocks follow the rules in creative too.
- Modifiers of all active foods are summed per property (flat first, then percent) and applied constantly; expiry removes everything in the same tick and tells the player. A maximum-health increase leaves current health; a decrease clamps without damage.
- Recovery: after `recoveryDelay` damage-free seconds, `baseRecoveryRate` percent of maximum health per second, as ordinary healing other mods can see; damage restarts the delay; spectators and the Inhibited effect pause it. The delay persists across logout.
- Every timer counts down at `depletionRate`, plus `hungerEffectIncrement` per level of the Hunger effect; 0 pauses. Addons can deduct time from every food at once (a duration payment) through the API.
- Bitter Berries (`jafs:bitter_berries`): always edible in survival, clear every active food at once, occupy no slot. Drop with a 1 in 16 chance from tall grass, fern, and double tall grass broken without shears; no recipe.
- Death clears foods unless `keepFoodsOnDeath`; the recovery delay resets on death. Foods, timers, and the delay are saved with the player and survive logout, dimension changes, and server restarts; a changed definition is reconciled on load.

## Configuration

`config/jafs/jafs.cfg` (server side; `/jafs reload` applies it and the definition file without a restart):

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `activeFoodLimit` | 3 | 1 to 12 | Food slots per player |
| `eligibilityThreshold` | 50 | 1 to 100 | Percent of full duration below which a food can be refreshed or replaced |
| `baseRecoveryRate` | 5.0 | 0 to 100 | Percent of maximum health restored per second once recovery is active |
| `recoveryDelay` | 10 | 0 to 300 | Damage-free seconds before recovery starts |
| `depletionRate` | 1.0 | 0 to 10 | Timer speed; 0 pauses natural depletion |
| `hungerEffectIncrement` | 0.5 | 0 to 10 | Added to the depletion rate per level of the Hunger effect |
| `inhibitedBlocksRecovery` | true | | Pause recovery under the Inhibited effect when that mod is present |
| `keepFoodsOnDeath` | false | | Whether active foods survive death |
| `diagnosticMode` | false | | One log line per decision (meals, timers, recovery, payments, reloads); nothing per tick |

`config/jafs/foods.json` is generated empty; `foods-examples.json` next to it is never loaded and holds commented, copy-ready entries. Values outside a range, unknown items, and malformed entries are reported with the definition id and field and the entry is skipped or kept inactive; the previous working configuration stays on a failed reload. Properties shipped: `jafs:max_health` (flat HP or percent), `jafs:recovery_rate` (percent), `jafs:depletion_rate` (percent).

`config/jafs/jafs-client.cfg` (client side, also in the mod's config screen; never changes gameplay): `hudAnchor` (eight positions, default `bottom-left`), `hudGrowth` (row or column and its starting end), `hudOffsetX`, `hudOffsetY`, `hudScale`, `propertyColorOverrides` (`<property id>=<vanilla color name>` per line), `showUnconfiguredTooltip`.

## Commands

All at permission level 2.

- `/jafs inspect <player>`: active foods with remaining and full durations and eligibility, combined bonuses per property, effective recovery rate and status, effective depletion rate.
- `/jafs clear <player>`: removes every active food, like Bitter Berries.
- `/jafs reload`: validates and applies `jafs.cfg` and `foods.json`, reports loaded, disabled, and rejected definitions. A removed definition clears its active foods; changed modifiers apply at once; a shorter duration caps remaining time.
- `/jafs report`: writes `config/jafs/jafs-foods-report.csv` with every edible item the game knows (kind, display name, mod, vanilla hunger and saturation, definition, status, duration, bonuses, matcher), for pack makers.

## Addon API

The `com.mahghuuuls.jafs.api` package is the public, stable surface (`JafsApi.API_VERSION` is `1.0`); an addon declares `required-after:jafs` and calls it from its own pre-init on.

- `JafsApi.registerProperty(PropertySpec, IPropertyApplier)`: a property with id, display name, unit, default color, supported operations, and limits, plus the applier that receives the per-player sums. Registration closes when loading completes. JAFS's own three properties go through the same path.
- `JafsApi.registerRateModifierProvider(id, IRateModifierProvider)`: per-player percent contributions to recovery rate and depletion rate from equipment, effects, or anything else; `markDirty(player)` when a contribution changes.
- `JafsApi.getFoodService()` (server): `getActiveFoods`, `getPropertyValue`, `canPay`, `pay` (a duration payment, atomic across all foods), `clearAll`.
- `api.client`: `ClientFoodApi.getSnapshot()` for addon HUDs; `FoodHudRenderEvent`, cancelable, to replace the HUD.

The repository holds a development-only fixture addon (`-PjafsFixture`) that exercises the API; it is not part of the release jar.

## Scope boundaries

- No food definitions, balance, recipes, or integrations ship with the mod; addons and packs own them.
- Hunger is pinned, not configurable; saturation and exhaustion do nothing.
- Configuration is read on the server; clients get the definitions over the network, so a pack ships `jafs.cfg` and `foods.json` server side and the client file only for HUD preferences.

## Development

Standard Forge 1.12.2 project on the CleanroomMC ForgeDevEnv template: `./gradlew build` produces `build/libs/jafs-1.0.0.jar` (`-PjafsFixture` also builds the fixture addon); `./gradlew runClient` and `./gradlew runServer` start the development runtimes. Development-only jars go in the git-ignored `libs/` folder (see `gradle/scripts/dependencies.gradle`). Unit tests run without Minecraft and cover the definition grammar and validation, slot and eligibility rules, bonus math, timers and payments, recovery, reload consequences, the HUD layout and time format, and the food report.

## License

MIT, see [LICENSE](LICENSE). The build scripts and project skeleton come from CleanroomMC ForgeDevEnv under MIT, see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
