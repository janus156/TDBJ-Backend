package com.tdbj.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";

    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    /**
     * 用setnx来实现加锁
     * @param timeoutsec
     * @return
     */
    @Override
    public boolean tryLock(long timeoutsec) {

        String threadIdentifier = ID_PREFIX + Thread.currentThread().getId();
        Boolean isSucceeded = stringRedisTemplate.opsForValue().
                setIfAbsent(KEY_PREFIX + name, threadIdentifier, timeoutsec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isSucceeded);
    }

    @Override
    public void unLock() {
        String threadIdentifier = ID_PREFIX + Thread.currentThread().getId();
        String threadIdentifierFromRedis = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 比较 锁中的线程标识 与 当前的线程标识 是否一致
        if (StrUtil.equals(threadIdentifier, threadIdentifierFromRedis)) {
            // 释放锁标识
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
