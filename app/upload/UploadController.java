package com.fh.controller.app.upload;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.fh.config.CommonConfig;
import com.fh.controller.base.BaseController;
import com.fh.util.AppUtil;
import com.fh.util.FileDownload;
import com.fh.util.FileUpload;
import com.fh.util.PageData;
import com.fh.util.UuidUtil;
import com.fh.util.XcxFileUpload;
import com.fh.util.cos.COSClientUtil;
import com.fh.util.file.UploadNewFile;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.Url.URLAvailability;

@Controller
@RequestMapping(value = "/api/upload")
public class UploadController extends BaseController {

	@SuppressWarnings({ "unused", "rawtypes" })
	@RequestMapping(value = "/wxAppUploadFile", method = RequestMethod.POST)
	@ResponseBody
	public String wxAppUploadFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 接受数据来源
		String fileJson = null;
		String pks = request.getParameter("pks");
		String exp_lot = request.getParameter("pks");
		String usercode = request.getParameter("usercode");
		String lot = request.getParameter("lot");
		String mp = request.getParameter("mp");

		String picUrl = "";
		String returnUrl = "";
		String fileName = "";
		String years = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

		// UFBoolean success=UFBoolean.FALSE;
		// 保存base64图片编码格式数组
		List<Map<String, String>> fileJsons = new ArrayList<Map<String, String>>();
		String success = "false";
		// 本地路径
		long startTime = System.currentTimeMillis();
		// 将当前上下文初始化给 CommonsMutipartResolver （多部分解析器）
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
				request.getSession().getServletContext());
		// 检查form中是否有enctype="multipart/form-data"
		if (multipartResolver.isMultipart(request)) {
			// 将request变成多部分request
			MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
			// 获取multiRequest 中所有的文件名
			Iterator iter = multiRequest.getFileNames();

			while (iter.hasNext()) {
				// 一次遍历所有文件
				MultipartFile file = multiRequest.getFile(iter.next().toString());
				if (file != null) {
					switch (CommonConfig.PIC_FLAG) {
					case 0:// 线上
						picUrl = CommonConfig.PIC_XCX_URL + years + "/";
						returnUrl = CommonConfig.PIC_XCX_HTTP_URL + years + "/";
						break;
					case 1:// 测试
						picUrl = CommonConfig.PIC_CESHI_URL + years + "/";
						returnUrl = CommonConfig.PIC_CESHI_HTTP_URL + years + "/";
						break;
					case 2:// 雪蕊
						picUrl = CommonConfig.PIC_dong_URL + years + "/";
						returnUrl = CommonConfig.PIC_dong_RETURN_URL + years + "/";
						break;
					case 3:// 霍学杰
						picUrl = CommonConfig.PIC_LOCALHOST_URL + years + "/";
						returnUrl = CommonConfig.PIC_LOCALHOST_HTTP_URL + years + "/";
						break;
					default:
						break;
					}
					if ("mp3".equals(mp)) {
						fileName = XcxFileUpload.fileUp(file, picUrl, this.get32UUID() + ".mp3"); // 执行上传
					} else if ("mp4".equals(mp)) {
						fileName = XcxFileUpload.fileUp(file, picUrl, this.get32UUID() + ".mp4"); // 执行上传
					} else {
						fileName = XcxFileUpload.fileUp(file, picUrl, this.get32UUID() + ".jpg"); // 执行上传
					}

					success = returnUrl + fileName;
				}

			}

		}
		long endTime = System.currentTimeMillis();
		Boolean isCan = isTong(success);
		if (!isCan) {
			success = "false";
		} else {// "true,"+
			success = "true," + returnUrl + fileName;
		}
		return success;
	}

	/**
	 * 
	 * 描述：得到图片的长和宽
	 * 
	 * @auther 李聪
	 * @date 2016年11月11日
	 * @version 2.0
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public static String ImageUtils(File fileName) throws IOException {
		Image srcImage = null;
		File srcFile = null;
		File destFile = null;
		String fileSuffix = null;
		int imageWidth = 0;
		int imageHeight = 0;

		File _file = fileName;
		_file.setReadOnly();
		srcFile = _file;
		fileSuffix = _file.getName().substring((_file.getName().indexOf(".") + 1), (_file.getName().length()));
		destFile = new File(srcFile.getPath().substring(0, (srcFile.getPath().lastIndexOf("."))) + "wenjianming" + "."
				+ fileSuffix);
		srcImage = javax.imageio.ImageIO.read(_file);

		// 得到图片的原始大小， 以便按比例压缩。
		imageWidth = srcImage.getWidth(null);
		imageHeight = srcImage.getHeight(null);
		srcImage.flush();
		return imageWidth + "," + imageHeight;
	}

	/**
	 * 将文件压缩后上传
	 * 
	 * @param file     //文件对象
	 * @param filePath //上传路径
	 * @param fileName //文件名
	 * @return 文件名
	 * @throws IOException
	 */
	public static String picUp(MultipartFile[] files) throws IOException {
		String years = new SimpleDateFormat("yyyy-MM").format(new Date());
		String picUrl = "";
		String returnUrl = "";
		String fileName = "";
		String pathAll = "";
		switch (CommonConfig.PIC_FLAG) {
		case 0:// 线上
			picUrl = CommonConfig.PIC_ONLINE_URL + years + "/";
			returnUrl = CommonConfig.PIC_ONLINE_HTTP_URL + years + "/";
			break;
		case 1:// 测试
			picUrl = CommonConfig.PIC_CESHI_URL + years + "/";
			returnUrl = CommonConfig.PIC_CESHI_HTTP_URL + years + "/";
			break;
		case 2:// 雪蕊
			picUrl = CommonConfig.PIC_dong_URL + years + "/";
			returnUrl = CommonConfig.PIC_dong_RETURN_URL + years + "/";
			break;
		case 3:// 霍学杰
			picUrl = CommonConfig.PIC_LOCALHOST_URL + years + "/";
			returnUrl = CommonConfig.PIC_LOCALHOST_HTTP_URL + years + "/";
			break;
		default:
			break;
		}
		if (files != null && files.length > 0) {
			// 循环获取file数组中得文件
			for (int i = 0; i < files.length; i++) {
				MultipartFile file = files[i];
				// 保存文件
				if (!file.isEmpty()) {
					String newfileName = "";
					fileName = FileUpload.fileUp(file, picUrl, UuidUtil.get32UUID()); // 执行上传
					newfileName = fileName;

					// 拼接返回地址
					pathAll = pathAll + returnUrl + newfileName + ",";

				}
			}
		}
		if (pathAll.contains(",")) {
			pathAll = pathAll.substring(0, pathAll.lastIndexOf(","));
		}
		return pathAll;
	}

	/**
	 * 
	 * 描述：图片上传模块
	 * 
	 * @author 董雪蕊，上传完之后，判断图片
	 * @date 2016年9月11日
	 * @version 2.0
	 * @param request
	 * @param files
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/picPut", method = RequestMethod.POST)
	@ResponseBody
	public Object picPut(HttpServletRequest request, @RequestParam("files") MultipartFile[] files,
			@RequestParam(value = "international", required = false) String international) throws Exception {
		String pathAll = "";// 所有的文件返回地址。
		logBefore(logger, "图片上传模块");
		Map<String, String> map = new HashMap<String, String>();
		String years = new SimpleDateFormat("yyyy-MM").format(new Date());
		String picUrl = "";
		String returnUrl = "";
		String fileName = "";
		PageData pd = new PageData();
		boolean international2 = false;// 国内版
		if (IsNull.paramsIsNull(international) == false) {
			if ("1".equals(international)) {
				international2 = true;// 国际版
			}
		}
		
		switch (CommonConfig.PIC_FLAG) {
		case 0:// 线上
			picUrl = CommonConfig.PIC_ONLINE_URL + years + "/";
			returnUrl = CommonConfig.PIC_ONLINE_HTTP_URL + years + "/";
			break;
		case 1:// 测试
			picUrl = CommonConfig.PIC_CESHI_URL + years + "/";
			returnUrl = CommonConfig.PIC_CESHI_HTTP_URL + years + "/";
			break;
		case 2:// 雪蕊
			picUrl = CommonConfig.PIC_dong_URL + years + "/";
			returnUrl = CommonConfig.PIC_dong_RETURN_URL + years + "/";
			break;
		case 3:// 霍学杰
			picUrl = CommonConfig.PIC_LOCALHOST_URL + years + "/";
			returnUrl = CommonConfig.PIC_LOCALHOST_HTTP_URL + years + "/";
			break;
		default:
			break;
		}
		if (files != null && files.length > 0) {
			// 循环获取file数组中得文件
			for (int i = 0; i < files.length; i++) {
				MultipartFile file = files[i];
				// 保存文件
				if (!file.isEmpty()) {
					String newfileName = "";
					fileName = FileUpload.fileUp(file, picUrl, UuidUtil.get32UUID());
					newfileName = fileName;

					pathAll = pathAll + returnUrl + newfileName + ",";

				}
			}
		}
		if (pathAll.contains(",")) {
			pathAll = pathAll.substring(0, pathAll.lastIndexOf(","));
		}
		map.put("path", pathAll);

		/**
		 * 判断文章图片能不能用，如果不能,报错
		 */
		Boolean can = true;
		String pathS[] = pathAll.split(",");
		URLAvailability u = new URLAvailability();
		for (int i = 0; i < pathS.length; i++) {
			if (u.isConnect(pathS[i]) != 200) {
				can = false;
				break;
			}
		}
		if (can) {// 图片上传错误
			if (international2) {
				map.put("result", "0000");
				map.put("retMessage", "Success");
			} else {
				map.put("result", "0000");
				map.put("retMessage", "图片上传成功!");
			}
		} else {
			
			if (international2) {
				map.put("result", "7003");
				map.put("retMessage", "Image upload failed, please check the network connection");
			} else {
				map.put("result", "7003");
				map.put("retMessage", "图片上传失败，请检查网络连接!");
			}
		}
		logAfter(logger);
		return AppUtil.returnObject(pd, map);
	}

	/**
	 * 判断文章图片能不能用，如果不能,报错
	 */
	public static boolean isTong(String pathAll) {
		Boolean can = true;
		String pathS[] = pathAll.split(",");
		URLAvailability u = new URLAvailability();
		for (int i = 0; i < pathS.length; i++) {
			if (u.isConnect(pathS[i]) != 200) {
				can = false;
				break;
			}
		}
		return can;
	}

	/**
	 * 将图片删除
	 */
	public static void delPicture(String pathAll) {
		String pathS[] = pathAll.split(",");
		for (int i = 0; i < pathS.length; i++) {
			String path = pathS[i];
			switch (CommonConfig.PIC_FLAG) {
			case 0:// 线上
				path = path.replace(CommonConfig.PIC_ONLINE_HTTP_URL, CommonConfig.PIC_ONLINE_URL);
				break;
			case 1:// 测试
				path = path.replace(CommonConfig.PIC_CESHI_HTTP_URL, CommonConfig.PIC_CESHI_URL);
				break;
			case 2:// 雪蕊
				path = path.replace(CommonConfig.PIC_dong_RETURN_URL, CommonConfig.PIC_dong_URL);
				break;
			case 3:// 霍学杰
				path = path.replace(CommonConfig.PIC_LOCALHOST_HTTP_URL, CommonConfig.PIC_LOCALHOST_URL);
				break;
			default:
				break;
			}
			File myFile = new File(path);
			// 删除图片
			// 路径为文件且不为空则进行删除
			if (myFile.isFile() && myFile.exists()) {
				myFile.delete();
			}
		}
	}

	/**
	 * 从腾讯云下载语音、文件配地址
	 * 
	 * @param Url
	 * @param type 0图片 1语音
	 * @throws IOException
	 */
	public static String getUrl(String Url, int type) throws IOException {
		String typeS = "pic/";
		String exName = ".jpg";
		if (type == 1) {
			typeS = "sound/";
			exName = ".mp3";
		}
		String fileName = UuidUtil.get32UUID() + exName;
		String years = new SimpleDateFormat("yyyy-MM").format(new Date());
		String picUrl = "";
		String returnUrl = "";
		// String pathAll="";
		switch (CommonConfig.PIC_FLAG) {
		case 0:// 线上
			picUrl = CommonConfig.PIC_ONLINE_URL + "/tencent/" + typeS + years + "/";
			returnUrl = CommonConfig.PIC_ONLINE_HTTP_URL + "/tencent/" + typeS + years + "/";
			break;
		case 1:// 测试
			picUrl = CommonConfig.PIC_CESHI_URL + "/tencent/" + typeS + years + "/";
			returnUrl = CommonConfig.PIC_CESHI_HTTP_URL + "/tencent/" + typeS + years + "/";
			break;
		case 2:// 雪蕊
			picUrl = CommonConfig.PIC_dong_URL + "/tencent/" + typeS + years + "/";
			returnUrl = CommonConfig.PIC_dong_RETURN_URL + "/tencent/" + typeS + years + "/";
			break;
		case 3:// 霍学杰
			picUrl = CommonConfig.PIC_LOCALHOST_URL + "/tencent/" + typeS + years + "/";
			returnUrl = CommonConfig.PIC_LOCALHOST_HTTP_URL + "/tencent/" + typeS + years + "/";
			break;
		default:
			break;
		}
		// 上传
		UploadNewFile.downloadNet(Url, picUrl + fileName);
		// 判断是否通
		if (isTong(returnUrl + fileName)) {
			return returnUrl + fileName;

		} else {
			return "";
		}

	}

	/**
	 * url获取文件
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @author 王立飞
	 * @date 2018年5月23日 上午10:35:15
	 */
	@RequestMapping(value = "/wxAppDownloadURL", method = RequestMethod.POST)
	@ResponseBody
	public String wxAppDownloadURL(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 接受数据来源
		String urlStr = request.getParameter("urlStr");

		String picUrl = "";
		String returnUrl = "";
		String fileName = "";
		String years = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String success = "false";
		switch (CommonConfig.PIC_FLAG) {
		case 0:// 线上
			picUrl = CommonConfig.PIC_XCX_URL + years + "/";
			returnUrl = CommonConfig.PIC_XCX_HTTP_URL + years + "/";
			break;
		case 1:// 测试
			picUrl = CommonConfig.PIC_CESHI_URL + years + "/";
			returnUrl = CommonConfig.PIC_CESHI_HTTP_URL + years + "/";
			break;
		case 2:// 雪蕊
			picUrl = CommonConfig.PIC_dong_URL + years + "/";
			returnUrl = CommonConfig.PIC_dong_RETURN_URL + years + "/";
			break;
		case 3:// 霍学杰
			picUrl = CommonConfig.PIC_LOCALHOST_URL + years + "/";
			returnUrl = CommonConfig.PIC_LOCALHOST_HTTP_URL + years + "/";
			break;
		default:
			break;
		}
		fileName = FileDownload.downLoadFromUrl(urlStr, this.get32UUID() + ".mp3", picUrl);

		success = returnUrl + fileName;

		Boolean isCan = isTong(success);
		if (!isCan) {
			success = "false";
		} else {// "true,"+
			success = "true," + returnUrl + fileName;
		}
		return success;
	}

	/**
	 * 王立飞 将文件上传COS 多个
	 * 
	 * @param file     //文件对象
	 * @param filePath //上传路径
	 * @param fileName //文件名
	 * @return 文件名
	 * @throws Exception
	 */
	public static String picUpCOS(MultipartFile[] files) throws Exception {

		String pathAll = "";
		String name = "";
		String imgUrl = "";
		String[] split = null;
		if (files != null && files.length > 0) {
			COSClientUtil cosClientUtil = new COSClientUtil();
			// 循环获取file数组中得文件
			for (int i = 0; i < files.length; i++) {
				MultipartFile file = files[i];
				// 保存文件
				if (!file.isEmpty()) {
					String newfileName = "";
					name = cosClientUtil.uploadFile2Cos(file);
					imgUrl = cosClientUtil.getImgUrl(name);
					split = imgUrl.split("\\?");
					newfileName = split[0];
					// 拼接返回地址
					pathAll = pathAll + newfileName + ",";

				}
			}
		}
		if (pathAll.contains(",")) {
			pathAll = pathAll.substring(0, pathAll.lastIndexOf(","));
		}
		return pathAll;
	}

	/**
	 * 王立飞 将文件上传COS 多单个
	 * 
	 * @param file     //文件对象
	 * @param filePath //上传路径
	 * @param fileName //文件名
	 * @return 文件名
	 * @throws Exception
	 */
	public static String picCOS(MultipartFile file) throws Exception {

		String pathAll = "";
		String name = "";
		String imgUrl = "";
		String[] split = null;
		if (!file.isEmpty()) {
			COSClientUtil cosClientUtil = new COSClientUtil();
			// 保存文件
			String newfileName = "";
			name = cosClientUtil.uploadFile2Cos(file);
			imgUrl = cosClientUtil.getImgUrl(name);
			split = imgUrl.split("\\?");
			newfileName = split[0];
			// 返回地址
			pathAll = newfileName;
		}
		return pathAll;
	}

	/**
	 * 上传图片,限制5张以内 本地服务器
	 * 
	 * @author 王立飞
	 * @date 2018年6月4日 上午10:14:03
	 */
	@RequestMapping(value = "/picFive", method = RequestMethod.POST)
	@ResponseBody
	public Object picFive(HttpServletRequest request, @RequestParam("files") MultipartFile[] files) throws Exception {
		String pathAll = "";// 所有的文件返回地址。
		logBefore(logger, "上传图片,限制5张以内");
		Map<String, String> map = new HashMap<String, String>();
		String years = new SimpleDateFormat("yyyy-MM").format(new Date());
		String picUrl = "";
		String returnUrl = "";
		String fileName = "";
		PageData pd = new PageData();
		if (files != null && files.length > 5) {
			map.put("result", "3001");
			map.put("retMessage", "图片超过了5张！");
		} else {
			switch (CommonConfig.PIC_FLAG) {
			case 0:// 线上
				picUrl = CommonConfig.PIC_ONLINE_URL + years + "/";
				returnUrl = CommonConfig.PIC_ONLINE_HTTP_URL + years + "/";
				break;
			case 1:// 测试
				picUrl = CommonConfig.PIC_CESHI_URL + years + "/";
				returnUrl = CommonConfig.PIC_CESHI_HTTP_URL + years + "/";
				break;
			case 2:// 雪蕊
				picUrl = CommonConfig.PIC_dong_URL + years + "/";
				returnUrl = CommonConfig.PIC_dong_RETURN_URL + years + "/";
				break;
			case 3:// 霍学杰
				picUrl = CommonConfig.PIC_LOCALHOST_URL + years + "/";
				returnUrl = CommonConfig.PIC_LOCALHOST_HTTP_URL + years + "/";
				break;
			default:
				break;
			}
			if (files != null && files.length > 0) {
				// 循环获取file数组中得文件
				for (int i = 0; i < files.length; i++) {
					MultipartFile file = files[i];
					// 保存文件
					if (!file.isEmpty()) {
						String newfileName = "";
						fileName = FileUpload.fileUp(file, picUrl, UuidUtil.get32UUID());
						newfileName = fileName;
						// 拼接返回地址
						pathAll = pathAll + returnUrl + newfileName + ",";
					}
				}
			}
			if (pathAll.contains(",")) {
				pathAll = pathAll.substring(0, pathAll.lastIndexOf(","));
			}
			map.put("path", pathAll);
			/**
			 * 判断文章图片能不能用，如果不能,报错
			 */
			Boolean can = true;
			String pathS[] = pathAll.split(",");
			URLAvailability u = new URLAvailability();
			for (int i = 0; i < pathS.length; i++) {
				if (u.isConnect(pathS[i]) != 200) {
					can = false;
					break;
				}
			}
			if (can) {// 图片上传错误
				map.put("result", "0000");
				map.put("retMessage", "图片上传成功!");
			} else {
				map.put("result", "7003");
				map.put("retMessage", "图片上传失败，请检查网络连接!");
			}
		}
		return AppUtil.returnObject(pd, map);
	}

	/**
	 * 删除COS文件
	 * 
	 * @author 王立飞
	 * @date 2018年7月30日 下午3:11:21
	 */
	public static void DelCOS(String fileUrl) throws Exception {
		// 创建
		COSClientUtil cosClientUtil = new COSClientUtil();
		// 执行
		cosClientUtil.DelImgUrl(fileUrl);
		// 销毁
		cosClientUtil.destory();
	}
}
