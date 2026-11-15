-- ServerScriptService/DataService (ModuleScript)
local DataStoreService = game:GetService('DataStoreService')

local store = DataStoreService:GetDataStore('YolkRushV1')

local Defaults = { Cash = 0, Pickaxe = 1, Speed = 0, Jump = 0 }
local cache = {}

local DataService = {}

local function newData()
    local t = {}
    for k, v in pairs(Defaults) do
        t[k] = v
    end
    return t
end

function DataService.Load(player)
    local data = newData()
    local ok, result = pcall(function()
        return store:GetAsync('u_' .. player.UserId)
    end)
    if ok then
        if type(result) == 'table' then
            for k in pairs(Defaults) do
                if type(result[k]) == 'number' then
                    data[k] = result[k]
                end
            end
        end
    else
        -- Load failed: play with defaults but never overwrite the saved data.
        warn('DataStore load failed for ' .. player.Name .. ': ' .. tostring(result))
        data._unsafe = true
    end
    cache[player.UserId] = data
    return data
end

function DataService.Get(player)
    return cache[player.UserId]
end

function DataService.Save(player)
    local data = cache[player.UserId]
    if not data or data._unsafe then
        return
    end
    local payload = {}
    for k in pairs(Defaults) do
        payload[k] = data[k]
    end
    local ok, err = pcall(function()
        store:SetAsync('u_' .. player.UserId, payload)
    end)
    if not ok then
        warn('DataStore save failed for ' .. player.Name .. ': ' .. tostring(err))
    end
end

function DataService.Release(player)
    cache[player.UserId] = nil
end

return DataService
