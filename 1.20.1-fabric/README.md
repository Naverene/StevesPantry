# Steve's Pantry for Minecraft 1.20.1 (Fabric)

| | |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Fabric Loader 0.19.5 (works with 0.15+), Fabric API 0.92.12+1.20.1 |
| Build | Fabric Loom 1.17.21, Gradle 9.5.1 (wrapper), Mojang mappings |
| Java | 17 (Gradle downloads it through the foojay toolchain resolver; Gradle itself needs JDK 21+) |

Needs Fabric API. Pam's HarvestCraft 2 (Fabric) is optional.

## Build and run

```sh
./gradlew build          # jar in build/libs/stevespantry-1.20.1-fabric-0.1.0.jar
./gradlew runClient
./gradlew runServer      # run dir is run/server; put eula=true in run/server/eula.txt first
```

## Differences from main (NeoForge 1.21.1)

- **No data components on 1.20.1: dish data lives in item NBT.** The dish stack's tag holds
  `ingredients` (list of item ids), `spices` (list of spice ids), `made_at`, `shelf_life`, and the
  computed `nutrition`, `saturation` (saturation points, as on main) and `eat_ticks`.
  `DishContents` and `Freshness` are still records, with `get(stack)`/`set(stack)` for the NBT.
- **Per-stack food values.** 1.20.1's `Item#getFoodProperties()` takes no stack, so the Dish
  registers an empty placeholder food (0 nutrition, which is what makes it edible at all) and one
  small mixin, `FoodDataMixin`, intercepts `FoodData#eat(Item, ItemStack)` (the single place
  eating restores hunger) and feeds the player from the stack's NBT instead. `getUseDuration` reads
  `eat_ticks`; `finishUsingItem` hands back the bowl like vanilla stews. An empty Dish (e.g. from
  `/give`) can't be eaten. Mods that read `Item#getFoodProperties()` directly see 0 for dishes.
- **Mortar and Pestle remainder** uses Fabric's stack-aware `FabricItem#getRecipeRemainder`.
- **Grind recipes** use `fabric:load_conditions` with `fabric:tags_populated` instead of NeoForge's
  `not(tag_empty)`. Recipe JSON uses 1.20.1's `"result": {"item": ...}` format and the old plural
  data folders (`recipes/`, `loot_tables/`, `tags/items/`, `tags/blocks/`).
- **Spice source tags** list the Pam's HarvestCraft 2 item ids plus main's `c:crops/...` style tags,
  and also the flat names 1.20.1-era Fabric mods use (`c:ginger`, `c:chile_peppers`,
  `c:bellpeppers`, `c:sesame_seeds` ...). Every entry is optional.
- **Perishable tag**: Fabric 1.20.1 has no `c:foods/...` tags, so it lists the vanilla raw/cooked
  meats, `#minecraft:fishes`, milk bucket and egg directly, plus optional `c:` tags
  (`c:raw_meats`, `c:cooked_fishes`, `c:milk`, `c:foods/raw_meat` ...).
- **Wandering trader** via `TradeOfferHelper.registerWanderingTraderOffers` (level 1 = generic,
  2 = rare); same prices as main.
- **Registration** goes straight into the vanilla registries (`ModRegistries` fields are the
  objects themselves, not deferred holders); creative tab via `FabricItemGroup`.
- **Icebox**: 1.20.1's `BaseContainerBlockEntity` doesn't manage an item list, so
  `IceboxBlockEntity` implements the container methods itself; drops contents in `onRemove`
  the way vanilla furnaces do. The screen calls `renderBackground` itself (1.20.1 doesn't).

## API

The same API as main (see the repo root's [API.md](../API.md)): package
`com.naverene.stevespantry.api`, `StevesPantryApi.get()` / `isAvailable()`, `VERSION = 1`, same
method names and behaviour. Registered spice items count in the dish recipe, shelf-life modifiers
run on every assembled dish (crafting grid and `makeDish`), and registered coolants work in the
Icebox, overriding the built-in snow/ice values.

`./gradlew build` also writes `build/libs/stevespantry-1.20.1-fabric-<version>-api.jar` (API
classes plus sources, remapped to intermediary like the main jar), so addons depend on it with
Loom's `modCompileOnly` and put the full jar on `modRuntimeOnly`:

```groovy
dependencies {
    modCompileOnly files('libs/stevespantry-1.20.1-fabric-0.1.0-api.jar')
    modRuntimeOnly files('libs/stevespantry-1.20.1-fabric-0.1.0.jar')
}
```

```java
// In your ModInitializer#onInitialize (Fabric has no mod construction / common setup split).
if (StevesPantryApi.isAvailable()) {
    IPantryApi pantry = StevesPantryApi.get();
    pantry.registerCoolant(MyItems.FREEZER_PACK, 48000);
    pantry.spice("chili").ifPresent(chili -> pantry.registerSpiceItem(MyItems.CHILI_POWDER, chili));
}
```

Differences from main's API, all from 1.20.1 itself:

- `ISpice.effect()` returns a `MobEffect` (1.20.1 has no `Holder<MobEffect>` for effects).
- Dish data is NBT, not data components, so `ages(stack)` means the stack has a `shelf_life` tag
  and `dish(stack)` reads the `ingredients`/`spices` tags; the results are the same.
- `perishableTag()` is the same `stevespantry:perishable` item tag (data folder `tags/items/`).
