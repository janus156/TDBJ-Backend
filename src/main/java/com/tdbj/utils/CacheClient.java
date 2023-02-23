package com.tdbj.utils;


import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.tdbj.utils.RedisConstants.*;

@Component
@Slf4j
public class CacheClient {
    private final StringRedisTemplate redisTemplate;

    public CacheClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /*
    设置key,再设置随机值，避免因大规模key失效，进而缓存雪崩
     */
    public void set(String key, Object value, Long time, TimeUnit timeUnit) {
        long random = RandomUtil.randomLong(10);
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),time+random,timeUnit);
    }

    /**
     * 设置逻辑过期，放到redis中
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void setWithLogicalExpiration(String key, Object value, Long time, TimeUnit timeUnit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /*
    缓存空值来解决缓存穿透
     */
    public <R,ID> R queryWithPass(String prefix, ID id, Class<R> type, Function<ID,R> dbFallBack,
                                  Long time, TimeUnit timeUnit) {
        String key = prefix + id;
        //查redis是否有,有的话将其转换为shop并返回
        String json = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }

        //命中空缓存
        if ("".equals(json)){
            return null;
        }

        //查询数据库
        R r=dbFallBack.apply(id);
        //数据库查不到,防止缓存穿透，缓存空值
        if (r==null){
            redisTemplate.opsForValue().set(key,"", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        //否则存到redis中
        //速度从0.5 s->40ms
        //把数据库的信息缓存
        this.set(key,r,time,timeUnit);

        return r;
    }


    //用逻辑过期缓存击穿问题,即热点问题
    public <R, ID> R dealWithCacheHotspotInvalid(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit timeUnit) {
        String key = keyPrefix + id;
        // 1. 从 Redis 中查询店铺缓存；
        String json = redisTemplate.opsForValue().get(key);
        // 2. 未命中
        if (StrUtil.isBlank(json)) {
            return null;
        }
        // 3. 命中,利用hutool工具将json序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 4. 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }

        String lockKey = LOCK_SHOP_KEY + id;
        SimpleRedisLock lock = new SimpleRedisLock(lockKey, redisTemplate);
        boolean isLocked = lock.tryLock(20);
        // 5.1 获取到互斥锁
        json = redisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        if (isLocked) {
                try {
                    //写到数据
                    R apply = dbFallback.apply(id);
                    this.setWithLogicalExpiration(key, apply, time, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放互斥锁
                    lock.unLock();
                }
        }
        // 5.2 返回店铺信息
        return r;
    }


}
