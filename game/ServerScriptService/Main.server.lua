-- ServerScriptService/Main (Script)
-- All game logic runs on the server so players cannot cheat by editing their client.
local Players = game:GetService('Players')
local ReplicatedStorage = game:GetService('ReplicatedStorage')
local ServerStorage = game:GetService('ServerStorage')
local CollectionService = game:GetService('CollectionService')

local Config = require(ReplicatedStorage:WaitForChild('Config'))
local DataService = require(script.Parent:WaitForChild('DataService'))

-- Remotes -------------------------------------------------------------------
local remotes = Instance.new('Folder')
remotes.Name = 'Remotes'
remotes.Parent = ReplicatedStorage

local swingEvent = Instance.new('RemoteEvent')
swingEvent.Name = 'Swing'
swingEvent.Parent = remotes

local buyFunction = Instance.new('RemoteFunction')
buyFunction.Name = 'Buy'
buyFunction.Parent = remotes

local notifyEvent = Instance.new('RemoteEvent')
notifyEvent.Name = 'Notify'
notifyEvent.Parent = remotes

-- State ---------------------------------------------------------------------
-- carrying[player] = { value, petName, tier, visual, guard }
local carrying = {}
local lastSwing = {}

local function notify(player, message)
    notifyEvent:FireClient(player, message)
end

local function syncAttributes(player)
    local d = DataService.Get(player)
    if not d then
        return
    end
    player:SetAttribute('Cash', d.Cash)
    player:SetAttribute('Pickaxe', d.Pickaxe)
    player:SetAttribute('Speed', d.Speed)
    player:SetAttribute('Jump', d.Jump)
end

local function applyStats(player)
    local d = DataService.Get(player)
    local char = player.Character
    local hum = char and char:FindFirstChildOfClass('Humanoid')
    if not (d and hum) then
        return
    end
    hum.UseJumpPower = true
    hum.WalkSpeed = Config.WalkSpeed(d.Speed)
    hum.JumpPower = Config.JumpPower(d.Jump)
end

local function clearCarry(player)
    local entry = carrying[player]
    if not entry then
        return
    end
    carrying[player] = nil
    if entry.visual then
        entry.visual:Destroy()
    end
    if entry.guard then
        entry.guard:Destroy()
    end
    player:SetAttribute('Carrying', '')
    player:SetAttribute('CarryValue', 0)
end

-- Eggs ----------------------------------------------------------------------
local function eggParts(egg)
    local parts = {}
    if egg:IsA('BasePart') then
        table.insert(parts, egg)
    end
    for _, d in ipairs(egg:GetDescendants()) do
        if d:IsA('BasePart') then
            table.insert(parts, d)
        end
    end
    return parts
end

local function hideEgg(egg)
    for _, p in ipairs(eggParts(egg)) do
        p:SetAttribute('WasTransparency', p.Transparency)
        p:SetAttribute('WasCollide', p.CanCollide)
        p.Transparency = 1
        p.CanCollide = false
    end
end

local function showEgg(egg)
    for _, p in ipairs(eggParts(egg)) do
        p.Transparency = p:GetAttribute('WasTransparency') or 0
        p.CanCollide = p:GetAttribute('WasCollide') ~= false
    end
end

local function setupEgg(egg)
    local tier = egg:GetAttribute('Tier') or 1
    local cfg = Config.Tiers[tier]
    if not cfg then
        warn('Egg ' .. egg:GetFullName() .. ' has an invalid Tier attribute')
        return
    end
    egg:SetAttribute('HP', cfg.HP)
    egg:SetAttribute('Broken', false)
end

for _, egg in ipairs(CollectionService:GetTagged('Egg')) do
    setupEgg(egg)
end
CollectionService:GetInstanceAddedSignal('Egg'):Connect(setupEgg)

-- Guard ---------------------------------------------------------------------
local function spawnGuard(egg, tier)
    local template = ServerStorage:FindFirstChild('Guard')
    if not template then
        warn('Put a rig named Guard in ServerStorage')
        return nil
    end
    local guard = template:Clone()
    local hum = guard:FindFirstChildOfClass('Humanoid')
    local root = guard:FindFirstChild('HumanoidRootPart')
    if not (hum and root) then
        guard:Destroy()
        warn('Guard needs a Humanoid and a HumanoidRootPart')
        return nil
    end
    guard:PivotTo(egg:GetPivot() * CFrame.new(0, 0, 8))
    guard.Parent = workspace
    hum.WalkSpeed = Config.Tiers[tier].GuardSpeed
    return guard
end

local function startChase(player, entry)
    local guard = entry.guard
    local hum = guard:FindFirstChildOfClass('Humanoid')
    local gRoot = guard:FindFirstChild('HumanoidRootPart')
    task.spawn(function()
        local started = os.clock()
        while carrying[player] == entry and guard.Parent do
            local char = player.Character
            local root = char and char:FindFirstChild('HumanoidRootPart')
            if not root then
                break
            end
            if (gRoot.Position - root.Position).Magnitude <= Config.GuardCatchDistance then
                clearCarry(player)
                notify(player, 'The guard caught you! Pet lost.')
                return
            end
            if os.clock() - started > Config.GuardTimeout then
                break
            end
            hum:MoveTo(root.Position)
            task.wait(0.2)
        end
        if carrying[player] == entry and entry.guard then
            entry.guard:Destroy()
            entry.guard = nil
            notify(player, 'The guard gave up. Get your pet home!')
        end
    end)
end

-- Breaking eggs -------------------------------------------------------------
local function makePetVisual(char, tierCfg)
    local head = char:FindFirstChild('Head')
    if not head then
        return nil
    end
    local part = Instance.new('Part')
    part.Shape = Enum.PartType.Ball
    part.Size = Vector3.new(1.6, 1.6, 1.6)
    part.Material = Enum.Material.Neon
    part.Color = tierCfg.Color
    part.CanCollide = false
    part.Massless = true
    part.CFrame = head.CFrame * CFrame.new(0, 2.6, 0)
    local weld = Instance.new('WeldConstraint')
    weld.Part0 = head
    weld.Part1 = part
    weld.Parent = part
    part.Parent = char
    return part
end

local function breakEgg(player, egg, tier)
    local cfg = Config.Tiers[tier]
    egg:SetAttribute('Broken', true)
    hideEgg(egg)
    task.delay(Config.EggRespawn, function()
        if egg.Parent then
            egg:SetAttribute('HP', cfg.HP)
            egg:SetAttribute('Broken', false)
            showEgg(egg)
        end
    end)

    local value = math.random(cfg.Reward[1], cfg.Reward[2])
    local petName = cfg.Pets[math.random(1, #cfg.Pets)]
    local entry = { value = value, petName = petName, tier = tier }
    entry.visual = makePetVisual(player.Character, cfg)
    entry.guard = spawnGuard(egg, tier)
    carrying[player] = entry
    player:SetAttribute('Carrying', petName)
    player:SetAttribute('CarryValue', value)

    if entry.guard then
        notify(player, 'You got a ' .. petName .. '! Run to the start line!')
        startChase(player, entry)
    else
        notify(player, 'You got a ' .. petName .. '! Take it to the start line.')
    end
end

swingEvent.OnServerEvent:Connect(function(player)
    local now = os.clock()
    if now - (lastSwing[player] or 0) < Config.SwingCooldown then
        return
    end
    lastSwing[player] = now

    local d = DataService.Get(player)
    local char = player.Character
    local root = char and char:FindFirstChild('HumanoidRootPart')
    if not (d and root) then
        return
    end
    if carrying[player] then
        notify(player, 'Bring your pet home first!')
        return
    end

    local best, bestDist
    for _, egg in ipairs(CollectionService:GetTagged('Egg')) do
        if egg:GetAttribute('Broken') == false then
            local dist = (egg:GetPivot().Position - root.Position).Magnitude
            if dist <= Config.SwingRange and (not bestDist or dist < bestDist) then
                best, bestDist = egg, dist
            end
        end
    end
    if not best then
        return
    end

    local tier = best:GetAttribute('Tier') or 1
    local cfg = Config.Tiers[tier]
    if not cfg then
        return
    end
    if d.Pickaxe < cfg.Pickaxe then
        notify(player, 'Need pickaxe level ' .. cfg.Pickaxe .. ' for a ' .. cfg.Name .. '.')
        return
    end

    local hp = (best:GetAttribute('HP') or cfg.HP) - Config.PickaxeDamage(d.Pickaxe)
    best:SetAttribute('HP', hp)
    if hp <= 0 then
        breakEgg(player, best, tier)
    end
end)

-- Delivery ------------------------------------------------------------------
local function inZone(zone, position)
    local rel = zone.CFrame:PointToObjectSpace(position)
    local size = zone.Size
    return math.abs(rel.X) <= size.X / 2
        and math.abs(rel.Y) <= size.Y / 2 + 4
        and math.abs(rel.Z) <= size.Z / 2
end

local function deliver(player, entry)
    local d = DataService.Get(player)
    clearCarry(player)
    if not d then
        return
    end
    d.Cash += entry.value
    syncAttributes(player)
    notify(player, entry.petName .. ' delivered! +$' .. entry.value)
end

task.spawn(function()
    while true do
        task.wait(0.25)
        for player, entry in pairs(carrying) do
            local char = player.Character
            local root = char and char:FindFirstChild('HumanoidRootPart')
            if root then
                for _, zone in ipairs(CollectionService:GetTagged('DeliveryZone')) do
                    if zone:IsA('BasePart') and inZone(zone, root.Position) then
                        deliver(player, entry)
                        break
                    end
                end
            end
        end
    end
end)

-- Upgrades ------------------------------------------------------------------
buyFunction.OnServerInvoke = function(player, upgradeName)
    if type(upgradeName) ~= 'string' or not Config.Upgrades[upgradeName] then
        return false, 'Invalid upgrade'
    end
    local d = DataService.Get(player)
    if not d then
        return false, 'Still loading'
    end
    local cost = Config.CostFor(upgradeName, d[upgradeName])
    if not cost then
        return false, upgradeName .. ' is maxed out'
    end
    if d.Cash < cost then
        return false, 'Not enough cash'
    end
    d.Cash -= cost
    d[upgradeName] += 1
    syncAttributes(player)
    applyStats(player)
    return true, upgradeName .. ' upgraded!'
end

-- Players -------------------------------------------------------------------
local function onCharacter(player, char)
    clearCarry(player)
    local hum = char:WaitForChild('Humanoid')
    applyStats(player)
    hum.Died:Connect(function()
        clearCarry(player)
    end)
end

local function onPlayerAdded(player)
    DataService.Load(player)
    syncAttributes(player)
    player:SetAttribute('Carrying', '')
    player:SetAttribute('CarryValue', 0)
    player.CharacterAdded:Connect(function(char)
        onCharacter(player, char)
    end)
    if player.Character then
        onCharacter(player, player.Character)
    end
end

Players.PlayerAdded:Connect(onPlayerAdded)
for _, player in ipairs(Players:GetPlayers()) do
    task.spawn(onPlayerAdded, player)
end

Players.PlayerRemoving:Connect(function(player)
    clearCarry(player)
    DataService.Save(player)
    DataService.Release(player)
    lastSwing[player] = nil
end)

-- Autosave every minute and when the server shuts down.
task.spawn(function()
    while true do
        task.wait(60)
        for _, player in ipairs(Players:GetPlayers()) do
            DataService.Save(player)
        end
    end
end)

game:BindToClose(function()
    for _, player in ipairs(Players:GetPlayers()) do
        DataService.Save(player)
    end
end)
