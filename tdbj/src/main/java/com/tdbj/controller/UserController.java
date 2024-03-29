package com.tdbj.controller;


import cn.hutool.core.bean.BeanUtil;
import com.tdbj.dto.LoginFormDTO;
import com.tdbj.dto.Result;
import com.tdbj.dto.UserDTO;
import com.tdbj.dto.Position;
import com.tdbj.entity.User;
import com.tdbj.entity.UserInfo;
import com.tdbj.mapper.UserMapper;
import com.tdbj.service.IUserInfoService;
import com.tdbj.service.IUserService;
import com.tdbj.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;


    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }

    /**
     * 盐加密
     * @param password
     * @return
     */
    @PostMapping("setpass")
    public Result setPass(@RequestParam("password") String password){
        return userService.setPass(password);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        return userService.login(loginForm);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        return userService.logout(request);
    }

    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 根据userid返回相应的个人主页
     * @param userId
     * @return
     */
    //todo 添加地理位置
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        // 返回
        return Result.ok(info);
    }

    /**
     * 根据用户id查信息
     * @param userId
     * @return
     */

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    /**
     * 保存地理信息
     * @param position
     * @return
     */

    @PostMapping("/savepos")
    public Result savePos(@RequestBody Position position){
        double latitude =position.getLatitude();
        double longitude =position.getLongitude();

        return userService.savePos(latitude,longitude);
    }

    @GetMapping("/queryPos")
    public Result getPos(){
        return userService.getPos();
    }

}
