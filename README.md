# Donut Macro (Fabric, Minecraft 1.21.4)

Press **F** in-game to open the menu:

1. **Item to sell** - item id, e.g. `minecraft:diamond`
2. **Auction house price** - sent as `/ah sell <price>` (e.g. `1000`, `1k`, `2.5m`)
3. **Fill orders** ON/OFF + **max items** it may move into orders per run (rounded to whole stacks)
4. **Pick up ground items** ON/OFF - walks to matching dropped items so they land in your inventory and get sold

Click **Save & start macro** to run it. Press F and click **Stop macro** to stop.
Settings are saved to `config/donutmacro.json`.

## How the loop works
- Item in inventory + orders ON + under the cap -> `/orders`, click an order for the item, shift-click stacks in.
- Item in inventory otherwise -> hold it and run `/ah sell <price>`.
- Nothing in inventory + ground pickup ON -> walk to the nearest matching dropped item (16 block radius).

## Build the jar (no local setup)
1. Create a GitHub repo and upload this whole folder (including the `.github` folder).
2. Open the **Actions** tab -> **build** -> **Run workflow**, then download the `donutmacro-jar` artifact.
3. Unzip it. Use `donutmacro-1.0.0.jar` (not the `-sources` one).

## Build locally
Install JDK 21, then run `./gradlew build` (`gradlew.bat build` on Windows). Jar is in `build/libs/`.

## Install with the Modrinth App
1. Create a **Fabric 1.21.4** profile.
2. Profile -> **Add content** -> search and add **Fabric API**.
3. Add content -> **Add from file** -> pick `donutmacro-1.0.0.jar`.
4. Launch and join Donut SMP.

## Notes
- F is also Minecraft's default "swap hands" key. On first launch the mod unbinds swap-hands so F works
  for the macro. Re-bind it any time in Options -> Controls.
- The /orders clicking is best-effort: it clicks the first menu slot holding your item and
  shift-clicks matching items in. If Donut's menu layout differs, adjust `MacroRunner.handleOrders`.
- If /orders fails, it is skipped for 30 seconds so selling keeps going.
- If Donut updates to a newer Minecraft version, change versions in `gradle.properties`.
- Automation can break server rules. Check Donut SMP's rules before using this.
