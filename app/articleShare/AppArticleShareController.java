package com.fh.controller.app.articleShare;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import com.fh.config.CommonMessage;
import com.fh.controller.app.upload.UploadController;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ArticleShareService;
import com.fh.service.app.CrashLogService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PagingUtil;

@Controller
@RequestMapping(value = "/api/articleShare")
public class AppArticleShareController extends BaseController {

	@Resource
	ArticleShareService articleShareService;
	@Resource
	private CrashLogService crashLogService;

	/**
	 * 新建分享素材
	 * 
	 * @author 董雪蕊 2018-03-05
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Object save(HttpServletRequest request,
			@RequestParam(value = "files", required = false) MultipartFile[] files,
			@RequestParam(value = "clinicId", required = false) String clinicId,
			@RequestParam(value = "doctorId", required = false) String doctorId,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "showType", required = false) String showType,
			@RequestParam(value = "jumpLink", required = false) String jumpLink,
			@RequestParam(value = "content", required = false) String content) {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		String result = "0000";
		String message = "查询成功";
		try {
			// 第一步：将所有参数保存
			pd.put("clinicId", clinicId);
			pd.put("doctorId", doctorId);
			pd.put("title", title);
			pd.put("showType", showType);
			pd.put("content", content);
			pd.put("jumpLink", jumpLink);
			Boolean isTong = true;
			if (files != null && files.length > 0) {
				String pathAll = UploadController.picUp(files);
				isTong = UploadController.isTong(pathAll);
				if (isTong) {
					pd.put("picUrlS", pathAll);
					pd.put("picLength", files.length);
				} else {
					isTong = false;
				}
			}
			if (isTong) {
				articleShareService.save(pd);
				result = "0000";
				message = "保存成功";
			} else {
				result = "7003";
				message = CommonMessage.CODE_7003;
			}

		} catch (Exception e) {
			result = "9999";
			message = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 分享素材列表
	 * 
	 * @author 董雪蕊 2018-03-05
	 */
	@RequestMapping(value = "/list")
	@ResponseBody
	public Object list() {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "查询成功";
		try {
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("pageIndex"))
					|| IsNull.paramsIsNull(pd.get("pageSize"))) {
				result = "9993";
				message = "参数异常";
				map.put("message", message);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				pd = PagingUtil.addPaging(pd);
				List<PageData> list = articleShareService.listByClinic(pd);
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
				} else {
					map.put("list", new ArrayList<>());
					map.put("size", 0);
				}
				result = "0000";
				message = "查询成功";
			}
		} catch (Exception e) {
			result = "9999";
			message = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 修改素材
	 * 
	 * @author 董雪蕊 2018-03-05
	 */
	@RequestMapping(value = "/edit", method = RequestMethod.POST)
	@ResponseBody
	public Object edit(HttpServletRequest request,
			@RequestParam(value = "files", required = false) MultipartFile[] files,
			@RequestParam(value = "articleShareId", required = true) String articleShareId,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "showType", required = false) String showType,
			@RequestParam(value = "jumpLink", required = false) String jumpLink,
			@RequestParam(value = "content", required = false) String content) {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		String result = "0000";
		String message = "查询成功";
		try {
			// 第一步：将所有参数保存
			pd.put("articleShareId", articleShareId);
			pd.put("title", title);
			pd.put("showType", showType);
			pd.put("content", content);
			pd.put("jumpLink", jumpLink);
			Boolean isTong = true;
			if (files != null && files.length > 0) {
				String pathAll = UploadController.picUp(files);
				isTong = UploadController.isTong(pathAll);
				if (isTong) {
					pd.put("picUrlS", pathAll);
					pd.put("picLength", files.length);
					// 把旧图片删除掉
					PageData data = articleShareService.findById(pd);
					if (IsNull.paramsIsNull(data.get("picUrlS")) == false) {
						UploadController.delPicture(data.get("picUrlS").toString());
					}
				}
			}
			// 第二步：将素材修改
			if (isTong) {
				articleShareService.updateArticle(pd);
				result = "0000";
				message = "修改成功";
			} else {
				result = "7003";
				message = CommonMessage.CODE_7003;
			}

		} catch (Exception e) {
			result = "9999";
			message = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 删除分享素材
	 * 
	 * @author 董雪蕊 2018-03-05
	 * @throws Exception
	 */
	@RequestMapping(value = "/delArticleShare")
	@ResponseBody
	public Object delArticleShare() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "查询成功";
		try {
			if (IsNull.paramsIsNull(pd.get("articleShareId"))) {
				result = "9993";
				message = "参数异常";
				map.put("message", message);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				PageData data = articleShareService.findById(pd);
				// 先将图片删除，再将数据删除
				if (IsNull.paramsIsNull(data.get("picUrlS")) == false) {
					UploadController.delPicture(data.get("picUrlS").toString());
				}
				articleShareService.del(pd);
				result = "0000";
				message = "删除成功";
			}
		} catch (Exception e) {
			PageData data = new PageData();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			data.put("content", sw.toString());
			data.put("logType", 0);
			data.put("createTime", DateUtil.getTime());
			crashLogService.save(data);
			result = "9999";
			message = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}
}
