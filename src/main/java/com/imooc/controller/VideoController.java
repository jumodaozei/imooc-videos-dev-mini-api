package com.imooc.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.imooc.pojo.Bgm;
import com.imooc.service.BgmService;
import com.imooc.utils.IMoocJSONResult;
import com.imooc.utils.MergeVideoMp3;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Api(value = "视频相关业务接口", tags = { "视频相关业务的controller" })
@RequestMapping("/video")
public class VideoController extends BasicController {
	
	@Autowired
	private BgmService bgmService;

	@ApiOperation(value = "用户上传视频", notes = "用户上传视频的接口")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "String", paramType = "form"),
			@ApiImplicitParam(name = "bgmId", value = "背景音乐id", required = false, dataType = "String", paramType = "form"),
			@ApiImplicitParam(name = "videoSeconds", value = "背景音乐播放长度", required = true, dataType = "double", paramType = "form"),
			@ApiImplicitParam(name = "videoWidth", value = "视频宽度", required = true, dataType = "int", paramType = "form"),
			@ApiImplicitParam(name = "videoHeight", value = "视频高度", required = true, dataType = "int", paramType = "form"),
			@ApiImplicitParam(name = "desc", value = "视频描述", required = false, dataType = "String", paramType = "form") })
	@PostMapping(value = "/upload", headers = "content-type=multipart/form-data")
	public IMoocJSONResult uploadFace(String userId, String bgmId, double videoSeconds, int videoWidth, int videoHeight,
			String desc,
			@ApiParam(value = "短视频", required = true)
			MultipartFile file) throws Exception {

		if (StringUtils.isBlank(userId)) {
			return IMoocJSONResult.errorMsg("用户id不能为空！");
		}

		// 用户上传文件的命名空间
		//String fileSpace = "D:\\imooc-videos-dev";

		// 保存到数据库中的路径 相对路径
		String uploadPathDB = "/" + userId + "/video";
		FileOutputStream fileOutputStream = null;
		InputStream inputStream = null;
		// 文件上传后的最终保存路径 绝对路径
		String finalVideoPath = "";
		try {
			if (file != null) {

				String fileName = file.getOriginalFilename();
				if (StringUtils.isNotBlank(fileName)) {
					
					finalVideoPath = FILESPACE + uploadPathDB + "/" + fileName;
					// 数据库保存的路径 相对路径
					uploadPathDB += ("/" + fileName);

					File outFile = new File(finalVideoPath);
					if (outFile.getParentFile() != null || !outFile.getParentFile().isDirectory()) {
						// 创建父文件夹
						outFile.getParentFile().mkdirs();
					}

					fileOutputStream = new FileOutputStream(outFile);
					inputStream = file.getInputStream();
					IOUtils.copy(inputStream, fileOutputStream);
				}
			} else {
				return IMoocJSONResult.errorMsg("上传出错！");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return IMoocJSONResult.errorMsg("上传出错！");
		} finally {
			if (fileOutputStream != null) {
				fileOutputStream.flush();
				fileOutputStream.close();
			}
		}
		
		//判断是否有bgmid 如果不为空 则需要把视频和bgm合并
		if(StringUtils.isNotBlank(bgmId)) {
			Bgm bgm = bgmService.querBgmById(bgmId);
			String mp3InputPath = FILESPACE + bgm.getPath();
			
			//String ffmpegEXE = "D:\\ffmpeg\\bin\\ffmpeg.exe";
			MergeVideoMp3 tool = new MergeVideoMp3(FFMPEGEXE);
			String videoInputPath = finalVideoPath;
			String videoOutputName =  UUID.randomUUID().toString() + ".mp4";
			uploadPathDB = "/" + userId + "/video/" + videoOutputName;
			finalVideoPath = FILESPACE + uploadPathDB;
			tool.convertor(videoInputPath, mp3InputPath, videoSeconds, finalVideoPath);
		}
		
		System.out.println("uploadPathDB:" + uploadPathDB);
		System.out.println("finalVideoPath:" + finalVideoPath);

		return IMoocJSONResult.ok();
	}
}
