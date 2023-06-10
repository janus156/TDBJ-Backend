package com.tdbj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdbj.dto.*;
import com.tdbj.dto.Result;
import com.tdbj.entity.User;
import com.tdbj.mapper.UserMapper;
import com.tdbj.service.IUserService;
import com.tdbj.utils.*;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.tdbj.utils.RedisConstants.*;
import static com.tdbj.utils.SystemConstants.USER_NICK_NAME_PREFIX;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Autowired
    private UserMapper userMapper;


    /*
    发送手机验证码
     */
    @Override
    public Result sendCode(String phone) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式不对");
        };
        String code = RandomUtil.randomString(6);

        // 3. 将验证码保存到 Redis

        redisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);


        return Result.ok();
    }


    /**
     * 用户登录,如果没有该用户就创建一个
     * @param loginForm
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm) {
        //验证手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式不对");
        }
        //redis验证码
        String cacheCode = redisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        //表单提交的验证码
        String loginFormCode = loginForm.getCode();
        if (!StrUtil.equals(cacheCode, loginFormCode)) {
            return Result.fail("验证码错误");
        }

        //查用户
        User user = query().eq("phone", phone).one();

        //如果没查到，就创建新的用户
        if (user==null){
            User newUser = createNewUserWithPhone(phone);
            user=newUser;
        }

        //查出权限集合
        List<String> list = userMapper.getPermByUserId(user.getId());
        LoginUser loginUser =new LoginUser(user,list);
        String userId = loginUser.getUser().getId().toString();
        //jwt加密
        String jwt = JwtUtil.createJWT(userId);
        //使用userid生成token
        String jsonStr = JSONUtil.toJsonStr(loginUser);
        redisTemplate.opsForValue().set(LOGIN_USER_KEY+userId,
                jsonStr,LOGIN_USER_TTL,TimeUnit.MINUTES);
        //把token响应给前端
        return  Result.ok(jwt);
    }


    /**
     * 登出
     * @param request
     * @return
     */

    @Override
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");

        if (StringUtil.isBlank(token)){
            return Result.fail("没有登录,无法登出");
        }
        //如果有用户再登出
        String key=LOGIN_USER_KEY+token;
        redisTemplate.delete(key);
        UserHolder.removeUser();

        return Result.ok();
    }

    //todo 根据经度纬度定位国家、省份
    //存储pos
    @Override
    public Result savePos(double longitude, double latitude) {
        UserDTO user = UserHolder.getUser();
        if (user==null){
            return Result.fail("用户未登录");
        }
        //用户已经登录
        User dbUser = getById(user.getId());
        dbUser.setLatitude(latitude);
        dbUser.setLongitude(longitude);

        System.out.println(dbUser);
        updateById(dbUser);
        return Result.ok();
    }

    @Override
    public Result getPos() {
        UserDTO user = UserHolder.getUser();
        if (user==null){
            return Result.fail("用户未登录");
        }

        //如果已经登录
        //用户已经登录
        User dbUser = getById(user.getId());
        if (dbUser.getLatitude()==null || dbUser.getLongitude()==null){
            return Result.fail("没有获取地理位置");
        }
        Position position = new Position();
        position.setLatitude(dbUser.getLatitude());
        position.setLongitude(dbUser.getLongitude());
        return Result.ok(position);
    }

    /**
     * 盐加密
     * @param password
     * @return
     */

    @Override
    public Result setPass(String password) {
        Long userId = UserHolder.getUser().getId();
        User user = query().eq("id", userId).one();
        //加密
        String encodePass = DigestUtils.md5DigestAsHex((password + SystemConstants.SALT).getBytes());
        user.setPassword(encodePass);
        //存储信息
        save(user);
        return Result.ok();
    }


    /**
     * 根据手机号创建新用户,存到数据库
     * @param phone
     * @return
     */
    private User createNewUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
