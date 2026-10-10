# Steve's Pantry: Minecraft 26.3 (NeoForge)

- Minecraft: 26.3
- Loader: NeoForge 26.3.0.64-beta (**NeoForge for 26.3 is still beta**: expect breaking changes
  between builds; bump `neo_version` in `gradle.properties` as new builds land)
- Build: ModDevGradle 2.0.148, Gradle 9.2.1
- Java: 25 (Gradle downloads it through the foojay toolchain resolver; any JDK 17+ can launch Gradle)

## Build and run

```
./gradlew build        # jar in build/libs/
./gradlew runClient
./gradlew runServer    # put eula=true in run/eula.txt first
```

## Differences from main

Same features, data components and file layout as the 1.21.1 build at the repo root. This is the
26.1.2 port moved to 26.3; the only code change 26.3 itself needed is the first point below. The
rest is what Minecraft 26.1 already changed underneath it:

- **Blocks have no codecs in 26.3.** `Block.codec()` and `simpleCodec` are gone, so `IceboxBlock`
  drops its `CODEC` field and `codec()` override.

- **Wandering trader trades are data.** NeoForge's `WandererTradesEvent` is gone because vanilla
  trades are now datapack entries. Each spice is a `data/stevespantry/villager_trade/wandering_trader/<spice>.json`
  trade, tagged into `#minecraft:wandering_trader/common` (generic spices) or `/uncommon` (rare
  ones, the old "rare" pool). The `event/SpiceTrades` class is removed.
- **Dish eating.** Food is now split into `minecraft:food` (nutrition, saturation) and
  `minecraft:consumable` (eat time). The dish recipe writes both, and the bowl comes back through
  the Dish item's `use_remainder` (`usingConvertsTo(Items.BOWL)`) instead of `FoodProperties`.
- **Spoiled Leftovers** get their Hunger/Nausea chances from consume effects on the consumable.
- **Mortar and Pestle** overrides NeoForge's `getCraftingRemainder(ItemInstance)`, which now
  returns an `ItemStackTemplate`.
- **Dish recipe**: `CustomRecipe` no longer takes a category, so `DishAssemblyRecipe` keeps its own
  and has its own `MapCodec`/`StreamCodec`, registered as a plain `RecipeSerializer`.
- **Icebox**: saves with `ValueInput`/`ValueOutput`; contents drop through the block entity's own
  removal hook; blocks no longer have tooltips, so the item is an `IceboxBlockItem` that shows them.
  The screen uses the new `GuiGraphicsExtractor` / `extractBackground` rendering.
- **Assets/data**: item model definitions in `assets/stevespantry/items/`; recipe ingredients use
  the plain string format (`"minecraft:stick"`, `"#minecraft:planks"`).
- Registry ids are passed to item/block properties through `registerItem`/`registerBlock`.
- Renames: `ResourceLocation` is `Identifier`; effects are `HASTE`, `RESISTANCE`, `STRENGTH`,
  `SPEED`, `JUMP_BOOST`, `NAUSEA`.
