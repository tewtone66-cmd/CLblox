# Yolk Rush (Roblox game)

Jump to the egg island, break eggs with your pickaxe, grab the pet, outrun the guard and bring the pet back to the start line for cash. Spend cash on pickaxe, speed and jump upgrades.

These scripts are written for **Roblox Studio** (PC/Mac). Nothing here touches the Roblox client; it is a normal Roblox experience.

## Where each script goes

| File | Studio location | Type |
|---|---|---|
| `ReplicatedStorage/Config.lua` | ReplicatedStorage, named `Config` | ModuleScript |
| `ServerScriptService/DataService.lua` | ServerScriptService, named `DataService` | ModuleScript |
| `ServerScriptService/Main.server.lua` | ServerScriptService, named `Main` | Script |
| `StarterPlayerScripts/HUD.client.lua` | StarterPlayer > StarterPlayerScripts, named `HUD` | LocalScript |

## Build the world (in Studio)

1. **Eggs:** make a Part (or Model) for each egg. Open View > Tag Editor, add the tag `Egg`. Select it, and add a number attribute `Tier` (1, 2 or 3). Higher tiers need a better pickaxe and have faster guards (see `Config.Tiers`).
2. **Start line / pen:** make a flat Part where players deliver pets. Add the tag `DeliveryZone`.
3. **Guard:** Avatar tab > Rig Builder > add a rig, rename it `Guard`, make sure no part is Anchored, drag it into **ServerStorage**. (A copy is spawned each time an egg breaks.)
4. **Obby:** build the jumping course between the start line and the egg island. Players start with low jump power, so make early gaps easy.
5. Put a SpawnLocation near the start line.

## Saving progress

- File > Publish to Roblox first.
- Game Settings > Security > turn on **Enable Studio Access to API Services**.

## How to play / controls

- PC: click or press `E` near an egg to swing.
- Mobile: tap the SWING button.

## Tuning

Everything numeric (costs, egg health, rewards, guard speed, cooldowns) is in `Config.lua`.

> Before publishing, search the name "Yolk Rush" on Roblox to make sure it is not already taken.
