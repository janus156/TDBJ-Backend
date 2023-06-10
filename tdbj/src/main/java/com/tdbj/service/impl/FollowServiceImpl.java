package com.tdbj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tdbj.dto.Result;
import com.tdbj.dto.UserDTO;
import com.tdbj.entity.Follow;
import com.tdbj.mapper.FollowMapper;
import com.tdbj.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdbj.service.IUserService;
import com.tdbj.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private IUserService userService;

    @Autowired
    private FollowMapper followMapper;

    @Override
    public Result followOrNot(Long followUserId, Boolean isFollowed) {
        //获得用户id
        Long userId = UserHolder.getUser().getId();
        //关注：用户id 关注者

        //判断是关注还是取关
        String key = "follow:" + userId;
        if (isFollowed){
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (Boolean.TRUE.equals(isSuccess)) {
                // 添加到 Redis 中（当前用户ID 为 key，关注用户ID 为 value） sadd userId followId
                redisTemplate.opsForSet().add(key, followUserId.toString());
        }
        }else {
            boolean isSuccess = remove(new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, userId).eq(
                    Follow::getFollowUserId, followUserId));
            if (BooleanUtil.isTrue(isSuccess)) {
                // 从 Redis 中删除
                redisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result isFollowed(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = lambdaQuery().eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followUserId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result commonFollow(Long followUserId) {

        Long userId = UserHolder.getUser().getId();

        String selfKey = "follow:" + userId;
        String aimKey = "follow:" + followUserId;

        Set<String> userIdSet = redisTemplate.opsForSet().intersect(selfKey, aimKey);

        //查出来没有交集
        if (userIdSet.isEmpty() || userIdSet == null) {
            // 无交集
            return Result.ok(Collections.emptyList());
        }

        List<UserDTO> userDTOList = userService.listByIds(userIdSet)
                .stream()
                .map(user -> BeanUtil.copyProperties(user,UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOList);
    }

    /**
     * 获得粉丝数量
     * @return
     */
    @Override
    public Result getFans() {
        UserDTO user = UserHolder.getUser();
        if (user==null){
            return Result.fail("没有登录");
        }
        //得到用户id
        Long id = user.getId();
        QueryWrapper<Follow> wrapper=new QueryWrapper<>();
        wrapper.eq("follow_user_id",id);
        int count = followMapper.selectCount(wrapper);
        return Result.ok(count);

    }
}
