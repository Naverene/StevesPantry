# Steve's Pantry for Minecraft 1.18.2 (Forge)

| | |
|---|---|
| Minecraft | 1.18.2 |
| Loader | Minecraft Forge 40.3.12 (works with 40.x) |
| Build | ModDevGradle legacy Forge plugin (`net.neoforged.moddev.legacyforge`) 2.0.148, Gradle 9.1.0 (wrapper), Mojang mappings |
| Java | 17 (Gradle downloads it through the foojay toolchain resolver; Gradle itself needs JDK 17+) |

Pam's HarvestCraft 2 (Crops, Trees) is optional.

## Build and run

```sh
./gradlew build          # jar in build/libs/stevespantry-1.18.2-0.1.0.jar
./gradlew runClient
./gradlew runServer      # run dir is run/; put eula=true in run/eula.txt first
```

## Differences from main (NeoForge 1.21.1)

- **No data components on 1.18.2: dish data lives in item NBT.** The dish stack's tag holds
  `ingredients` (list of item ids), `spices` (list of spice ids), `made_at`, `shelf_life`, and the
  computed `nutrition`, `saturation` (saturation points, as on main) and `eat_ticks`.
  `DishContents` and `Freshness` are still records, with `get(stack)`/`set(stack)` for the NBT.
- **Per-stack food values** through Forge 40's stack-aware `IForgeItem#getFoodProperties(ItemStack,
  LivingEntity)`, which vanilla eating (`FoodData#eat`, `LivingEntity#addEatEffect`) calls on 1.18.2
  Forge. No mixin needed. The Dish registers a small base food only so it counts as edible;
  `getUseDuration` reads `eat_ticks`.
- **Mortar and Pestle remainder** uses Forge 1.18.2's `hasContainerItem`/`getContainerItem`
  (renamed `CraftingRemainingItem` in later versions).
- **Grind recipes** use Forge's top-level `"conditions"` key with `forge:not` + `forge:tag_empty`
  (main uses `neoforge:conditions`). Recipe JSON uses `"result": {"item": ...}`, no `category`,
  and the old plural data folders (`recipes/`, `loot_tables/`, `tags/items/`, `tags/blocks/`).
- **Spice source tags** list the Pam's HarvestCraft 2 item ids (`pamhc2crops:gingeritem`,
  `pamhc2trees:cinnamonitem` ...) and the `forge:crops/...`, `forge:seeds/...`, `forge:fruits/...`
  style tags 1.18.2 Forge mods use, in place of main's `c:` tags. Every entry is optional.
- **Perishable tag**: Forge 1.18.2 has no `c:foods/...` tags, so it lists the vanilla raw/cooked
  meats, fish, milk bucket and egg directly, plus optional `forge:` tags (`forge:raw_meats`,
  `forge:cooked_fishes`, `forge:milk`, `forge:eggs` ...).
- **Wandering trader** via Forge's `WandererTradesEvent`, as on main; offers take `ItemStack` costs.
- **Creative tab**: 1.18.2 tabs aren't registry entries, so `ModRegistries.TAB` is a plain
  `CreativeModeTab` subclass whose `fillItemList` keeps main's order (the Dish isn't listed, as on main).
- **Registration**: `RegistryObject`s from Forge `DeferredRegister`s (`ForgeRegistries.BLOCK_ENTITIES`,
  `CONTAINERS` are the 1.18.2 names). Text uses `TranslatableComponent`/`TextComponent`.
- **Icebox**: 1.18.2's `BaseContainerBlockEntity` doesn't manage an item list, so `IceboxBlockEntity`
  implements the container methods itself, plus the reach check `Container.stillValidBlockEntity`
  does on later versions. Forge hoppers use the item-handler capability, so it exposes a
  `SidedInvWrapper` per face to keep main's top/bottom = food, sides = ice rule. The block has no
  map colour (1.18.2 uses `Material.WOOD`). The screen draws with `PoseStack` and calls
  `renderBackground` itself.

## API

Other mods can read and extend Steve's Pantry through `com.naverene.stevespantry.api`, the same
package, types and methods as main (see the root [API.md](../API.md)). Everything outside that package
is internal. `./gradlew build` also produces `build/libs/stevespantry-1.18.2-0.1.0-api.jar` (API classes plus sources):
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
on 1.18.2), and the freshness and dish data the API reads and writes live in the stack's NBT
(`made_at`, `shelf_life`, `ingredients`, `spices`) rather than data components.
