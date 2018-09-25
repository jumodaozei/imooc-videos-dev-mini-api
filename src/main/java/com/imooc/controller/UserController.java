package com.imooc.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.imooc.pojo.Users;
import com.imooc.pojo.vo.PublisherVideo;
import com.imooc.pojo.vo.UsersVO;
import com.imooc.service.UserService;
import com.imooc.utils.IMoocJSONResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;

@RestController
@Api(value = "用户相关业务接口", tags = { "用户相关业务的controller" })
@RequestMapping("/user")
public class UserController extends BasicController {

	@Autowired
	private UserService userServicel;

	@ApiOperation(value = "用户上传头像", notes = "用户上传头像的接口")
	@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "String", paramType = "query")
	@PostMapping("/uploadFace")
	public IMoocJSONResult uploadFace(String userId, @RequestParam("file") MultipartFile[] files) throws Exception {
		
		
		if(StringUtils.isBlank(userId)) {
			return IMoocJSONResult.errorMsg("用户id不能为空！");
		}

		// 用户上传文件的命名空间
		String fileSpace = "D:\\imooc-videos-dev";

		// 保存到数据库中的路径 相对路径
		String uploadPathDB = "/" + userId + "/face";
		FileOutputStream fileOutputStream = null;
		InputStream inputStream = null;
		try {
			if (files != null && files.length > 0) {
				
				String fileName = files[0].getOriginalFilename();
				if (StringUtils.isNotBlank(fileName)) {
					// 文件上传后的最终保存路径 绝对路径
					String finalFacePath = fileSpace + uploadPathDB + "/" + fileName;
					// 数据库保存的路径 相对路径
					uploadPathDB += ("/" + fileName);

					File outFile = new File(finalFacePath);
					if (outFile.getParentFile() != null || !outFile.getParentFile().isDirectory()) {
						// 创建父文件夹
						outFile.getParentFile().mkdirs();
					}

					fileOutputStream = new FileOutputStream(outFile);
					inputStream = files[0].getInputStream();
					IOUtils.copy(inputStream, fileOutputStream);
				}
			}else {
				return IMoocJSONResult.errorMsg("上传出错！");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return IMoocJSONResult.errorMsg("上传出错！");
		}finally {
			if(fileOutputStream != null) {
				fileOutputStream.flush();
				fileOutputStream.close();
			}
		}
		
		Users user = new Users();
		user.setId(userId);
		user.setFaceImage(uploadPathDB);
		userServicel.updateUserInfo(user);
		

		return IMoocJSONResult.ok(uploadPathDB);
	}
	
	@ApiOperation(value = "查询用户信息", notes = "查询用户信息的接口")
	@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "String", paramType = "query")
	@PostMapping("/query")
	public IMoocJSONResult query(String userId) throws Exception {
		
		
		if(StringUtils.isBlank(userId)) {
			return IMoocJSONResult.errorMsg("用户id不能为空！");
		}
		
		Users user = userServicel.queryUserInfo(userId);
		UsersVO userVO = new UsersVO();
		BeanUtils.copyProperties(user, userVO);
		
		return IMoocJSONResult.ok(userVO);
	}
	
	@PostMapping("/queryPublisher")
	public IMoocJSONResult queryPublisher(String loginuserId,String videoId,String publisherUserId) throws Exception {
		
		//可能有游客点击 页面上需要展示发布者的信息  所以只需要判断这个id不为空即可
		if(StringUtils.isBlank(publisherUserId)) {
			return IMoocJSONResult.errorMsg("");
		}
		
		//1 查询视频发布者的信息
		Users user = userServicel.queryUserInfo(publisherUserId);
		UsersVO publisher = new UsersVO();
		BeanUtils.copyProperties(user, publisher);
		
		//2 查询当前登陆者和视频的点赞关系
		boolean userLikeVideo = userServicel.isUserLikeVideo(loginuserId, videoId);
		
		PublisherVideo bean = new PublisherVideo();
		bean.setPublisher(publisher);
		bean.setUserLikeVideo(userLikeVideo);
		
		return IMoocJSONResult.ok(bean);
	}

}
