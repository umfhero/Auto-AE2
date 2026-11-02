# Auto AE2

An Applied Energistics 2 add-on for Minecraft 26.1.2, NeoForge 26.1.2.109 and AE2 26.1.13-beta. Open the folder as a Gradle project in IntelliJ and select JDK 25 for Gradle. Run the IntelliJ **Client** configuration to use the existing development world. Build using `gradlew.bat build`; the mod jar is produced in `build/libs`.

The local development Client includes [JEI](https://modrinth.com/mod/jei) for browsing recipes and uses, [Jade](https://modrinth.com/mod/jade) for identifying blocks and entities, and [Mouse Tweaks](https://modrinth.com/mod/mouse-tweaks) for inventory handling. Hover an item and press R for its recipes or U for its uses; mouse-wheel and shift-drag shortcuts help move items. JEI's MezzConfig dependency is also installed. These helper mods are in `run/mods` and are not bundled as requirements of the finished mod. If the folder is absent in another checkout, install compatible NeoForge 26.1.2 versions separately.

The Automation Planner is crafted with four iron ingots, a quartz glass, two fluix crystals, a crafting table and a logic processor. The Placement Wrench uses two iron ingots, a fluix crystal and a certus quartz crystal. Look them up in JEI for the crafting layouts.

## Simple Certus Farm

The planner offers a Simple Certus Farm based on the [AE2 guide](https://guide.appliedenergistics.org/1.20.1/example-setups/simple-certus-farm/). Connect the planner to a powered ME network with the required items in its storage. The book shows the materials and their availability; red means some are missing and green means all 18 are available. Insert a Placement Wrench and select **Link**; materials are not required to link. After returning the wrench to your inventory, select a blueprint and size in that planner without reinserting it. Each successful placement draws a fresh set from ME storage. The planner must be present, loaded and in the same dimension as the wrench.

Aim at an existing budding Certus Quartz block and press **G** with the wrench in your main hand to open placement controls. Adjust or rotate the hologram and select **Place**. Leave the surrounding space clear and connect the exposed main cable to your ME network after construction. The farm's storage bus is configured with priority 1000; a higher-priority destination on your network may affect where its output goes. Flawless budding quartz normally cannot be collected intact, so build around it where it is found. Other budding blocks eventually degrade and need replacing.

Drag the planner's 3D preview to look around the farm. In placement controls, use the left/right arrow keys to move sideways, up/down to move further or closer, Shift+Up/Down to change height, and R to rotate. The controls are also clickable; hover over a button for its purpose.

The planner does not take construction materials from neighbouring chests or barrels. Wrenches linked to the old oak-log test retain their previous blueprint until relinked.

The catalogue also offers two controller designs, assembler/provider modules, crafting CPUs, charger automation and processor production. Adjustable designs default to their largest offered preset. The 127-block Controller Array is relatively open; Controller Cube offers three starter columns followed by hollow 3x3x3 to 7x7x7 frames. These are offered designs, not AE2's absolute size limits. Place a controller away from existing network nodes and plan its connection to avoid a controller conflict. Keep the CPU separate from existing CPUs. A single crafting storage block is a valid CPU; co-processors are optional and improve parallel work. The assembler needs encoded patterns and a connected CPU; the charger needs a processing pattern and a connection from its exposed main cable to the main network. Other designs in BLUEPRINT_RESEARCH.md are not yet implemented.

## Processor production

Choose Processor Production and click **P** in the preview to select how many of each press to install, up to eight of each type. The planner shows the number currently in ME storage. Up to five pressed inscribers share a pattern provider; an additional unpressed inscriber and its own provider perform final assembly. Connect the dense backbone to a powered network with enough channels. The presses are included in the requirements and installed during placement.

The planner includes blank patterns in the cost and encodes the printing and final assembly patterns into the appropriate providers during placement. It validates the standard inscriber recipes first; if a modpack changes them, placement stops instead of installing an incorrect pattern. The inscribers are configured to export their outputs. Connect the dense cable to the main ME network. The user has tested the processor layout and automatic pattern installation in the existing test world.

## Furnace automation

Select one to eight furnaces and connect the dense backbone to a powered network. Up to four furnaces share a provider, with a hopper above each furnace to deliver the input. Export buses supply coal from ME storage, and import buses return the output. Add suitable processing patterns to the providers and supply coal. This layout still needs a full in-game crafting test.

## Checking and crafting materials

Open the book to see item icons, available counts and a plus sign for items with a known ME crafting pattern. With JEI installed, hover an item and use its configured recipe, uses and bookmark keys (normally R, U and A). The **Craft** button opens AE2's crafting confirmation for missing materials that have a known pattern; requirements without a pattern are skipped and must be supplied separately. AE2 may ask for confirmation for each requested ingredient. The planner selects a compatible cable colour from ME storage or known crafting patterns and updates the requirements and preview. Each connected cable group uses a single colour across its smart and dense cables. If there is no suitable stock or pattern, a single network defaults to Fluix. The farm and charger use two different non-Fluix colours to keep their networks separate. Mixed colours within a group are not currently supported.
