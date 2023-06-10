package com.tdbj.service;

import com.tdbj.dto.Result;
import com.tdbj.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

}
