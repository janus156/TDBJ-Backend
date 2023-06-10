package com.tdbj.service.impl;

import cn.hutool.json.JSONUtil;
import com.tdbj.dto.OrderTrans;
import com.tdbj.dto.Result;
import com.tdbj.entity.SeckillVoucher;
import com.tdbj.entity.VoucherOrder;
import com.tdbj.mapper.VoucherOrderMapper;
import com.tdbj.service.ISeckillVoucherService;
import com.tdbj.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdbj.utils.RedisConstants;
import com.tdbj.utils.RedisIdWorker;
import com.tdbj.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

//todo 修改到gitlab
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override

    public Result seckillVoucher(Long voucherId) {
        //ThreadLocal得到用户id
        Long userId = UserHolder.getUser().getId();


        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        // 判断秒杀是否开始或结束（未开始或已结束，返回异常结果）
        if (LocalDateTime.now().isBefore(seckillVoucher.getBeginTime())) {
            return Result.fail("秒杀尚未开始..");
        }
        if (LocalDateTime.now().isAfter(seckillVoucher.getEndTime())) {
            return Result.fail("秒杀已经结束..");
        }
        // 判断库存是否充足（不充足返回异常结果）
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足..");
        }

        RLock rLock = redissonClient.getLock(RedisConstants.LOCK_VOUCHER_KEY + voucherId);



        boolean isLock = false;
        try {
            isLock = rLock.tryLock(1, 3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!isLock){
            return Result.fail("创建订单失败");
        }

        try {
            // 一人一单（根据 优惠券id 和 用户id 查询订单；存在，则直接返回）
            Integer count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
            if (count > 0) {
                return Result.fail("不可重复下单");
            }

            //库存判断
            boolean isUpdate = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId).gt("stock", 0)
                    .update();
            if (!isUpdate){
                return Result.fail("没有库存");
            }

            //  全局唯一的消息ID，需要封装到CorrelationData中
            CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
            //  添加callback
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

            OrderTrans orderTrans=new OrderTrans();
            orderTrans.setVoucherId(voucherId);
            orderTrans.setUserId(userId);
            String orderStr = JSONUtil.toJsonStr(orderTrans);
            // 4.发送消息
            rabbitTemplate.convertAndSend("tdbj.store", "store", voucherId, correlationData);
            rabbitTemplate.convertAndSend("tdbj.order", "order", orderStr, correlationData);
        } finally {
            rLock.unlock();
        }
        return Result.ok();
    }



    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "tdbj.order.queue", durable = "true"),
            exchange = @Exchange(name = "tdbj.order"),
            key = "order"
    ))
    public void listenOrderQueue(String orderStr) {
        OrderTrans orderTrans = JSONUtil.toBean(orderStr, OrderTrans.class);

        Long userId=orderTrans.getUserId();
        Long voucherId=orderTrans.getVoucherId();
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        log.info("tdbj.order.queue收到消息:"+voucherId);
    }



}


