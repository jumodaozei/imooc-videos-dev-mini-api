package com.imooc.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.imooc.pojo.Users;
import com.imooc.service.UserService;
import com.imooc.utils.IMoocJSONResult;
import com.imooc.utils.MD5Utils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@Api(value = "用户注册登录接口", tags = { "注册和登录的controller" })
public class RegistLoginController {

	@Autowired
	private UserService userServicel;

	@ApiOperation(value="用户注册",notes="用户注册的接口")
	@PostMapping("/regist")
	public IMoocJSONResult regist(@RequestBody Users user) throws Exception {
		// 1 判断用户名密码必须不为空
		if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
			return IMoocJSONResult.errorMsg("用户名或密码不能为空！");
		}
		// 2 判断用户名是否存在
		boolean usernameIsExist = userServicel.queryUserNameIsExists(user.getUsername());
		// 3 保存注册的用户信息
		if (!usernameIsExist) {
			user.setNickname(user.getUsername());
			user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
			user.setFansCounts(0);
			user.setReceiveLikeCounts(0);
			user.setFaceImage(null);
			user.setFollowCounts(0);
			userServicel.saveUser(user);
		} else {
			return IMoocJSONResult.errorMsg("用户名已存在！");
		}
		return IMoocJSONResult.ok();
	}

}
