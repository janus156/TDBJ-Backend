package com.tdbj.utils;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript();
//        UNLOCK_SCRIPT.setLocation((Resource)
//                new ClassPathResource("classpath:unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

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



    /**
     * 释放锁
     */
    @Override
    public void unLock() {
        // 调用 Lua 脚本
//        stringRedisTemplate.execute(
//                UNLOCK_SCRIPT,  // SCRIPT
//                Collections.singletonList(KEY_PREFIX + name),   // KEY[1]
//                ID_PREFIX + Thread.currentThread().getId()    // ARGV[1]
//        );
        String threadIdentifier = ID_PREFIX + Thread.currentThread().getId();

        String threadId= stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if (threadIdentifier.equals(threadId)){
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }

}
