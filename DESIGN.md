# Steve's Pantry: design

A food mod for **NeoForge 1.21.1** that adds spices and player-assembled dishes, built to sit
alongside **Pam's HarvestCraft 2** (Crops, Trees, Food Core). HarvestCraft is optional: the mod
loads without it, but most spice sources come from its crops and trees.

## Spices

Sixteen spices. Every spice tooltip shows its botanical Latin name in grey italics, then what it
does in a dish.

| Spice | Latin name | In a dish | Source |
|---|---|---|---|
| Black Pepper | *Piper nigrum* | Haste | HC2 peppercorn |
| Cinnamon | *Cinnamomum verum* | Regeneration | HC2 cinnamon |
| Nutmeg | *Myristica fragrans* | Night Vision | HC2 nutmeg |
| Vanilla | *Vanilla planifolia* | Absorption | HC2 vanilla bean |
| Ground Ginger | *Zingiber officinale* | Resistance | HC2 ginger |
| Mustard Powder | *Sinapis alba* | Strength | HC2 mustard seeds |
| Toasted Sesame | *Sesamum indicum* | +1.5 saturation | HC2 sesame seeds |
| Paprika | *Capsicum annuum* | Fire Resistance | HC2 bell pepper |
| Chili Flakes | *Capsicum frutescens* | Speed | HC2 chili pepper |
| Garlic Powder | *Allium sativum* | keeps 50% longer, +0.5 saturation | HC2 garlic |
| Saffron | *Crocus sativus* | Luck | wandering trader (rare) |
| Cumin | *Cuminum cyminum* | +1.5 saturation | wandering trader (rare) |
| Turmeric | *Curcuma longa* | Health Boost | wandering trader (rare) |
| Cardamom | *Elettaria cardamomum* | Jump Boost | wandering trader (rare) |
| Clove | *Syzygium aromaticum* | keeps 100% longer | wandering trader (rare) |
| Star Anise | *Illicium verum* | Water Breathing | wandering trader (rare) |

**Getting spices.** Craft a **Mortar and Pestle** (stick over a stone bowl shape). Put it in the
grid with a source crop to get 2 spice; the mortar stays in the grid and wears down (128 uses).
Each source is an item tag, `stevespantry:spice_sources/<spice>`, which lists the HarvestCraft
item id and the matching `c:` tag, both optional. Modpacks can add sources with a datapack. A grind
recipe only loads when its tag has something in it. The wandering trader sells every spice.

The HarvestCraft item ids (`pamhc2crops:gingeritem` and so on) follow HC2's naming and are confirmed
for the crops; the tree ids (`pamhc2trees:cinnamonitem` etc.) follow the same pattern but still need
checking in game.

## Dishes (the Tinkers' Construct part)

There is one **Dish** item, and each dish stack carries what it was made from, the way a Tinkers'
tool carries its parts. Shapeless craft, anywhere in the grid:

- exactly 1 bowl
- 1 to 4 foods: any edible item, so every vanilla and HarvestCraft food works
- 0 to 3 different spices

The result's stats come from its parts:

- **Nutrition**: sum of the foods, +1 for each different food beyond the first, capped at 20.
- **Saturation**: sum of the foods plus spice bonuses, capped at the nutrition.
- **Eat time**: longer the more foods go in.
- **Name**: built from the ingredients, e.g. "Carrot Dish", "Spiced Beef and Potato Dish", "Spiced Salmon Medley".
- **Tooltip**: freshness, ingredient list, each spice with its Latin name and bonus.

Eating gives the bowl back.

Data lives in two item data components: `stevespantry:dish_contents` (ingredients, spices) and
`stevespantry:freshness` (when it was made, how long it keeps). Nutrition is written to the
vanilla `minecraft:food` component, so vanilla eating logic, and any mod that reads food values,
sees the real numbers.

## Spoilage

Dishes don't last forever.

- Base shelf life is **3 in-game days**. Clove doubles it, garlic adds 50%, and they stack.
- The clock is world game time, so food doesn't rot while the server is off, but does rot in chests.
- A freshness bar sits where a tool's durability bar would, green to red.
- **Fresh** (more than half left): full nutrition and every spice effect (1 minute each).
- **Stale** (under half): still feeds you, but the spices have gone flat, so no effects.
- **Spoiled**: turns into **Spoiled Leftovers** in your inventory (1 hunger, likely Hunger and
  maybe Nausea). A spoiled dish pulled from a chest turns the next tick.

## Next steps

1. Grow the rare spices: saffron crocus, cumin, turmeric, cardamom crops; clove and star anise trees.
2. Tint the dish texture from its ingredients, like Tinkers' part colours.
3. Signature combos: named recipes (curry, chai, pumpkin spice) that give bonus effects when a dish
   has the right spice set.
4. Ways to slow spoilage: a pantry/cellar block, salting with HarvestCraft salt, an icebox.
5. Config for shelf life and effect lengths.
6. JEI/EMI page for dish assembly.
