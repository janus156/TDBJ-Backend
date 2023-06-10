package com.tdbj.controller;
import com.tdbj.dto.Result;
import com.tdbj.service.IFollowService;
import com.tdbj.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    /**
     * 关注或取关
     */
    @PutMapping("/{id}/{isFollowed}")
    public Result followOrNot(@PathVariable("id") Long followUserId, @PathVariable("isFollowed") Boolean isFollowed) {
        return followService.followOrNot(followUserId, isFollowed);
    }

    /**
     * 判断是否关注该用户
     * @param followUserId 关注用户的ID
     */
    @GetMapping("/or/not/{id}")
    public Result isFollowed(@PathVariable("id") Long followUserId) {
        return followService.isFollowed(followUserId);
    }

    /*
    * 共同关注*/
    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable("id") Long followUserId) {
        return followService.commonFollow(followUserId);
    }

    @GetMapping("/getFans")
    public Result getFans(){
        return followService.getFans();
    }


}
