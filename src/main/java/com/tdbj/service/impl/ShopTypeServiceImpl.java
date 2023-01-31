package com.tdbj.service.impl;

import cn.hutool.json.JSONUtil;
import com.tdbj.dto.Result;
import com.tdbj.entity.ShopType;
import com.tdbj.mapper.ShopTypeMapper;
import com.tdbj.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static com.tdbj.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Result usingListToQueryByCache() {
        //查询redis中是否含有数据
        List<String>  shopTypeJsonList = redisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);

        //2. Redis 中存在则直接返回
        if (!shopTypeJsonList.isEmpty()){
            ArrayList<ShopType> shopTypeList=new ArrayList<>();
            for (String str:shopTypeJsonList){
                ShopType shopType = JSONUtil.toBean(str, ShopType.class);
                shopTypeList.add(shopType);
            }
            return Result.ok(shopTypeList);
        }

        // 3.Redis 中不存在则从数据库中查询；数据库中不存在则报错。
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        if (shopTypeList==null ||shopTypeList.isEmpty()){
            return Result.fail("该店铺类型不存在！") ;
        }

        // 4. 数据库中存在，将其保存到 Redis 中并返回。

        for (ShopType shopType : shopTypeList){
            String jsonStr = JSONUtil.toJsonStr(shopType);
            shopTypeJsonList.add(jsonStr);
        }
        redisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY,shopTypeJsonList);

        return Result.ok(shopTypeList);
    }
}
