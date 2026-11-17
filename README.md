# Auto Helmet Swap

Client-side Fabric mod for Minecraft 26.2. Three keybinds (Options → Controls → Auto Helmet Swap), each toggling the helmet slot between two items:

| Keybind (default) | Toggles |
|---|---|
| Z | Superman Head ↔ Jack Head |
| V | Netherite Helmet ↔ Superman Head |
| B | Netherite Helmet ↔ Jack Head |

- Superman Head = any item named `Сила Супермена`, Jack Head = any item named `Голова Джека`.
- Netherite Helmet = item type `minecraft:netherite_helmet` (a netherite helmet carrying one of the two head names counts as that head).
- Swaps use normal inventory clicks, so the full item stack (enchantments, name, lore, all components) is moved unchanged. Nothing is added to the game: no items, no recipes.

## Requirements

Minecraft 26.2, Fabric Loader 0.20.1+, Fabric API 0.161.0+26.2 (any 26.2 build), Java 25.

## Build

    ./gradlew build        # Windows: gradlew.bat build

The mod JAR is `build/libs/auto-helmet-swap-1.0.0.jar` (ignore the `-sources` JAR). Drop it into `.minecraft/mods`.

Without a local Java 25: push this folder to a GitHub repository, the workflow in `.github/workflows/build.yml` builds the JAR and attaches it to the run as the `auto-helmet-swap` artifact.
