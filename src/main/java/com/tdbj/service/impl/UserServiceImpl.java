package com.tdbj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdbj.dto.LoginFormDTO;
import com.tdbj.dto.Position;
import com.tdbj.dto.Result;
import com.tdbj.dto.UserDTO;
import com.tdbj.entity.User;
import com.tdbj.mapper.UserMapper;
import com.tdbj.service.IUserService;
import com.tdbj.utils.RegexUtils;
import com.tdbj.utils.SystemConstants;
import com.tdbj.utils.UserHolder;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tdbj.utils.RedisConstants.*;
import static com.tdbj.utils.SystemConstants.USER_NICK_NAME_PREFIX;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;


    /*
    发送手机验证码
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式不对");
        };

        String code = RandomUtil.randomString(6);

        // 3. 将验证码保存到 Redis

        redisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        log.debug("code: {}",code);

        return Result.ok();
    }

    //todo 密码登录


    //todo 利用阿里云短信平台

    /**
     * 用户登录,如果没有该用户就创建一个
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
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

        // 保存用户信息到 Redis 中（随机生成 Token，作为登录令牌；将 User 对象转为 Hash 存储）
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        //将值全部转为string类型
                        .setFieldValueEditor((filedName, fieldValue) -> fieldValue.toString())
        );


        String tokenKey=LOGIN_USER_KEY + token;
        //存储user到redis
        redisTemplate.opsForHash().putAll(tokenKey, userMap);
        redisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
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
        String encodePass = DigestUtils.md5DigestAsHex((password + SystemConstants.salt).getBytes());
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
