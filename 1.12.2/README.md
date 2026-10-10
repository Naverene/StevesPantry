# Steve's Pantry for Minecraft 1.12.2 (Forge)

| | |
|---|---|
| Minecraft | 1.12.2 |
| Loader | Forge 14.23.5.2847 or newer (2860+ recommended, latest 2864) |
| Build | RetroFuturaGradle 1.4.9, Gradle 8.14.3 (wrapper), MCP `stable_39` mappings |
| Java | 8 (Gradle downloads an Azul JDK 8 through the foojay toolchain resolver; Gradle itself runs on JDK 17/21) |

Pam's HarvestCraft 1.12.2 is optional.

## Build and run

```sh
./gradlew build          # jar in build/libs/stevespantry-1.12.2-0.1.0.jar (the -dev jar is deobfuscated)
./gradlew runClient
./gradlew runServer --args nogui   # run dir is run/; put eula=true in run/eula.txt first
```

RetroFuturaGradle fixes the dev environment at Forge 14.23.5.2847; nothing in the mod needs a
newer build, so the jar declares `required-after:forge@[14.23.5.2847,)`. (RFG 2.x needs Gradle to
run on Java 25, so this uses the last 1.x release with Gradle 8.)

## Differences from main (NeoForge 1.21.1)

- **No data components: dish data lives in item NBT.** Same field names as main's components:
  `ingredients` (list of `{id, Damage}` compounds, since 1.12.2 items still carry metadata, e.g. the
  four raw fish), `spices` (list of spice ids), `made_at`, `shelf_life`, plus the computed
  `nutrition`, `saturation` (saturation points, as on main) and `eat_ticks`. `DishContents` and
  `Freshness` are plain classes with `get(stack)`/`set(stack)`.
- **Per-stack food values.** `DishItem` extends `ItemFood` and overrides the stack-aware
  `getHealAmount`/`getSaturationModifier` (points turned back into a modifier) and
  `getMaxItemUseDuration`, which is what `FoodStats` reads. Eating returns a bowl like vanilla stews.
  An empty Dish (e.g. from `/give`) can't be eaten. "Any edible item" means any `ItemFood`.
- **No item tags: the Ore Dictionary instead** (`ModTags`). Spice sources are Pam's HarvestCraft
  1.12.2 ore names, checked against its source (`cropPeppercorn`, `cropCinnamon`, `cropNutmeg`,
  `cropVanillabean`, `cropGinger`, `cropMustard`/`seedMustard`, `cropSesame`/`seedSesameseed`,
  `cropBellpepper`, `cropChilipepper`, `cropGarlic`) plus the usual alternatives other mods use
  (`cropSaffron`, `cropCumin`, `cropTurmeric`, `cropCardamom`, `cropClove`, `cropStaranise`,
  `spice<Name>` ...). Perishables are HarvestCraft's `listAllmeatraw`/`listAllmeatcooked`/
  `listAllfishraw`/`listAllfishcooked`/`listAllfishfresh`/`listAllmilk`/`listAllheavycream`/
  `listAllegg` (plus `foodMilk`, `egg`) and the vanilla meats, fish, rabbit stew, milk bucket and egg.
- **Grind recipes are registered in code** as `ShapelessOreRecipe`s (mortar with wildcard damage +
  the spice's source ore names combined), in `RegistryEvent.Register<IRecipe>` at lowest priority,
  and only for spices whose ore names have items by then (HarvestCraft fills its ore names while
  registering items). That's the stand-in for main's "tag isn't empty" condition. The mortar's
  wear uses `getContainerItem`.
- **Dish recipe** is an `IRecipe` registered through `RegistryEvent`, marked dynamic (no recipe book).
  Mortar and icebox recipes are JSON in `assets/stevespantry/recipes/` (icebox uses `forge:ore_shaped`
  with `plankWood`/`ingotIron`; the mortar uses smooth stone, `minecraft:stone` data 0).
- **No wandering trader in 1.12.2.** The Farmer career sells the spices instead (`SpiceTrades`,
  via `VillagerRegistry`): a random common spice at career level 2 (1 emerald -> 4) and a random rare
  one at level 3 (3 emeralds -> 2), 8 uses each.
- **Icebox** is a `BlockContainer` + `TileEntityLockable`/`ISidedInventory`/`ITickable`; hoppers use
  `SidedInvWrapper` capabilities (top/bottom = food, sides = ice, insert only). GUI through
  `IGuiHandler` on the sided proxy; the cold gauge syncs as window properties. No blue ice in
  1.12.2, so coolants are snowball, snow block, ice and packed ice. No block loot tables or
  mineable tags here: the block drops itself and `Material.WOOD` + an axe harvest level make the axe
  the right tool. Named iceboxes keep their name.
- **Assets** use 1.12.2 names: `en_us.lang`, `textures/items`, `textures/blocks`, a `normal`
  blockstate variant, plus `mcmod.info` and `pack.mcmeta`. Item models are registered in
  `ModelRegistryEvent`.
- **Registration**: `ModRegistries` fields are the objects themselves, registered from Forge's
  registry events; the creative tab is a `CreativeTabs` listing items in main's order.
- **Spoiled Leftovers** is an `ItemFood` that rolls its Hunger (80%) and Nausea (30%) in
  `onFoodEaten`, since 1.12.2 foods only take one potion effect.
- `main`'s `stevespantry:spices` tag has no counterpart (nothing in the code reads it).
