# Steve's Pantry: Minecraft 26.1.2 (NeoForge)

- Minecraft: 26.1.2
- Loader: NeoForge 26.1.2.115
- Build: ModDevGradle 2.0.148, Gradle 9.2.1
- Java: 25 (Gradle downloads it through the foojay toolchain resolver; any JDK 17+ can launch Gradle)

## Build and run

```
./gradlew build        # jar in build/libs/
./gradlew runClient
./gradlew runServer    # put eula=true in run/eula.txt first
```

## Differences from main

Same features, data components and file layout as the 1.21.1 build at the repo root. What changed
is what Minecraft 26.1 changed underneath it:

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
