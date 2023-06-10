package com.tdbj.utils;

public class RedisConstants {
    /**
     * 登录前缀、以及过期时间
     */
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 2L;

    /**
     * 店铺前缀
     */
    public static final String CACHE_SHOP_TYPE_KEY="cache:shoptype";
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    /**
     * 锁
     */
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final String LOCK_VOUCHER_KEY = "lock:voucher:";

    //点赞的粉丝
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    //feed流前缀
    public static final String FEED_KEY = "feed:";

}
