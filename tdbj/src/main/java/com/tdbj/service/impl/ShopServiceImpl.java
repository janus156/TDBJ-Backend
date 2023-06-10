package com.tdbj.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdbj.dto.Result;
import com.tdbj.dto.UserDTO;
import com.tdbj.entity.Shop;
import com.tdbj.mapper.ShopMapper;
import com.tdbj.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdbj.utils.CacheClient;
import com.tdbj.utils.SystemConstants;
import com.tdbj.utils.UserHolder;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tdbj.utils.RedisConstants.*;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    public StringRedisTemplate redisTemplate;

    @Autowired
    private CacheClient cacheClient;



    @Override
    public Result queryById(Long id) {

        if (id==null) return Result.fail("错误！");
        //缓存空值解决缓存穿透
//        Shop shop = cacheClient.queryWithPass(CACHE_SHOP_KEY, id, Shop.class,
//                this::getById,
//                CACHE_SHOP_TTL, TimeUnit.MINUTES);
        log.debug("访问了:"+id);
        //逻辑过期解决缓存击穿
        Shop shop=cacheClient.dealWithCacheHotspotInvalid(CACHE_SHOP_KEY,id,Shop.class,
                this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        return Result.ok(shop);
    }


    /*
    修改shop信息后，删除redis缓存.加入@Transactional保证事务性
     */
    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        if (isAdmin()){
        }
        Long id = shop.getId();
        if (id ==null){
            return Result.fail("店铺id不存在");
        }
        //修改数据库
        updateById(shop);
        //删除缓存
        Boolean flag = redisTemplate.delete(CACHE_SHOP_KEY + id);
        if (!BooleanUtil.isTrue(flag)){
            return Result.fail("更新失败");
        }
        return Result.ok();
    }

    /**
     * 返回分页的数据
     * @param typeId
     * @param current
     * @param x
     * @param y
     * @param sortBy
     * @return
     */
    @Override
    public Result queryShopByTypeId(Integer typeId, Integer current, Double x, Double y,String sortBy) {
        Page<Shop> shopList = query().eq("type_id", typeId).orderByDesc(sortBy)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(shopList.getRecords());
    }

    /**
     * 删除商店 @Transactional事务
     * @param shop
     * @return
     */
    @Transactional
    @Override
    public Result delete(Shop shop) {
        if (shop==null){
            return Result.fail("删除的店铺不存在");
        }
        delete(shop);
        redisTemplate.delete(CACHE_SHOP_KEY+shop.getId());
        return Result.ok();
    }

    @Override
    public Result queryShopByName(String name, Integer current) {
        if (StringUtil.isBlank(name)){
            return Result.fail("输入的不能为空!");
        }
        // 根据类型分页查询
        Page<Shop> shopList =query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(shopList);
    }

    @Override
    public Result saveShop(Shop shop) {
        if (shop==null) return Result.fail("店铺为空");
        save(shop);
        return Result.ok();
    }

    public Boolean isAdmin(){
        UserDTO user = UserHolder.getUser();
        String role = user.getRole();
        if (!"admin".equals(role)){
            return false;
        }
        return true;
    }


}
