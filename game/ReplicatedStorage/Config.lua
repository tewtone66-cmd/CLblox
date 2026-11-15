-- ReplicatedStorage/Config (ModuleScript)
local Config = {}

Config.SwingCooldown = 0.35      -- seconds between pickaxe swings
Config.SwingRange = 14           -- studs from player to egg
Config.EggRespawn = 20           -- seconds until a broken egg returns
Config.GuardTimeout = 45         -- guard gives up after this many seconds
Config.GuardCatchDistance = 4    -- studs: guard catches you at this distance

-- Upgrade levels. Pickaxe starts at 1, Speed and Jump start at 0.
Config.Upgrades = {
    Pickaxe = { Label = 'Pickaxe', BaseCost = 50, Growth = 1.7, Max = 10 },
    Speed = { Label = 'Speed', BaseCost = 30, Growth = 1.5, Max = 15 },
    Jump = { Label = 'Jump', BaseCost = 30, Growth = 1.5, Max = 15 },
}

-- Egg tiers. Pickaxe = minimum pickaxe level needed to damage this egg.
Config.Tiers = {
    [1] = {
        Name = 'Plain Egg', HP = 60, Pickaxe = 1, Reward = { 20, 40 }, GuardSpeed = 14,
        Color = Color3.fromRGB(255, 214, 107), Pets = { 'Chick', 'Duckling' },
    },
    [2] = {
        Name = 'Spotted Egg', HP = 160, Pickaxe = 3, Reward = { 60, 120 }, GuardSpeed = 17,
        Color = Color3.fromRGB(94, 230, 195), Pets = { 'Lizard', 'Baby Turtle' },
    },
    [3] = {
        Name = 'Golden Egg', HP = 400, Pickaxe = 6, Reward = { 180, 360 }, GuardSpeed = 20,
        Color = Color3.fromRGB(255, 107, 94), Pets = { 'Baby Dino', 'Dragonet' },
    },
}

-- Cost to go from `level` to `level + 1`. Returns nil at max level.
function Config.CostFor(name, level)
    local u = Config.Upgrades[name]
    if not u or level >= u.Max then
        return nil
    end
    return math.floor(u.BaseCost * (u.Growth ^ level))
end

function Config.WalkSpeed(speedLevel)
    return 16 + speedLevel * 1.5
end

function Config.JumpPower(jumpLevel)
    return 50 + jumpLevel * 4
end

function Config.PickaxeDamage(pickaxeLevel)
    return 10 + pickaxeLevel * 8
end

return Config
