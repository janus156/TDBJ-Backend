package com.tdbj.service;

import com.tdbj.dto.Result;
import com.tdbj.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IShopTypeService extends IService<ShopType> {

    Result usingListToQueryByCache();
}
