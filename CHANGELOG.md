# Changelog

All notable changes to Just Another Food System. The format follows Keep a Changelog; versions follow Semantic Versioning.

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
