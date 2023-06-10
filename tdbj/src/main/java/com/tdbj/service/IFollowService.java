package com.tdbj.service;

import com.tdbj.dto.Result;
import com.tdbj.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IFollowService extends IService<Follow> {

    Result followOrNot(Long followUserId, Boolean isFollowed);

    Result isFollowed(Long followUserId);

    Result commonFollow(Long followUserId);

    Result getFans();
}
