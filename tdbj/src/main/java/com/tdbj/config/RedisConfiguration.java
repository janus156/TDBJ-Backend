package com.tdbj.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfiguration {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // Redis 地址和密码（useSingleServer 单节点，useClusterServers 集群）
        config.useSingleServer().setAddress("redis://114.132.226.23:6379").setPassword("user1");
        // 创建客户端
        return Redisson.create(config);
    }
}
