# Steve's Pantry for Minecraft 1.19.2 (Forge)

| | |
|---|---|
| Minecraft | 1.19.2 |
| Loader | Minecraft Forge 43.5.2 (needs 43.5+ for `ResourceLocation.fromNamespaceAndPath` and constructor-injected `FMLJavaModLoadingContext`) |
| Build | ModDevGradle legacy Forge plugin (`net.neoforged.moddev.legacyforge`) 2.0.148, Gradle 9.1.0 (wrapper), Mojang mappings |
| Java | 17 (Gradle downloads it through the foojay toolchain resolver; Gradle itself needs JDK 17+) |

Pam's HarvestCraft 2 (Crops, Trees) is optional.

## Build and run

```sh
./gradlew build          # reobfuscated jar in build/libs/stevespantry-1.19.2-forge-0.1.0.jar
./gradlew runClient      # run dir: run/client
./gradlew runServer      # run dir: run/server; put eula=true in run/server/eula.txt first
```

## Differences from main (NeoForge 1.21.1)

- **No data components on 1.19.2: dish data lives in item NBT.** The dish stack's tag holds
  `ingredients` (list of item ids), `spices` (list of spice ids), `made_at`, `shelf_life`, and the
  computed `nutrition`, `saturation` (saturation points, as on main) and `eat_ticks`, the same shape
  as the 1.20.1 Fabric port. `DishContents` and `Freshness` are records with `get(stack)`/`set(stack)`.
- **Per-stack food values** come from Forge's `IForgeItem#getFoodProperties(ItemStack, LivingEntity)`,
  which vanilla eating calls on Forge, so no mixin is needed. The Dish registers an empty placeholder
  food (0 nutrition, which is what makes it edible at all); `getUseDuration` reads `eat_ticks`;
  `finishUsingItem` hands back the bowl. An empty Dish (e.g. from `/give`) can't be eaten.
- **Grind recipes** use Forge's `forge:conditions` with `forge:not` + `forge:tag_empty` instead of
  NeoForge's `neoforge:conditions`. Recipe JSON uses 1.19.2's `"result": {"item": ...}` format, no
  `category` field, and the old plural data folders (`recipes/`, `loot_tables/`, `tags/items/`,
  `tags/blocks/`). The dish recipe uses `SimpleRecipeSerializer` (no crafting book category in 1.19.2).
- **Spice source tags** list the Pam's HarvestCraft 2 item ids (`pamhc2crops:gingeritem`,
  `pamhc2trees:cinnamonitem` ...) plus `forge:` tags (`forge:crops/ginger`, `forge:seeds/mustard`,
  `forge:fruits/cinnamon` ...) instead of `c:`. Every entry is optional.
- **Perishable tag**: Forge 1.19.2 has no food tags, so it lists the vanilla raw/cooked meats,
  `#minecraft:fishes`, milk bucket and egg directly, plus optional `forge:` tags.
- **Creative tab** is a `CreativeModeTab` subclass (1.19.2 has no tab registry); items join it with
  `Item.Properties#tab`.
- **Registration** uses Forge `DeferredRegister`/`RegistryObject`. There is no client-only `@Mod`
  entrypoint on 1.19.2, so `StevesPantry` calls `StevesPantryClient.init` through `DistExecutor`, and
  the screen is registered with `MenuScreens.register` in `FMLClientSetupEvent`.
- **Icebox**: 1.19.2's `BaseContainerBlockEntity` doesn't manage an item list, so
  `IceboxBlockEntity` implements the container methods itself. Forge hoppers use the item handler
  capability, so the block entity exposes a `SidedInvWrapper` per face that keeps main's hopper rules.
  The block uses `Material.WOOD` (no `MapColor` builder yet) and drops contents in `onRemove`.
  The screen draws with `PoseStack`/`RenderSystem` instead of `GuiGraphics`.
- **Wandering trader** via Forge's `WandererTradesEvent`; offers use `ItemStack` costs (no `ItemCost`).

## API

Other mods can read and extend Steve's Pantry through `com.naverene.stevespantry.api`, the same
package, types and methods as main (see the root [API.md](../API.md)). Everything outside that package
is internal. `./gradlew build` also produces `build/libs/stevespantry-1.19.2-forge-0.1.0-api.jar` (API classes plus sources):
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
on 1.19.2), and the freshness and dish data the API reads and writes live in the stack's NBT
(`made_at`, `shelf_life`, `ingredients`, `spices`) rather than data components.
