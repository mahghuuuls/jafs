This mod replaces hunger with timed food bonuses, in the style of Valheim. Hunger stops mattering: nobody starves, sprinting is never blocked, and any food can be eaten at any time. Eating a configured food activates its bonuses for a set time. The mod ships no food values of its own: the pack maker writes a definition for each food, with its duration and bonuses, in one JSON file.

<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

**Food**

- A player holds up to three active foods at once (configurable). Each one keeps its full bonus until its time runs out, then the bonus is gone and a message says so.
- A food can be eaten again once it is below half of its duration (configurable). Eating it earlier is refused with a short message. When every slot is full, a new food replaces the active food that can be eaten again and has the least time left.
- Bonuses come as properties: maximum health, health recovery, food timer speed, and a set of player stats such as movement speed, attack damage, and armor (the full list is below). Several foods add up. Addon mods can add their own properties.
- The Hunger effect makes every food timer run faster, one step per effect level.
- A small HUD by the hotbar shows each active food's icon with its time left. The time turns yellow once the food can be eaten again and red in its last minute. Its position, direction, and size are set in the mod's config screen.
- The tooltip of a configured food lists its bonuses and duration. Other edible items say that no bonus is configured.
- Bitter Berries, the one item added, clear every active food at once. They drop now and then from tall grass and ferns broken by hand.
- The hunger bar is hidden. Death clears active foods unless the pack keeps them.

**Health recovery**

Health comes back on its own, with or without food. After ten seconds without damage (configurable), health returns at five percent of maximum health per second (configurable) until full. Taking damage restarts the wait. Food can make recovery faster or slower through the recovery rate property.

**For modpack creators**

Food does nothing until you write definitions. Edit `config/jafs/foods.json` on the server, then run `/jafs reload` (or restart). `foods-examples.json` next to it has copy-ready entries with comments. One entry looks like this:

```json
{
  "id": "steak",
  "match": [ { "item": "minecraft:cooked_beef" } ],
  "fullDuration": 1200,
  "modifiers": [
    { "property": "jafs:max_health", "operation": "flat", "amount": 4 },
    { "property": "jafs:recovery_rate", "operation": "percent", "amount": 25 }
  ]
}
```

A definition can match several items, an item variant, an item by NBT (a potion by its type), or a cake-like block. Drinks and potions are ordinary items until a definition names them. `config/jafs/jafs.cfg` holds the slot count, the eating threshold, the recovery delay and rate, and the timer speed. Each property has its limits (listed in the README); an amount past a limit is reported and that bonus is skipped. Bad entries are reported in the log and skipped; the game never crashes over a definition.

**Properties a food can change**

Each modifier names a property, an operation (`flat` adds a fixed amount, `percent` scales the current value), and an amount. Always available:

- `jafs:max_health`: maximum health, in hearts worth of health points (flat) or percent.
- `jafs:recovery_rate`: how fast health comes back, in percent of the base rate.
- `jafs:depletion_rate`: how fast every food timer counts down, in percent of the base speed.
- `jafs:movement_speed`: walking and sprinting speed, in percent.
- `jafs:attack_damage`: melee damage, flat or percent.
- `jafs:attack_speed`: how fast attacks recharge, flat or percent.
- `jafs:armor`: armor points, flat or percent, with no armor worn too.
- `jafs:armor_toughness`: armor toughness, flat or percent.
- `jafs:knockback_resistance`: from 0 to 1, where 1 ignores knockback (flat).
- `jafs:luck`: the luck stat that loot tables read (flat).
- `jafs:block_reach`: how far away blocks can be reached, in blocks (flat) or percent.

With [Potion Core](https://www.curseforge.com/minecraft/mc-mods/potion-core) installed, also:

- `jafs:jump_height`: jump height, flat or percent.
- `jafs:dig_speed`: block breaking speed, flat or percent.
- `jafs:projectile_damage`: damage of arrows and other projectiles, flat or percent.
- `jafs:magic_damage`: magic damage dealt, flat or percent.
- `jafs:damage_resistance`: less damage taken when positive, more when negative, flat or percent.
- `jafs:magic_shielding`: magic damage absorbed, flat.
- `jafs:step_height`: how high a step the player walks up without jumping, in blocks (flat) or percent.

Without Potion Core, a definition that names one of these keeps its other bonuses, and the log says which one was skipped.

**Set bonuses with Setworks**

With [Setworks](https://github.com/mahghuuuls/setworks) installed, an equipment set can grant food bonuses while it is worn. A Setworks bonus uses the effect kind `jafs:property` with one of these two properties and an amount in percent:

- `jafs:recovery_rate`: faster or slower health recovery.
- `jafs:depletion_rate`: faster or slower food timers.

The other properties are stats that Setworks already changes on its own with `setworks:attribute`. Set bonuses add up with each other and with food, and `/jafs inspect` lists each one. A small Setworks script:

```
#loader setworks
import mods.setworks.Setworks;

Setworks.armorSet("mypack:iron_armor", "Iron Armor",
    <minecraft:iron_helmet>, <minecraft:iron_chestplate>, <minecraft:iron_leggings>, <minecraft:iron_boots>);

Setworks.bonus("mypack:iron_recovery", "mypack:iron_armor", 2, "Two pieces: +50% recovery rate")
    .effect("jafs:property", {property: "jafs:recovery_rate", amount: "50"});

Setworks.fullSetBonus("mypack:iron_endurance", "mypack:iron_armor", "Full set: food timers 30% slower")
    .effect("jafs:property", {property: "jafs:depletion_rate", amount: "-30"});
```

**Food report**

`/jafs report` writes a spreadsheet (`config/jafs/jafs-foods-report.csv`) of every edible item in your pack: its mod, its vanilla hunger and saturation values, the definition that covers it, its duration and bonuses, and whether it is still unconfigured. Open it in any spreadsheet program to see what your pack still needs and to plan its food balance.

**Other mods**

- Remove [Recovery and Hunger Tweaks](https://www.curseforge.com/minecraft/mc-mods/recovery-and-hunger-tweaks); both mods heal, and this one warns about it.
- Mods that remove or pin hunger, and hunger overhauls built on [AppleCore](https://www.curseforge.com/minecraft/mc-mods/applecore), do the same job and should not be installed together with this mod. AppleCore itself is fine.
- [AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) and [LemonSkin](https://www.curseforge.com/minecraft/mc-mods/lemonskin) still show vanilla hunger values in food tooltips and a health preview on the hearts; those numbers mean nothing here, so turn those options off in their config.
- [RPG-Hud](https://www.curseforge.com/minecraft/mc-mods/rpg-hud) draws its own hunger bar; disable that element in its settings.
- With [Inhibited](https://www.curseforge.com/minecraft/mc-mods/inhibited) installed, recovery pauses while the Inhibited effect is active (can be turned off).
- With [Potion Core](https://www.curseforge.com/minecraft/mc-mods/potion-core) installed, seven more stats can be food bonuses (see the property list above).
- With [Setworks](https://github.com/mahghuuuls/setworks) installed, set bonuses can raise or lower recovery rate and food timer speed (see above).

**Install**

Needed on the server and on every client. No other mod is required.

Source: [github.com/mahghuuuls/jafs](https://github.com/mahghuuuls/jafs)
