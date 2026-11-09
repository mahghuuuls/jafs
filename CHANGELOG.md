# Changelog

All notable changes to Just Another Food System. The format follows Keep a Changelog; versions follow Semantic Versioning.

## [1.2.0] - 2026-11-09

### Added

- Statusworks contribution types `jafs:recovery_rate` and `jafs:depletion_rate`: a Statusworks status, pool, or mark can raise or lower a player's recovery rate or food depletion rate in percent while it is active, scaled as the status defines (fixed, per stack, per amount, per fill). Contributions add up with food and Setworks bonuses; `/jafs inspect` lists each one by its Statusworks key under `jafs:statusworks`. A value past the property limits (-1000 to 1000) is used at the nearer limit and logged once per player and key.

### Compatibility

- Optional [Statusworks](https://www.curseforge.com/minecraft/mc-mods/statusworks) 1.2.0 or newer, detected at runtime (`after:statusworks` ordering, no version range in the metadata). With an older Statusworks, one warning at startup and nothing is registered.
- Tested on Forge 2847 (development client with Statusworks 1.2.0, Setworks 1.1.0, and CraftTweaker 4.1.20.718) and on Cleanroom 0.7.14 with Statusworks and CraftTweaker present.

## [1.1.0] - 2026-11-03

### Added

- Eight attribute-backed properties for food definitions: `jafs:movement_speed`, `jafs:attack_damage`, `jafs:attack_speed`, `jafs:armor`, `jafs:armor_toughness`, `jafs:knockback_resistance`, `jafs:luck`, `jafs:block_reach`. Each is applied as saved attribute modifiers on the player (one flat, one percent), shown in the tooltip with its unit and color, and removed at expiry, death, Bitter Berries, and `/jafs clear`.
- Seven more properties that exist only with Potion Core loaded: `jafs:jump_height`, `jafs:dig_speed`, `jafs:projectile_damage`, `jafs:magic_damage`, `jafs:damage_resistance` (positive is less damage taken), `jafs:magic_shielding`, `jafs:step_height`. Without Potion Core a definition naming one is reported at load and that modifier is skipped; the rest of the definition still applies.
- Per-property limits for every modifier (README table); an amount outside them is reported with the definition id and field.
- Setworks effect kind `jafs:property`: an equipment-set bonus can grant a recovery rate or food depletion rate bonus in percent while the set is worn; several bonuses add up; `/jafs inspect` lists each bonus by its Setworks instance id under `external=`.
- Two more examples in `foods-examples.json` (an attribute food and a Potion Core food).

### Compatibility

- Optional [Potion Core](https://www.curseforge.com/minecraft/mc-mods/potion-core) 1.9 and optional [Setworks](https://github.com/mahghuuuls/setworks) 1.0.0, both detected at runtime (`after:setworks` ordering; nothing from either mod is referenced).
- Tested on Forge 2847 (development client and dedicated server) and on Cleanroom 0.7.14 with Potion Core, Setworks, and CraftTweaker present.

## [1.0.0] - 2026-10-20

First release.

### Added

- Hunger replacement: no starvation, sprinting never blocked, any food edible at any time, no hunger regeneration, the hunger bar hidden; unconfigured consumables unchanged.
- Food definitions in `config/jafs/foods.json` (empty by default) with a commented examples file: matchers by item, item variant, NBT fields, or cake-like block; full duration; priority; modifiers on properties.
- Food slots (three by default): a new food takes an empty slot, an eligible food is refreshed, a full set replaces the eligible food with the least time left, anything else is refused with a message and the item is kept. Bonuses apply at constant strength until the timer ends; the player is told when a food runs out.
- Properties: maximum health (flat or percent), recovery rate, and food depletion rate; flat and percent modifiers from all active foods add up.
- Recovery: health returns after a damage-free delay at a percent of maximum health per second; damage restarts the delay; spectators and the Inhibited effect pause it.
- Depletion: a configurable timer speed, 0 pauses; each level of the Hunger effect speeds timers up.
- Bitter Berries: a drop from tall grass and ferns broken without shears that clears every active food.
- HUD: each active food's icon with its compact time left, white, yellow once the food can be eaten again, red in its last minute; eight anchors, a growth direction, offsets, scale, and property colors in the client config and the mod's config screen; a cancelable render event for addons.
- Tooltips: bonuses with signed amounts and the duration on configured foods; a line on edible items without a definition (can be hidden).
- Commands at permission level 2: `/jafs inspect`, `/jafs clear`, `/jafs reload` (live reload with defined consequences for active foods), `/jafs report` (a CSV of every edible item with its definition and bonuses).
- Lifecycle: foods, timers, and the recovery delay saved with the player; death clears foods unless configured otherwise; reconciliation of changed definitions on load.
- Addon API (`com.mahghuuuls.jafs.api`, version 1.0): property registration, rate modifier providers, the server food service with duration payments, a client snapshot.
- `diagnosticMode`: one log line per decision for server owners and pack makers.

### Compatibility

- Requires Forge 14.23.5.2847 or newer on client and server (minimum; built against 2847). Tested on Forge 2847 and on Cleanroom 0.6.13.
- No other mod required. Optional Inhibited 1.2.0 (recovery pause). AppleCore 3.4.0 present: no change in behavior.
- Recovery and Hunger Tweaks: both mods heal; a warning at startup and a notice to operators at login ask for one to be removed.
- Incompatible by design with other mods that remove or pin hunger and with AppleCore-based hunger overhauls (documented, not blocked).

### Known Issues

- AppleSkin and LemonSkin still draw vanilla hunger values in food tooltips and a health preview on the hearts; turn those options off in their config. RPG-Hud's hunger element must be disabled in its settings.
- Removing the mod leaves the last maximum-health bonus on existing players as a saved attribute modifier.
