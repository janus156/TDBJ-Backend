package com.tdbj.service.impl;

import com.tdbj.dto.Result;
import com.tdbj.entity.SeckillVoucher;
import com.tdbj.entity.VoucherOrder;
import com.tdbj.mapper.VoucherOrderMapper;
import com.tdbj.service.ISeckillVoucherService;
import com.tdbj.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdbj.utils.RedisConstants;
import com.tdbj.utils.OnlyIdWorker;
import com.tdbj.utils.SimpleRedisLock;
import com.tdbj.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private OnlyIdWorker onlyIdWorker;


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override

    public Result seckillVoucher(Long voucherId) {

        //查优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        // 2. 判断秒杀是否开始或结束（未开始或已结束，返回异常结果）
        if (LocalDateTime.now().isBefore(seckillVoucher.getBeginTime())) {
            return Result.fail("秒杀尚未开始..");
        }
        if (LocalDateTime.now().isAfter(seckillVoucher.getEndTime())) {
            return Result.fail("秒杀已经结束..");
        }

        // 3. 判断库存是否充足（不充足返回异常结果）
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足..");
        }


        //获得setnx锁
        SimpleRedisLock lock = new SimpleRedisLock(RedisConstants.LOCK_VOUCHER_KEY + voucherId, redisTemplate);

        boolean isLock = lock.tryLock(1);
        

        //如果获取锁失败
        if (!isLock){
            return Result.fail("创建订单失败");
        }
        try{
            return createVoucherOrder(voucherId);
        }finally {
            //释放锁
            lock.unLock();
        }

    }

    /*
    根据优惠券id来创建订单
     */
    @Transactional
    public  Result createVoucherOrder(Long voucherId) {
        // 4. 一人一单（根据 优惠券id 和 用户id 查询订单；存在，则直接返回）
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if (count > 0) {
            return Result.fail("不可重复下单");
        }

        //5.判断库存。stock必须大于0才可修改
        boolean isAccomplished = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();

        if (!isAccomplished) {
            return Result.fail("购买失败");
        }

        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = onlyIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        boolean isSaved = save(voucherOrder);
        if (!isSaved) {
            return Result.fail("下单失败..");
        }

        // 6. 返回 订单 id
        return Result.ok(orderId);
    }

}
