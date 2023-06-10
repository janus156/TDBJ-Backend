package com.tdbj.controller;


import com.tdbj.dto.Result;
import com.tdbj.entity.Shop;
import com.tdbj.service.IShopService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping("save")
    @PreAuthorize("hasAuthority('write')")
    public Result saveShop(@RequestBody Shop shop) {
        return shopService.saveShop(shop);
    }

    /**
     * 删除了店铺
     * @param shop
     * @return
     */

    @DeleteMapping("delete")
    @PreAuthorize("hasAuthority('write')")
    public Result deleteShop(@RequestBody Shop shop){
        return shopService.delete(shop);

    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping("update")
    @PreAuthorize("hasAuthority('write')")
    public Result updateShop(@RequestBody Shop shop) {
        // 写入数据库
        return shopService.updateShop(shop);
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */

    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y,
            @RequestParam(value = "sortBy", required = false) String sortBy) {
        return shopService.queryShopByTypeId(typeId, current, x, y,sortBy);
    }

    /**
     *
     *
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        return shopService.queryShopByName(name,current);
    }
}
