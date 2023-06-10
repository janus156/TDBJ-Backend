package com.tdbj.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    /**
     * 2023过年的时间戳
     */
    private static final long BEGIN_TIMESTAMP_2023 = 1675120589L;
    /**
     * 序列号的位数
     */
    private static final int BITS_COUNT = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        // 1. 时间戳
        long currentTimestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timestamp = currentTimestamp - BEGIN_TIMESTAMP_2023;

        // 2. 序列号
        String formatTime = DateTimeFormatter.ofPattern("yyyy:MM:dd").format(LocalDateTime.now());
        long serialNumber = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + formatTime);

        // 3. 拼接（时间戳向左移 32 位，通过或运算将其与序列号拼接）
        return timestamp << BITS_COUNT | serialNumber;
    }
}
