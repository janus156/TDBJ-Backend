package com.tdbj.mapper;

import com.tdbj.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


public interface UserMapper extends BaseMapper<User> {
    List<String> getPermByUserId(Long userId);

    User getUserByName(String nickName);
}
