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
-- 获取库存量
local n = tonumber(v)
if n == nil then
  return -1
end
-- 如果库存大于0就执行扣减操作.
-- 把 判断库存是否充足和扣减操作放在lua脚本里面执行的目的是: 为了防止两个线程同时判断库存>0(此时库存为1),并扣减, 导致超卖
-- 用lua脚本配合上redis的单线程执行特性, 可以防止超卖.
if n > 0 then
  redis.call('DECR', key)
  return 1
end
-- 库存已满返回0
return 0
