package com.tdbj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdbj.dto.LoginFormDTO;
import com.tdbj.dto.Result;
import com.tdbj.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);


    Result logout(HttpServletRequest request);

    Result savePos(double latitude, double longitude);

    Result getPos();

    Result setPass(String password);
}
