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
| Saffron | *Crocus sativus* | Luck | any mod's `c:crops/` tag, or the wandering trader |
| Cumin | *Cuminum cyminum* | +1.5 saturation | any mod's `c:crops/` tag, or the wandering trader |
| Turmeric | *Curcuma longa* | Health Boost | any mod's `c:crops/` tag, or the wandering trader |
| Cardamom | *Elettaria cardamomum* | Jump Boost | any mod's `c:crops/` tag, or the wandering trader |
| Clove | *Syzygium aromaticum* | keeps 100% longer | any mod's `c:crops/` tag, or the wandering trader |
| Star Anise | *Illicium verum* | Water Breathing | any mod's `c:crops/` tag, or the wandering trader |

**Getting spices.** Craft a **Mortar and Pestle** (stick over a stone bowl shape). Put it in the
grid with a source crop to get 2 spice; the mortar stays in the grid and wears down (128 uses).

Each spice's sources are an item tag, `stevespantry:spice_sources/<spice>`. It lists the
HarvestCraft item (where there is one) and the shared `c:` tags other mods use, such as
`c:crops/ginger`, `c:crops/chili_pepper` or `c:seeds/mustard`. Every entry is optional, so Pam's
isn't required: any mod that puts its ginger in `c:crops/ginger` can be ground into Ground Ginger.
Modpacks can add more sources with a datapack. A grind recipe only loads when its tag has
something in it. The wandering trader sells every spice, which covers the rare ones until a mod
provides them.

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
- **Perishable** ingredients (meat, fish, milk, eggs) halve it. They're the item tag
  `stevespantry:perishable`, built from the shared `c:` tags (`c:foods/raw_meat`,
  `c:foods/cooked_fish`, `c:drinks/milk`, `c:eggs` and so on), so other mods' meat counts too.
  Perishable dishes say so in their tooltip.
- The clock is world game time, so food doesn't rot while the server is off, but does rot in chests.
- A freshness bar sits where a tool's durability bar would, green to red.
- **Fresh** (more than half left): full nutrition and every spice effect (1 minute each).
- **Stale** (under half): still feeds you, but the spices have gone flat, so no effects.
- **Spoiled**: turns into **Spoiled Leftovers** in your inventory (1 hunger, likely Hunger and
  maybe Nausea). A spoiled dish pulled from a chest turns the next tick.

### Icebox

An unpowered, chest-sized block: 27 food slots plus a separate **ice slot** in a side panel with
a cold gauge. Dishes inside spoil **3x slower** while there's ice, so a perishable dish kept cold
outlasts a dry one left on the shelf. Recipe: planks in the corners, iron ingots on the edges,
packed ice in the middle.

- **Ice** melts like furnace fuel, and only while there's a dish inside. Each item is worth some
  chilling time: snowball 1/4 day, snow block or ice 1 day, packed ice 9 days, blue ice 81 days.
  With no ice left, dishes spoil at the normal rate. Hovering the gauge shows the days left.
- It works by giving back two thirds of the chilled time to each dish's freshness once a second,
  measured from its last chill (saved with the block), so it keeps working while its chunk is
  unloaded, melting ice for that time too.
- Hoppers above or below reach the food slots; hoppers on the sides feed the ice slot (and can't
  pull ice out). Comparators work like a chest.

### Walk-in Freezer

A powered multiblock, GregTech style but with no GregTech dependency. It works like GregTech's
cleanroom: it has no storage of its own. Chests, barrels and other containers (modded storage too)
placed **inside the room** spoil **10x slower** while it has power (the Icebox is 3x). It uses the
same refund-the-elapsed-time trick as the Icebox (`block/Chiller.java`, shared by both), so it also
keeps working while unloaded, as long as it had the power to cover that time.

- **Shape**: a hollow cube. The walk-in size is **5x5x5** with a 3x3x3 room inside; a compact
  **3x3x3** with one block inside (room for a chest) also works. The **Controller** sits in the
  middle of one wall, facing out. The rest of the shell is **Freezer Casing**, or any mix of buses
  and hatches. A walk-in freezer can have doors (any door) in its side walls, anywhere but the edges.
  Anything can go inside the room.
- Only the controller, buses and hatches have block entities. Casing is a plain block.
- **Controller**: right-click says whether it's running, how many containers it's chilling and how
  much power is stored, or what's wrong with the shell. It re-checks the shell once a second.
- **Energy Hatch** (at least one): takes Forge Energy (FE) from any cable, stores 100,000 FE,
  accepts up to 1,000 FE/t. The freezer draws a flat **20 FE/t**, only while there's food inside.
  Right-click shows the charge. Without power, or with a broken shell, food inside ages normally.
- **Input / Output Bus** (optional, 9 slots, middle of a wall only): pass-through hatches. An input
  bus takes items from hoppers and pipes outside and pushes them into the container right behind it
  inside the room. An output bus pulls from the container behind it, for hoppers and pipes outside.
  Food sitting in a bus is chilled too.
- An Icebox inside the freezer keeps its own 3x and isn't chilled again.
- Recipes: 5 iron + 4 packed ice make 4 casings; the controller is an Icebox and a comparator in a
  ring of casing; buses are a hopper above (input) or below (output) a casing; the energy hatch is a
  redstone block on a casing.

## Next steps

1. Grow the rare spices ourselves (other mods can already supply them through `c:` tags): saffron crocus, cumin, turmeric, cardamom crops; clove and star anise trees.
2. Tint the dish texture from its ingredients, like Tinkers' part colours.
3. Signature combos: named recipes (curry, chai, pumpkin spice) that give bonus effects when a dish
   has the right spice set.
4. More ways to slow spoilage: a pantry/cellar block, salting with HarvestCraft salt.
5. Config for shelf life and effect lengths.
6. JEI/EMI page for dish assembly.
