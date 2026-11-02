# Auto AE2 project notes

## Project and working practices

Auto AE2 is a work-in-progress Applied Energistics 2 helper for Minecraft 26.1.2, NeoForge 26.1.2.109 and Java 25. The pinned AE2 dependency is 26.1.13-beta and GuideME 26.1.14-beta is used for previews. Keep dependency versions and the supported ranges in the metadata consistent with the intended ATM11 version, and check the current target before changing them. The mod ID is `autoae2`, and the Java package is `dev.umf.autoae2`.

Keep this file updated when architecture, functionality, dependencies, testing status or user-facing decisions change. Read `SETUP.md` for gameplay instructions and `BLUEPRINT_RESEARCH.md` for research, but verify them against the implementation when changing behaviour.

The development world is user-owned. Use the IntelliJ **Client** configuration and the existing `Auto AE2 Test` world for in-world checks. Do not create another world as a substitute. Save and back up the world before modifying it, and do not interrupt the user's active game. `run/`, `run-validation/`, `.backups/`, IDE configuration, Gradle caches and build output must not be published. JEI, Jade, Mouse Tweaks and MezzConfig in `run/mods` are local development aids and are not bundled.

Build and run the unit tests with JDK 25 and the included Gradle wrapper: `./gradlew build` on Linux or `gradlew.bat build` on Windows. Test an IntelliJ Client launch when changing runtime integration. The GitHub Actions workflow runs the wrapper with Temurin 25. The produced jar is under `build/libs`. The mod currently declares All Rights Reserved; do not change the licence without the owner's instruction.

## Player workflow

The mod adds an Automation Planner and a Placement Wrench, both with early-game recipes and their own creative tab. Place the planner on a powered AE2 network. Its animated connected state follows its active grid node. The planner reads materials from its connected ME network, not adjacent chests. A wrench is linked by inserting it in the planner; linking is free, and the wrench follows that planner's selected design while its chunk is loaded. Each placement draws a fresh set of materials and the planner must be loaded in the same dimension.

The planner has light and dark themes, a draggable 3D preview, requirements with JEI item interaction, AE2 pattern availability and a Craft button. Craft opens AE2's own confirmation for craftable missing items and skips items without a known ME pattern. The user tested Craft with an inscriber pattern. Cable choices prefer available or craftable stock. A connected cable group uses one colour, while the farm and charger use two different non-Fluix colours to preserve isolation; mixing colours within a group is not supported.

Hold a linked wrench to see the placement hologram and press G for controls. Arrow keys move the hologram, Shift plus Up/Down changes height, R rotates, and the controls provide reset, place and close. Failures appear in the placement panel or the action bar. The server validates the link, selection, materials, permissions and clearance. Never force-load the planner chunk or replace the budding block used by a Certus farm.

## Blueprints and status

- Simple Certus Farm follows AE2's published simple farm. It builds around an existing bud, configures storage buses and needs a connection to the main network. Degrading buds need replacing.
- The controller uses an Array/Cube toggle. Array offers 1 to 127 connected controllers. Cube offers eight stages: 1, 2 or 3 in a column, then hollow 3x3x3 to 7x7x7 edge frames (20, 32, 44, 56 and 68 controllers). These are offered designs, not a claim of the AE2 maximum. Place controllers away from other network nodes and connect them carefully.
- Assembler/provider modules grow from a starter pair to a 2x2x9 checkerboard. A connected crafting CPU and patterns are still required. CPU designs run from a single 1k storage block to a 3x3x3 unit with 256k storage and 26 co-processors; keep separate CPUs apart.
- Charger automation follows the AE2 guide topology and requires its pattern and main-network connection.
- Processor Production lets the player choose 0 to 8 of each press, placing up to five pressed inscribers beside each provider and a separate unpressed assembly station. Blank patterns are included in the cost. `ProcessorPatterns` validates standard inscriber recipes, then encodes printing and final assembly patterns into their matching providers. Placement stops if the installed recipes differ. The user reported the processor setup and automatic pattern installation worked; connect its dense cable to the main network.
- Furnace Automation offers 1 to 8 furnaces. Up to four share a provider using hoppers above the furnaces; coal is supplied by export buses and output returned by import buses. Supply coal, patterns and power. End-to-end in-game crafting remains unverified.

## Implementation map

- `AutoAE2`, `AutoAE2Client` and resources register blocks, items, menus, packets, recipes, rendering and the creative tab.
- `PlannerBlock`, `PlannerBlockEntity`, `PlannerMenu` and `PlannerScreen` manage the AE node, persistent selection, server-synchronised inventory and UI. Preserve the planner link identity and existing saved data compatibility.
- `BlueprintCatalog` defines placeable cells, parts, requirements and adjustable stages. `CertusFarm` and `FarmLayout` define the separate farm. Changes must keep costs, previews, holograms and placement consistent. Tests in `BlueprintCatalogTest` check topology, costs and rotations.
- `CableChoices` chooses cable IDs and colours. Maintain isolation between farm and charger networks, and keep preview, requirements, wrench, actual extraction and placed parts in sync.
- `PlacementWrench`, `PendingPlacements`, `AdjustPlacement`, `PlacementKeys`, `PlacementControlsScreen`, `PlacementHologram` and `PlacementFeedback` implement linking and placement UX. Placement is authoritative on the server; clients only preview.
- `ModuleBuilder` and `FarmBuilder` preflight, withdraw and place with rollback. Processor setup and automatic pattern encoding are in `ProcessorPatterns`. Recheck atomicity, inventory ownership and refunds when changing placement.
- `FarmPreview` uses GuideME to render an isolated scene with AE2 parts and does not alter the world. The hologram uses simplified translucent geometry and textures. `PlannerJeiPlugin` is an optional compile-only JEI integration.

## Known limits

The project is still in development. Several layouts have not had full live crafting validation, particularly furnace automation and the available-pattern/alternative-colour paths. Do not describe all blueprints or modpack compatibility as tested. Installed recipes can vary between modpacks. The optional JEI API is compile-only; the mod must remain loadable without JEI. The controller selection model changed from a 257-count cube to eight discrete stages; saved larger values clamp to the last stage and older linked wrenches synchronise with their planner.
