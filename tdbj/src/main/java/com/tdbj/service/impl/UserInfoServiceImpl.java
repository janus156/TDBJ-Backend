package com.tdbj.service.impl;

import com.tdbj.entity.UserInfo;
import com.tdbj.mapper.UserInfoMapper;
import com.tdbj.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
