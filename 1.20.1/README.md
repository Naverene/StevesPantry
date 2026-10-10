# Steve's Pantry for Minecraft 1.20.1 (Forge)

- **Minecraft:** 1.20.1
- **Loader:** Minecraft Forge 47.4.26 (any 47.x should work; `mods.toml` asks for `[47,)`)
- **Java:** 17 (Gradle's toolchain support downloads it through the foojay resolver if it isn't installed)
- **Build tooling:** ModDevGradle's legacy Forge plugin (`net.neoforged.moddev.legacyforge` 2.0.148),
  Gradle 9.1, official Mojang mappings

## Building and running

```sh
./gradlew build        # reobfuscated mod jar in build/libs/
./gradlew runClient    # dev client
./gradlew runServer    # dev dedicated server (put eula=true in run/eula.txt first)
```

`build/libs/stevespantry-1.20.1-0.1.0.jar` is the one to drop into a Forge 1.20.1 `mods` folder (it
has been remapped to SRG names by `reobfJar`).

Pam's HarvestCraft 2 (Crops, Trees) for 1.20.1 is optional, as on main.

## Differences from main

- **No data components on 1.20.1.** Dish data lives in the item's NBT, with the same field names main's
  components use: `ingredients` (item ids), `spices` (spice ids), `made_at`, `shelf_life`, plus the
  computed `nutrition`, `saturation` (absolute amount restored) and `eat_ticks`. `DishContents` and
  `Freshness` are still records, with `get(stack)` / `set(stack)` helpers instead of codecs.
- **Food values per stack** come from Forge's `IForgeItem#getFoodProperties(ItemStack, LivingEntity)`
  and `getUseDuration(ItemStack)` overrides on `DishItem`, which read that NBT; the registered item has a
  placeholder food value only so vanilla treats it as edible. The bowl comes back the way a stew's does.
- **Spice sources** list the Pam's HarvestCraft 2 item ids (same as main: `pamhc2crops:gingeritem`,
  `pamhc2trees:cinnamonitem`, ...) plus the `forge:` tags of that era (`forge:crops/ginger`,
  `forge:crops/mustardseeds`, `forge:seeds/mustard`, ...), and also the `c:` tags, since some
  multi-loader mods tag their crops with `c:` on Forge 1.20.1 too.
- **Perishable** has no `c:foods/*` tags to lean on in 1.20.1, so it lists the vanilla meats, fish, milk
  bucket and egg directly, plus the common `forge:` tags (`forge:raw_meats`, `forge:raw_beef`,
  `forge:milk`, `forge:eggs`, ...) and the `c:` ones.
- **Conditional grind recipes** use Forge's top-level `"conditions"` key with `forge:not` +
  `forge:tag_empty` (main uses `neoforge:conditions`).
- **Hopper faces:** Forge hoppers talk to the item-handler capability, so `IceboxBlockEntity` hands out a
  `SidedInvWrapper` per face; top/bottom reach the food, sides feed (only) the ice slot, as on main.
- **Client setup:** Forge 1.20.1 has no client-only `@Mod` entrypoint, so `StevesPantry` calls
  `StevesPantryClient.init` on the physical client, and the screen is registered with
  `MenuScreens.register` in `FMLClientSetupEvent`.
- Data folders use the 1.20.1 plural names (`recipes/`, `loot_tables/`, `tags/items/`, `tags/blocks/`),
  and recipe results use `"item"` instead of `"id"`. A `pack.mcmeta` (pack format 15) is included.

## API

Other mods can read and extend Steve's Pantry through `com.naverene.stevespantry.api`, the same
package, types and methods as main (see the root [API.md](../API.md)). Everything outside that package
is internal. `./gradlew build` also produces `build/libs/stevespantry-1.20.1-0.1.0-api.jar` (API classes plus sources):
compile against it with `compileOnly`, and put the full mod jar on the runtime classpath.

```java
if (StevesPantryApi.isAvailable()) {          // false when Steve's Pantry isn't installed
    IPantryApi pantry = StevesPantryApi.get();
    pantry.registerCoolant(MyItems.FREEZER_PACK.get(), 48000);
    pantry.spice("chili").ifPresent(chili -> pantry.registerSpiceItem(MyItems.CHILI_POWDER.get(), chili));
    pantry.registerShelfLifeModifier((foods, spices, life) ->
            foods.stream().anyMatch(f -> f.is(MyItems.SALT.get())) ? life * 2 : life);
}
```

| Area | Methods |
|---|---|
| Spices | `spices()`, `spice(id)`, `spiceOf(stack)`, `spiceStack(spice, n)`, `registerSpiceItem(item, spice)` |
| Dishes | `isDish(stack)`, `dish(stack)` (a `DishView`), `makeDish(foods, spices)` |
| Freshness | `ages`, `stamp`, `stage` (`FreshnessStage`), `remaining`, `ticksLeft`, `chill(stack, ticks, level)` |
| Shelf life | `perishableTag()` (`stevespantry:perishable`), `isPerishable(stack)`, `registerShelfLifeModifier(modifier)` |
| Cold storage | `registerCoolant(item, ticks)`, `coolantTicks(stack)` (an ice block is 24000) |

Items registered with `registerSpiceItem` season dishes in the crafting grid, shelf-life modifiers
run on every assembled dish, and registered coolants work in the Icebox (overriding the built-in
snow and ice values), all as on main. Do registrations during mod construction or common setup.

Differences from main: `ISpice#effect()` returns a plain `MobEffect` (there's no `Holder<MobEffect>`
on 1.20.1), and the freshness and dish data the API reads and writes live in the stack's NBT
(`made_at`, `shelf_life`, `ingredients`, `spices`) rather than data components.
