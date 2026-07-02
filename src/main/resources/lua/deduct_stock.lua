-- 号源库存原子扣减（Redis Lua）
-- KEYS[1] = regist:stock:{emp}:{date}:{noon}
-- 返回： 1 = 扣减成功；0 = 已约满；-1 = 库存 key 未初始化（需 lazy seed 后重试）
local key = KEYS[1]
if redis.call('EXISTS', key) == 0 then
  return -1
end
local v = redis.call('GET', key)
if v == false then
  return -1
end
local n = tonumber(v)
if n == nil then
  return -1
end
if n > 0 then
  redis.call('DECR', key)
  return 1
end
return 0
