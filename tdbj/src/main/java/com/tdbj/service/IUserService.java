package com.tdbj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdbj.dto.LoginFormDTO;
import com.tdbj.dto.Result;
import com.tdbj.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {

    Result sendCode(String phone);

    Result login(LoginFormDTO loginForm);

    Result logout(HttpServletRequest request);

    Result savePos(double latitude, double longitude);

    Result getPos();

    Result setPass(String password);
}
