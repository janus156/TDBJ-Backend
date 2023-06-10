package com.tdbj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdbj.dto.Result;
import com.tdbj.dto.ScrollResult;
import com.tdbj.dto.UserDTO;
import com.tdbj.entity.Blog;
import com.tdbj.entity.Follow;
import com.tdbj.entity.User;
import com.tdbj.mapper.BlogMapper;
import com.tdbj.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdbj.service.IFollowService;
import com.tdbj.service.IUserService;
import com.tdbj.utils.SystemConstants;
import com.tdbj.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.tdbj.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.tdbj.utils.RedisConstants.FEED_KEY;


@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Autowired
    private IFollowService followService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 首页展示的blog
     * @param current
     * @return
     */
    @Override
    public Result queryHotBlog(Integer current) {
        Page<Blog> page = query().orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE) );

        List<Blog> records = page.getRecords();

        records.forEach(blog -> {
            this.isBlogLiked(blog);
            this.queryBlogById(blog.getId());
        });
        return Result.ok(records);
    }

    //根据id查blog
    @Override
    public Result queryBlogById(Long id) {
        //根据id查到blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        //塞入用户信息
        queryBlogWithUserInfo(blog);
        //再塞入用户是否点赞的信息
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 判断当前的用户可不可以点赞
     * @param blog
     */
    private void isBlogLiked(Blog blog) {
        //如果没有登录，直接返回，不用判断有没有点赞
        if (UserHolder.getUser()==null){
            return;
        }
        Long userId = UserHolder.getUser().getId();
        //获取用户是否点赞
        String key=BLOG_LIKED_KEY+blog.getId();
        //到redis判断
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }

    /**
     * 用户点赞或者取消点赞该blog
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();

        String key=BLOG_LIKED_KEY+id;
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());

        //如果不是成员
        if (score == null){
            Boolean isSucceed = update().setSql("liked = liked + 1").eq("id", id).update();
            if (BooleanUtil.isTrue(isSucceed)) {
                //set key取blog， value取用户id
                redisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else {  //当前用户已经点赞,把redis删除
            Boolean isSucceed = update().setSql("liked = liked - 1").eq("id", id).update();
            if (BooleanUtil.isTrue(isSucceed)) {
                redisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();

    }
    /*
    * 查看点赞排行榜 */
    @Override
    public Result queryBlogLikes(Long id) {
        String key=BLOG_LIKED_KEY+id;
        //range方法查到用户id
        Set<String> top5 = redisTemplate.opsForZSet().range(key, 0, 4);

        if (top5==null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        //解析用户id
        List<Long> userIds = top5.stream().map(Long::valueOf).collect(Collectors.toList());

        //查用户
        List<UserDTO> userDTOS = userService.listByIds(userIds)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }

    /**
     * 保存blog并推送给粉丝邮件箱
     * @param blog
     * @return
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 1. 获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2. 保存探店博文
        blog.setUserId(userId);

        boolean isSuccess = save(blog);
        if (BooleanUtil.isFalse(isSuccess)) {
            return Result.fail("发布失败～");
        }
        // 3. 查询笔记作者的所有粉丝（select * from tb_follow where follow_user_id = ?）
        List<Follow> followList = followService.lambdaQuery().eq(Follow::getFollowUserId, userId).list();
        if (followList == null || followList.isEmpty() ) {
            return Result.ok(blog.getId());
        }
        // 4. 推送笔记给所有粉丝
        for (Follow follow:followList){
            Long userid = follow.getUserId();
            // 推送到粉丝收件箱
            String key = FEED_KEY + userid;
            redisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }

        // 5. 返回id
        return Result.ok(blog.getId());
    }

    /**
     * 粉丝查看自己的邮件箱,利用reverseRangeByScore
     * 展现最新的blog
     * @param max
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();

        // 2. 查询收件箱 ZREVRANGEBYSCORE key max min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> tupleSet = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if (tupleSet == null || tupleSet.isEmpty() ) {
            return Result.ok();
        }

        // 3. 将redis返回的tupleSet中的blogid，存放到blogIdList
        List<Long> blogIdList = new ArrayList<>(tupleSet.size());
        long minTime = 0;
        int nextOffset = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tupleSet) {
            blogIdList.add(Long.valueOf(tuple.getValue()));
            // 时间戳（最后一个元素即为最小时间戳）
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                nextOffset ++;
            } else {
                minTime = time;
                nextOffset = 1;
            }
        }

        // 4. 根据 ID 查询 Blog
        String blogIdStr = StrUtil.join(", ", blogIdList);
        List<Blog> blogList = lambdaQuery().in(Blog::getId, blogIdList).last("ORDER BY FIELD(id, " + blogIdStr + ")").list();
        for (Blog blog : blogList) {
            // 完善 Blog 数据：查询并且设置与 Blog 有关的用户信息，以及 Blog 是否被该用户点赞
            queryBlogWithUserInfo(blog);
            isBlogLiked(blog);
        }

        // 5. 封装并返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogList);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(nextOffset);
        return Result.ok(scrollResult);
    }

    private void queryBlogWithUserInfo(Blog blog) {
        User user = userService.getById(blog.getUserId());
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
    }
}
