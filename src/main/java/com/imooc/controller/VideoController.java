package com.imooc.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.imooc.enums.VideoStatusEnum;
import com.imooc.pojo.Bgm;
import com.imooc.pojo.Comments;
import com.imooc.pojo.Videos;
import com.imooc.service.BgmService;
import com.imooc.service.VideoService;
import com.imooc.utils.FetchVideoCover;
import com.imooc.utils.IMoocJSONResult;
import com.imooc.utils.MergeVideoMp3;
import com.imooc.utils.PagedResult;

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
	
	@Autowired
	private VideoService videoService;

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
		
		String coverPathDB = "/" + userId + "/video";
		
		FileOutputStream fileOutputStream = null;
		InputStream inputStream = null;
		// 文件上传后的最终保存路径 绝对路径
		String finalVideoPath = "";
		try {
			if (file != null) {

				String fileName = file.getOriginalFilename();
				
				String fileNamePrefix = fileName.split("\\.")[0];
				
				if (StringUtils.isNotBlank(fileName)) {
					
					finalVideoPath = FILESPACE + uploadPathDB + "/" + fileName;
					// 数据库保存的路径 相对路径
					uploadPathDB += ("/" + fileName);
					
					coverPathDB = coverPathDB + "/" + fileNamePrefix + ".jpg";

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
		
		//对视频截图
		
		FetchVideoCover fetchVideoCover = new FetchVideoCover(FFMPEGEXE);
		fetchVideoCover.getCover(finalVideoPath,FILESPACE + coverPathDB);
		
		//保存视频信息到数据库
		Videos video = new Videos();
		video.setAudioId(bgmId);
		video.setUserId(userId);
		video.setVideoSeconds((float)videoSeconds);
		video.setVideoHeight(videoHeight);
		video.setVideoWidth(videoWidth);
		video.setVideoDesc(desc);
		video.setVideoPath(uploadPathDB);
		video.setCoverPath(coverPathDB);
		video.setStatus(VideoStatusEnum.SUCCESS.getValue());
		video.setCreateTime(new Date());
		String videoid = videoService.saveVideo(video);
		

		return IMoocJSONResult.ok(videoid);
	}
	
	@ApiOperation(value = "上传封面", notes = "用户上传封面的接口")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "videoId", value = "视频主键id", required = true, dataType = "String", paramType = "form"),
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "String", paramType = "form"),
			})
	@PostMapping(value = "/uploadCover", headers = "content-type=multipart/form-data")
	public IMoocJSONResult uploadCover(String videoId,
			String userId,
			@ApiParam(value = "封面", required = true)
			MultipartFile file) throws Exception {

		if (StringUtils.isBlank(videoId) || StringUtils.isBlank(userId)) {
			return IMoocJSONResult.errorMsg("视频主键id或用户id不能为空！");
		}

		// 用户上传文件的命名空间
		//String fileSpace = "D:\\imooc-videos-dev";

		// 保存到数据库中的路径 相对路径
		String uploadPathDB = "/" + userId + "/video";
		FileOutputStream fileOutputStream = null;
		InputStream inputStream = null;
		// 文件上传后的最终保存路径 绝对路径
		String finalCoverPath = "";
		try {
			if (file != null) {

				String fileName = file.getOriginalFilename();
				if (StringUtils.isNotBlank(fileName)) {
					
					finalCoverPath = FILESPACE + uploadPathDB + "/" + fileName;
					// 数据库保存的路径 相对路径
					uploadPathDB += ("/" + fileName);

					File outFile = new File(finalCoverPath);
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
		
		videoService.updateVideo(videoId, uploadPathDB);
		
		return IMoocJSONResult.ok();
	}
	
	/**
	 * 
	 * @param video
	 * @param isSaveRecord 1-需要保存 0-不需要保存
	 * @param page
	 * @return
	 * @throws Exception
	 */
	@PostMapping(value = "/showAll")
	public IMoocJSONResult showAll(@RequestBody Videos video, Integer isSaveRecord, Integer page) throws Exception {
		
		if(page == null) {
			page = 1;
		}
		PagedResult result = videoService.getAllVideos(video,isSaveRecord,page, PAGE_SIZE);
		
		return IMoocJSONResult.ok(result);
	}
	
	@PostMapping(value = "/showMyLike")
	public IMoocJSONResult showMyLike(String userId, Integer page, Integer pageSize) throws Exception {
		
		if(StringUtils.isBlank(userId)) {
			return IMoocJSONResult.errorMap("userId不能为空！");
		}
		
		if(page == null) {
			page = 1;
		}
		if(pageSize == null) {
			pageSize = 6;
		}
		
		PagedResult videoList = videoService.queryMyLikeVideos(userId, page, pageSize);
		
		return IMoocJSONResult.ok(videoList);
	}
	
	/**
	 * @Description: 我关注的人发的视频
	 */
	@PostMapping("/showMyFollow")
	public IMoocJSONResult showMyFollow(String userId, Integer page) throws Exception {
		
		if (StringUtils.isBlank(userId)) {
			return IMoocJSONResult.ok();
		}
		
		if (page == null) {
			page = 1;
		}

		int pageSize = 6;
		
		PagedResult videosList = videoService.queryMyFollowVideos(userId, page, pageSize);
		
		return IMoocJSONResult.ok(videosList);
	}
	
	
	@PostMapping(value = "/hot")
	public IMoocJSONResult hot() throws Exception {
		
		return IMoocJSONResult.ok(videoService.getHotWords());
	}
	
	@PostMapping(value = "/userLike")
	public IMoocJSONResult userLike(String userId, String videoId, String videoCreaterId) throws Exception {
		videoService.userLikeVideo(userId, videoId, videoCreaterId);
		return IMoocJSONResult.ok();
	}
	
	@PostMapping(value = "/userUnLike")
	public IMoocJSONResult userUnLike(String userId, String videoId, String videoCreaterId) throws Exception {
		videoService.userUnLikeVideo(userId, videoId, videoCreaterId);
		return IMoocJSONResult.ok();
	}
	
	@PostMapping("/saveComment")
	public IMoocJSONResult saveComment(@RequestBody Comments comment, String fatherCommentId, String toUserId) throws Exception {
		comment.setFatherCommentId(fatherCommentId);
		comment.setToUserId(toUserId);
		videoService.saveComment(comment);
		return IMoocJSONResult.ok();
	}
	
	@PostMapping("/getVideoComments")
	public IMoocJSONResult getVideoComments(String videoId, Integer page, Integer pageSize) throws Exception {
		
		if (StringUtils.isBlank(videoId)) {
			return IMoocJSONResult.ok();
		}
		// 分页查询视频列表，时间顺序倒序排序
		if (page == null) {
			page = 1;
		}
		if (pageSize == null) {
			pageSize = 10;
		}
		PagedResult list = videoService.getAllComments(videoId, page, pageSize);
		return IMoocJSONResult.ok(list);
	}
}
