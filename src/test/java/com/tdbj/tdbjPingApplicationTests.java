package com.tdbj;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.tdbj.entity.Shop;
import com.tdbj.service.IShopService;
import com.tdbj.service.IUserService;
import com.tdbj.utils.RedisData;
import com.tdbj.utils.OnlyIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static com.tdbj.utils.RedisConstants.LOCK_SHOP_KEY;


@SpringBootTest
class tdbjPingApplicationTests {

    @Autowired
    public StringRedisTemplate redisTemplate;

    @Autowired
    public OnlyIdWorker worker;

    @Autowired
    public IShopService shopService;

    @Autowired
    public IUserService userService;


    /**
     * 店铺测试
     */
    @Test
    void lockShop(){
        Boolean flag = redisTemplate.opsForValue().
                setIfAbsent(LOCK_SHOP_KEY + 1, "1", 20, TimeUnit.SECONDS);
        if (BooleanUtil.isTrue(flag)){
            Shop shop = shopService.query().eq("id", 1).one();

            RedisData redisData=new RedisData();
            redisData.setData(shop);

            redisTemplate.opsForValue().set(LOCK_SHOP_KEY + 1, JSONUtil.toJsonStr(redisData));
        }
    }

    /**
     * 订单测试
     */
//    @Test
//    void lockOrder(){
//        Voucher voucher=new Voucher();
//        voucher.setId(1L);
//        Boolean flag = redisTemplate.opsForValue().setIfAbsent("order:" + voucher.getId()
//                , "", 20, TimeUnit.SECONDS);
//        if (BooleanUtil.isTrue(flag)){
//
//        }
//
//    }

    /**
     * 锁测试
     */
    @Test
    void unlock(){
        redisTemplate.delete(LOCK_SHOP_KEY + 1);
    }

    @Test
    void order(){
        long order = worker.nextId("order");
        System.out.println(order);
    }
}
