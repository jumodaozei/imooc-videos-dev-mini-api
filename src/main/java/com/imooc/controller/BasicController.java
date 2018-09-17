package com.imooc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.imooc.utils.RedisOperator;

@RestController
public class BasicController {
	
	@Autowired
	public RedisOperator redis;
	
	public static final String USER_REDIS_SESSION = "user-redis-session";
	
	//文件保存命名空间
	public static final String FILESPACE = "D:\\imooc-videos-dev";
	
	
	//ffmpeg所在目录
	public static final String FFMPEGEXE = "D:\\ffmpeg\\bin\\ffmpeg.exe";
	
	//分页
	public static final Integer PAGE_SIZE = 5;
	
}