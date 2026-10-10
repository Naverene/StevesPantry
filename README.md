# Steve's Pantry

Spices and build-your-own dishes for Minecraft **1.21.1 / NeoForge**, made to pair with
Pam's HarvestCraft 2. See [DESIGN.md](DESIGN.md) for how everything works, and [API.md](API.md)
for hooking another mod into it.

## Build

Needs Java 21.

```
./gradlew build        # jar lands in build/libs/
./gradlew runClient    # dev client
```

The NeoForge version is in `gradle.properties` (`neo_version`); bump it to the latest 21.1.x if
Gradle can't resolve it. The GitHub Actions workflow in `.github/workflows/build.yml` builds the
jar on every push.

To test with HarvestCraft, drop the HC2 Crops, Trees and Food Core NeoForge 1.21.1 jars into
`run/mods/`.
