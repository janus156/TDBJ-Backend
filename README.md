# TDBJ-advanced
探店笔记后端升级版,添加了消息队列的逻辑

项目介绍：基于 Spring Boot+Redis+RabbitMQ+Spring Security的店铺评价 APP，实现了用户按条件查找店铺、写笔记作点评、看热评关注博主、点 赞博客、博主主动推送笔记的一整套业务流程。

·基于 Spring Security、RBAC权限模型，实现访问控制、权限管理。

·使用 Redis 实现分布式 Session，解决集群间登录态同步问题，利用盐加密保证账号安全性

·使用 Redis 对高频访问店铺进行缓存，降低数据库压力同时提升 87% 的数据查询性能。

·使用泛型+函数式接口，封装了不同业务通用的缓存工具类，解决了缓存穿透 、缓存击穿、缓存雪崩等问题

· 使用Redission 作为分布式锁、保证线程安全，解决了超卖问题。RabbitMQ 异步扣减库存、生成订单、提高响应速度。

· 基于确认回执机制、持久化保证RabbitMQ的消息不丢失，对多次处理失败的异常消息放到死信队列，信息入库。

· 使用AOF+RDB混合持久化 防止数据丢失，lua脚本，保证操作原子性。

· 使用 Redis Set 数据结构实现用户关注、共同关注功能（交集），基于Zset实现点赞排行榜。 

· 使用常量类全局管理 Redis Key 前缀、TTL 等，保证了键空间的业务隔离，减少冲突。实现了唯一且自增ID。
