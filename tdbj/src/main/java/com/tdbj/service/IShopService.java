package com.tdbj.service;

import com.tdbj.dto.Result;
import com.tdbj.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result updateShop(Shop shop);

    Result queryShopByTypeId(Integer typeId, Integer current, Double x, Double y,String sortBy);

    Result delete(Shop shop);

    Result queryShopByName(String name,Integer current);

    Result saveShop(Shop shop);
}
