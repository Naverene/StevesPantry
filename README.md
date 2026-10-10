# Steve's Pantry

Spices and build-your-own dishes for Minecraft **1.21.1 / NeoForge**, made to pair with
Pam's HarvestCraft 2. See [DESIGN.md](DESIGN.md) for how everything works.

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

## Other Minecraft versions

Each port lives in its own folder, named after its Minecraft version, and is a complete Gradle
project: `cd` into it and run `./gradlew build`. Each folder's README lists how that version
differs from this one. The `.github/workflows/ports.yml` workflow builds every folder.

| Folder | Loader | Java | Spice sources |
|---|---|---|---|
| (root) | NeoForge 1.21.1 | 21 | `c:` tags |
| `26.3` | NeoForge 26.3 (beta) | 25 | `c:` tags |
| `26.1.2` | NeoForge 26.1.2 | 25 | `c:` tags |
| `1.20.1` | Forge 47.4 | 17 | `forge:` tags |
| `1.20.1-fabric` | Fabric 1.20.1 | 17 | `c:` tags |
| `1.19.2` | Forge 43.5 | 17 | `forge:` tags |
| `1.18.2` | Forge 40.3 | 17 | `forge:` tags |
| `1.16.5` | Forge 36.2 | 8 | `forge:` tags |
| `1.12.2` | Forge 14.23.5 | 8 | Ore Dictionary |
| `1.7.10` | Forge 10.13.4 | 8 | Ore Dictionary |

There's no wandering trader in 1.12.2 or 1.7.10, so farmer villagers sell the spices there.
