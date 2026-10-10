# Steve's Pantry for Minecraft 1.16.5 (Forge)

- **Minecraft:** 1.16.5
- **Loader:** Minecraft Forge 36.2.42 (any 36.x should work; `mods.toml` asks for `[36,)`)
- **Java:** 8 for compiling and running the game (Gradle's toolchain support downloads it through the
  foojay resolver if it isn't installed). Gradle itself runs on Java 17 or 21.
- **Build tooling:** ForgeGradle 6 (`[6.0,6.2)`, as in Forge's own 36.2.42 MDK), Gradle 8.14.3,
  official Mojang mappings (`official` / `1.16.5`)

## Building and running

```sh
./gradlew build        # reobfuscated mod jar in build/libs/
./gradlew runClient    # dev client
./gradlew runServer    # dev dedicated server (put eula=true in run/eula.txt first)
```

`build/libs/stevespantry-1.16.5-0.1.0.jar` is the one to drop into a Forge 1.16.5 `mods` folder (it
has been remapped to SRG names by `reobfJar`).

Pam's HarvestCraft 2 (Crops, Trees) for 1.16.5 is optional, as on main.

## Differences from main

- **MCP class names.** Forge's official mappings on 1.16.5 only rename fields and methods; classes keep
  their MCP names (`TileEntity`, `Container`, `ContainerType`, `ItemGroup`, `ContainerScreen`,
  `MatrixStack`, `TranslationTextComponent`, ...). The mod's own classes keep main's names
  (`IceboxBlockEntity`, `IceboxMenu`, ...). Java 8 source: no records, `var`, switch expressions or
  pattern `instanceof`, so `DishContents` and `Freshness` are small immutable classes.
- **No data components on 1.16.5.** Dish data lives in the item's NBT, with the same field names main's
  components use: `ingredients`, `spices`, `made_at`, `shelf_life`, plus the computed `nutrition`,
  `saturation` (absolute amount restored) and `eat_ticks`.
- **Food values per stack:** Forge 36 has no stack-aware `getFoodProperties` hook (it came in later
  Forge versions). The registered dish's food restores 0/0, so vanilla's eating code (sounds, stats,
  advancements) adds nothing itself; `DishItem#finishUsingItem` then feeds the player the stack's
  own nutrition and saturation through `FoodStats#eat(int, float)`. `getUseDuration(ItemStack)` reads
  the eat time. The bowl comes back as on main.
- **Freshness bar** uses Forge 36's `showDurabilityBar` / `getDurabilityForDisplay` /
  `getRGBDurabilityForDisplay` hooks; the mortar's grid remainder uses `hasContainerItem` /
  `getContainerItem`.
- **Spice sources** list the Pam's HarvestCraft 2 1.16.5 item ids (`pamhc2crops:gingeritem`,
  `pamhc2trees:cinnamonitem`, ...) plus the `forge:` tags of that era (`forge:crops/ginger`,
  `forge:crops/mustardseeds`, `forge:seeds/mustard`, ...). All entries are optional
  (`{"id": ..., "required": false}`, which vanilla 1.16.2+ tag files support). No `c:` tags.
- **Perishable** lists the vanilla meats, fish, milk bucket and egg directly, plus the common
  `forge:` tags (`forge:raw_meats`, `forge:milk`, `forge:eggs`, ...).
- **Conditional grind recipes** use Forge's top-level `"conditions"` key with `forge:not` +
  `forge:tag_empty`. On 1.16.5 tags are bound before recipes are parsed, so this sees the real tag.
- **Icebox** is a ticking `LockableTileEntity` (`ITickableTileEntity`) on a `ContainerBlock`; hoppers get
  a `SidedInvWrapper` per face through the item-handler capability (top/bottom = food, sides = ice
  slot only). The screen draws with `MatrixStack` and the bound chest texture.
- **No `mineable/axe` tag** before 1.17: the block uses Forge's `harvestTool(ToolType.AXE)` instead.
- **Creative tab** is an `ItemGroup` subclass whose `fillItemList` keeps main's order.
- **Client setup:** `StevesPantry` calls `StevesPantryClient.init` on the physical client; the screen is
  registered with `ScreenManager.register` in `FMLClientSetupEvent`.
- Data folders use the old plural names (`recipes/`, `loot_tables/`, `tags/items/`), recipes have no
  `category` and use `"item"` results, and `pack.mcmeta` uses pack format 6.

## API

Other mods can read and extend Steve's Pantry through `com.naverene.stevespantry.api`, the same
package, type names and methods as main's [API.md](../API.md) (`StevesPantryApi.VERSION` = 1).
Everything outside that package is internal. `./gradlew build` also writes
`build/libs/stevespantry-1.16.5-0.1.0-api.jar` (API classes in official mappings, plus sources) to
compile against.

```java
if (StevesPantryApi.isAvailable()) {          // false when Steve's Pantry isn't installed
    IPantryApi pantry = StevesPantryApi.get();
    pantry.registerCoolant(MyItems.FREEZER_PACK.get(), 48000);
    pantry.spice("chili").ifPresent(chili -> pantry.registerSpiceItem(MyItems.CHILI_POWDER.get(), chili));
    pantry.registerShelfLifeModifier((foods, spices, life) ->
            foods.stream().anyMatch(f -> f.getItem() == MyItems.SALT.get()) ? life * 2 : life);
}
```

Spices (`spices`, `spice`, `spiceOf`, `spiceStack`, `registerSpiceItem`), dishes (`isDish`, `dish`,
`makeDish`), freshness (`ages`, `stamp`, `stage`, `remaining`, `ticksLeft`, `chill`), shelf life
(`perishableTag`, `isPerishable`, `registerShelfLifeModifier`) and cold storage (`registerCoolant`,
`coolantTicks`) work as on main. Registered spice items count in the dish recipe, shelf-life
modifiers run on every assembled dish, and registered coolants work in the Icebox. Do registrations
during mod construction or common setup. Differences on 1.16.5:

- Freshness methods take a `World` instead of a `Level`.
- `ISpice#effect()` returns an `Effect` (no `Holder` on 1.16.5).
- `perishableTag()` returns an `ITag.INamedTag<Item>` (Forge's optional named tag for
  `stevespantry:perishable`) instead of a `TagKey<Item>`.
- `DishView` is a small immutable class with the record's accessors (`ingredients()`, `spices()`,
  `perishable()`), since this version compiles as Java 8.
