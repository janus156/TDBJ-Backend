
package com.tdbj;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.tdbj.dto.LoginFormDTO;
import com.tdbj.dto.LoginUser;
import com.tdbj.dto.Result;
import com.tdbj.dto.UserDTO;
import com.tdbj.entity.Blog;
import com.tdbj.entity.Shop;
import com.tdbj.entity.User;
import com.tdbj.entity.Voucher;
import com.tdbj.mapper.ShopMapper;
import com.tdbj.mapper.UserMapper;
import com.tdbj.service.IBlogService;
import com.tdbj.service.IShopService;
import com.tdbj.service.IUserService;
import com.tdbj.service.IVoucherOrderService;
import com.tdbj.utils.*;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.tdbj.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.tdbj.utils.RedisConstants.LOCK_SHOP_KEY;

//todo 提交到gitlab
@SpringBootTest
@Slf4j
class tdbjPingApplicationTests {

    @Autowired
    public StringRedisTemplate redisTemplate;

    @Autowired
    public RedisIdWorker worker;

    @Autowired
    public IShopService shopService;

    @Autowired
    public IUserService userService;

    @Autowired
    private IBlogService blogService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private IVoucherOrderService voucherOrderService;


    /**
     * 需要哪些参数？
     * 1.查询id
     * 2.查询的类型
     * 3.返回值
     * 4.前缀
     * 5.过期时间
     * 6.function
     */
    @Test
    void getUser(){
        LoginFormDTO loginFormDTO=new LoginFormDTO();
        loginFormDTO.setCode("123456");
        loginFormDTO.setPhone("13388191007");

        Result login = userService.login(loginFormDTO);
    }

    @Test
    void sendMessage(){
        redisTemplate.opsForValue().set("login:code:13388191007","123456");
    }

    @Test
    void saveBlog(){
        UserDTO userDTO=new UserDTO();
        userDTO.setId(13688668893L);
        UserHolder.saveUser(userDTO);

        Blog blog=new Blog();
        blog.setName("不拉");
        blog.setContent("没啥内容");
        blog.setTitle("无标题");
        blog.setImages("ddd");
        blog.setShopId(4L);
        blogService.saveBlog(blog);
    }

    @Test
    void testHotSpot(){
        CacheClient cacheClient=new CacheClient(redisTemplate);
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpiration(CACHE_SHOP_KEY+1L,shop,1L,TimeUnit.SECONDS);
    }

    @Test
    void getPermTest(){
        List<String> permList = userMapper.getPermByUserId(1015L);
        System.out.println(permList);
    }

    @Test
    void getUserByName(){
        User user = userMapper.getUserByName("可爱多");
        System.out.println(user);
    }

    @Test
    void sendCode(){
        Result result = userService.sendCode("13388191007");
    }

    @Test
    void Login(){
        LoginFormDTO loginFormDTO=new LoginFormDTO();
        loginFormDTO.setCode("glr0g1");
        loginFormDTO.setPhone("13388191007");
        Result result1 = userService.login(loginFormDTO);
    }


    @Test
    void getPerm(){
        String str = redisTemplate.opsForValue().get("login:1015" );
        LoginUser loginUser = JSONUtil.toBean(str, LoginUser.class);
        System.out.println(loginUser);
    }

    /**
     * redission获取锁的尝试
     * @throws InterruptedException
     */
    @Test
    void testRedission() throws InterruptedException {
        RLock rLock = redissonClient.getLock("Shop:Lock");

        //重试时间、
        boolean isLock = rLock.tryLock(1, 10, TimeUnit.SECONDS);

        if (isLock){
            try {
                System.out.println(Thread.currentThread().getName()+"得到了锁");
            }finally {
                rLock.unlock();
            }
        }
    }

    @Test
    void getKeys(){
        Set<String> stringSet = redisTemplate.keys("*");
        System.out.println(stringSet);
    }

    @Test
    public void testSendMessage2SimpleQueue() throws InterruptedException {
        // 1.消息体
        String message = "hello, store queue!";
        // 2.全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        // 3.添加callback
        correlationData.getFuture().addCallback(
                result -> {
                    if(result.isAck()){
                        // 3.1.ack，消息成功
                        log.debug("消息发送成功, ID:{}", correlationData.getId());
                    }else{
                        // 3.2.nack，消息失败
                        log.error("消息发送失败, ID:{}, 原因{}",correlationData.getId(), result.getReason());
                    }
                },
                ex -> {
                    log.error("消息发送异常, ID:{}, 原因{}",correlationData.getId(),ex.getMessage());
                }
        );
        // 4.发送消息
        rabbitTemplate.convertAndSend("tdbj.store", "store", message, correlationData);

    }



    @Test
    void doOrderService(){
        voucherOrderService.seckillVoucher(1L);
    }

}

