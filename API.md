# Steve's Pantry API

Other mods can read and extend Steve's Pantry through the classes in
`com.naverene.stevespantry.api`. Everything outside that package is internal and may change.

## Getting it

`./gradlew build` also produces `stevespantry-<version>-api.jar` (API classes plus sources). Compile
against that, and put the full mod jar on your runtime classpath:

```groovy
dependencies {
    compileOnly files('libs/stevespantry-0.1.0-api.jar')
    runtimeOnly files('libs/stevespantry-0.1.0.jar')
}
```

```java
if (StevesPantryApi.isAvailable()) {          // false when Steve's Pantry isn't installed
    IPantryApi pantry = StevesPantryApi.get();
}
```

The API is safe to fetch at any point, including your mod's constructor, whatever order mods load
in. `StevesPantryApi.VERSION` is bumped when the API changes. Do registrations during mod
construction or common setup, before a world loads.

## What's in it

| Area | Methods | Use it for |
|---|---|---|
| Spices | `spices()`, `spice(id)`, `spiceOf(stack)`, `spiceStack(spice, n)` | Listing spices with their Latin names, colours and bonuses (`ISpice`); turning a stack into a spice. |
| | `registerSpiceItem(item, spice)` | Letting your own item season dishes as one of our spices (your ground pepper counts as `black_pepper`). |
| Dishes | `isDish(stack)`, `dish(stack)` | Reading a dish's ingredients, spices and whether it's perishable (`DishView`). |
| | `makeDish(foods, spices)` | Cooking machines: builds a dish exactly as the crafting grid would, or returns empty if the rules aren't met. |
| Freshness | `ages`, `stamp`, `stage`, `remaining`, `ticksLeft` | Reading how fresh a dish is (`FreshnessStage`: FRESH, STALE, SPOILED). |
| | `chill(stack, ticks, level)` | Your own cold storage: give back `(n-1)/n` of elapsed ticks to make food age at `1/n` speed. |
| Shelf life | `perishableTag()`, `isPerishable(stack)` | The `stevespantry:perishable` item tag (add to it with a datapack). |
| | `registerShelfLifeModifier(modifier)` | Adjusting every new dish's shelf life (a preservative ingredient, a seasonal rule). |
| Cold storage | `registerCoolant(item, ticks)`, `coolantTicks(stack)` | Making your item work as Icebox ice. An ice block is 24000 ticks (one day). |

## Example

```java
IPantryApi pantry = StevesPantryApi.get();
pantry.registerCoolant(MyItems.FREEZER_PACK.get(), 48000);
pantry.spice("chili").ifPresent(chili -> pantry.registerSpiceItem(MyItems.CHILI_POWDER.get(), chili));
pantry.registerShelfLifeModifier((foods, spices, life) ->
        foods.stream().anyMatch(f -> f.is(MyItems.SALT.get())) ? life * 2 : life);
```
