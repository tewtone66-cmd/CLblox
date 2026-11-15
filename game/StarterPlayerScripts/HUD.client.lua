-- StarterPlayer/StarterPlayerScripts/HUD (LocalScript)
-- Builds the whole interface from code. Reads state from player attributes set by the server.
local Players = game:GetService('Players')
local ReplicatedStorage = game:GetService('ReplicatedStorage')
local UserInputService = game:GetService('UserInputService')

local player = Players.LocalPlayer
local Config = require(ReplicatedStorage:WaitForChild('Config'))
local remotes = ReplicatedStorage:WaitForChild('Remotes')
local swing = remotes:WaitForChild('Swing')
local buy = remotes:WaitForChild('Buy')
local notify = remotes:WaitForChild('Notify')

local YOLK = Color3.fromRGB(255, 214, 107)
local NIGHT = Color3.fromRGB(27, 21, 48)
local MIST = Color3.fromRGB(237, 233, 255)
local MINT = Color3.fromRGB(94, 230, 195)

local gui = Instance.new('ScreenGui')
gui.Name = 'YolkHUD'
gui.ResetOnSpawn = false
gui.Parent = player:WaitForChild('PlayerGui')

local function round(inst, radius)
    local c = Instance.new('UICorner')
    c.CornerRadius = UDim.new(0, radius)
    c.Parent = inst
end

local function label(props)
    local l = Instance.new('TextLabel')
    l.BackgroundColor3 = NIGHT
    l.BackgroundTransparency = 0.1
    l.TextColor3 = MIST
    l.Font = Enum.Font.GothamBold
    l.TextSize = 18
    for k, v in pairs(props) do
        l[k] = v
    end
    l.Parent = gui
    round(l, 14)
    return l
end

local cashLabel = label({
    Size = UDim2.fromOffset(200, 48),
    Position = UDim2.new(0.5, -100, 0, 12),
    TextColor3 = YOLK,
    Font = Enum.Font.GothamBlack,
    TextSize = 26,
    Text = '$0',
})

local carryLabel = label({
    Size = UDim2.fromOffset(360, 36),
    Position = UDim2.new(0.5, -180, 0, 66),
    TextColor3 = MINT,
    Text = '',
    Visible = false,
})

local messageLabel = label({
    Size = UDim2.fromOffset(440, 40),
    Position = UDim2.new(0.5, -220, 0, 108),
    Text = '',
    Visible = false,
})

-- Upgrade buttons -------------------------------------------------------------
local order = { 'Pickaxe', 'Speed', 'Jump' }
local buttons = {}
local busy = false

local messageToken = 0
local function showMessage(text)
    messageToken += 1
    local mine = messageToken
    messageLabel.Text = text
    messageLabel.Visible = true
    task.delay(3, function()
        if mine == messageToken then
            messageLabel.Visible = false
        end
    end)
end

for i, name in ipairs(order) do
    local b = Instance.new('TextButton')
    b.Size = UDim2.fromOffset(230, 48)
    b.Position = UDim2.new(0, 12, 0.5, -80 + (i - 1) * 56)
    b.BackgroundColor3 = YOLK
    b.TextColor3 = NIGHT
    b.Font = Enum.Font.GothamBlack
    b.TextSize = 16
    b.AutoButtonColor = true
    b.Parent = gui
    round(b, 14)
    b.Activated:Connect(function()
        if busy then
            return
        end
        busy = true
        local ok, msg = buy:InvokeServer(name)
        showMessage(msg or (ok and 'Done' or 'Failed'))
        busy = false
    end)
    buttons[name] = b
end

local function refresh()
    cashLabel.Text = '$' .. tostring(player:GetAttribute('Cash') or 0)
    for name, b in pairs(buttons) do
        local level = player:GetAttribute(name) or 0
        local cost = Config.CostFor(name, level)
        local u = Config.Upgrades[name]
        if cost then
            b.Text = string.format('%s Lv %d  |  $%d', u.Label, level, cost)
        else
            b.Text = string.format('%s Lv %d  |  MAX', u.Label, level)
        end
    end
    local carried = player:GetAttribute('Carrying')
    if carried and carried ~= '' then
        carryLabel.Text = string.format('%s ($%d) - run to the start line!', carried, player:GetAttribute('CarryValue') or 0)
        carryLabel.Visible = true
    else
        carryLabel.Visible = false
    end
end

player.AttributeChanged:Connect(refresh)
refresh()

notify.OnClientEvent:Connect(showMessage)

-- Swinging --------------------------------------------------------------------
local swingButton = Instance.new('TextButton')
swingButton.Size = UDim2.fromOffset(110, 110)
swingButton.Position = UDim2.new(1, -130, 1, -150)
swingButton.BackgroundColor3 = MINT
swingButton.TextColor3 = NIGHT
swingButton.Font = Enum.Font.GothamBlack
swingButton.TextSize = 20
swingButton.Text = 'SWING'
swingButton.Parent = gui
round(swingButton, 55)
swingButton.Activated:Connect(function()
    swing:FireServer()
end)

UserInputService.InputBegan:Connect(function(input, processed)
    if processed then
        return
    end
    if input.UserInputType == Enum.UserInputType.MouseButton1 or input.KeyCode == Enum.KeyCode.E then
        swing:FireServer()
    end
end)
