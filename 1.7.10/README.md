# Steve's Pantry for Minecraft 1.7.10 (Forge)

| | |
|---|---|
| Minecraft | 1.7.10 |
| Loader | Forge 10.13.4.1614 |
| Build | RetroFuturaGradle 1.4.9, Gradle 8.14.3 (wrapper), MCP stable_12 mappings |
| Java | 8 (Gradle downloads Azul JDK 8 through the foojay toolchain resolver; Gradle itself runs on JDK 17+) |

Pam's HarvestCraft (1.7.10) is optional.

## Build and run

```sh
./gradlew build          # reobfuscated jar in build/libs/stevespantry-0.1.0.jar
./gradlew runClient
./gradlew runServer      # run dir is run/; creates server.properties and eula.txt on first run (asks)
```

## Differences from main (NeoForge 1.21.1)

- **Dish data lives in item NBT** (no data components): `ingredients` (list of `{id, meta}`, since
  1.7.10 items still use metadata, e.g. raw salmon is `minecraft:fish` meta 1), `spices` (spice
  ids), `made_at`, `shelf_life`, and the computed `nutrition`, `saturation` (saturation points, as
  on main) and `eat_ticks`. `DishContents`/`Freshness` are small classes with `get(stack)`/`set(stack)`.
- **Dish is an `ItemFood`** whose `func_150905_g`/`func_150906_h` (heal amount / saturation modifier)
  and `getMaxItemUseDuration` read the stack's NBT, so mods reading `ItemFood` see real values.
  `onEaten` applies effects, calls `FoodStats.addStats` itself and returns a bowl.
- **Freshness bar** uses Forge's `showDurabilityBar`/`getDurabilityForDisplay`; Forge colours it
  green to red itself. Spoiling only happens in the main inventory (1.7.10 has no offhand).
- **No item tags: Ore Dictionary instead.** `ModTags` lists, per spice, the ore names Pam's
  HarvestCraft 1.7.10 registers (`cropPeppercorn`, `cropCinnamon`, `cropNutmeg`, `cropVanillabean`,
  `cropGinger`, `cropMustard`/`seedMustard`, `cropSesame`/`seedSesameseed`, `cropBellpepper`,
  `cropChilipepper`, `cropGarlic`) plus common alternatives and the rare ones (`cropSaffron`,
  `cropCumin`, `cropTurmeric`, `cropCardamom`, `cropClove`, `cropStaranise`, ...). Grind recipes are
  `ShapelessOreRecipe`s added in post-init only for names that have items (main's "tag not empty").
  Perishables are `listAllmeatraw/meatcooked/fishraw/fishcooked/milk/egg` plus the vanilla meats,
  fish, milk bucket and egg.
- **All recipes are in code** (no JSON recipes in 1.7.10); dish assembly is an `IRecipe` registered
  with `RecipeSorter` after vanilla shapeless recipes.
- **No wandering trader in 1.7.10**: the spice trades (1 emerald -> 4, rare 3 emeralds -> 2) are
  offered by the **Farmer** villager through `VillagerRegistry.IVillageTradeHandler`. Each spice is a
  candidate trade with some chance each time a farmer rolls new trades (25% generic, 10% rare).
- **Saffron gives Invisibility**: 1.7.10 has no Luck effect (and its Saturation effect does nothing
  over time).
- **Icebox**: `BlockContainer` + `TileEntity` ticking in `updateEntity`, `ISidedInventory` for
  hoppers (top/bottom food, sides ice, insert only). No blue ice in 1.7.10, so coolants are snowball,
  snow block, ice and packed ice. GUI via `IGuiHandler`; the screen is widened to cover the ice
  panel, because 1.7.10 treats clicks outside the GUI width as "drop the held stack". No loot
  table or mineable tag: the block drops itself and is set as axe-harvestable in code. Its tooltip
  lives on an `ItemBlock` (`IceboxItem`).
- **Spoiled Leftovers** is a small `ItemFood` subclass, since 1.7.10 food takes only one chance effect.
- **Assets**: 1.7.10 has no item/block model JSON or blockstates; icons come from
  `textures/items` and `textures/blocks` via `registerIcons`. Lang file is `en_US.lang`.
- **Proxies**: `CommonProxy` (also the GUI handler) and `StevesPantryClient` (client proxy, sets
  `PantryClock` and opens the screen) replace main's client mod entrypoint.
- The walk-in freezer is not ported yet; when it is, the 1.7.10 version will run on EU
  (IC2/GregTech) instead of FE.
