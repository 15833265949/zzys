package com.fh.controller.app.article;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fh.config.CommonConfig;
import com.fh.config.CommonMessage;
import com.fh.controller.app.upload.UploadController;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ArticleService;
import com.fh.service.app.ClinicService;
import com.fh.service.statistics.TjFunctionService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PagingUtil;

/**
 * 描述：公告逻辑处理类
 * 
 * @author 董雪蕊
 * @date 2017.10.16 版本：1.0 备注：霍学杰 20180527 修改
 */

@Controller
@RequestMapping(value = "/api/article")
public class AppArticleControler extends BaseController {

	@Resource
	private ArticleService articleService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private TjFunctionService functionService;

	/**
	 * app医生端 使用 描述：发布动态接口
	 * 
	 * @author 董雪蕊
	 * @date 2017年10月31日
	 */
	@RequestMapping(value = "/save")
	@ResponseBody
	public Object picUpload(HttpServletRequest request,
			@RequestParam(value = "files", required = false) MultipartFile[] files,
			@RequestParam(value = "doctorId", required = false) String doctorId,
			@RequestParam(value = "clinicId", required = false) String clinicId,
			@RequestParam(value = "articleTitle", required = false) String articleTitle,
			@RequestParam(value = "content", required = false) String content) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String result = "";
		String retMessage = "";
		String pathAll = "";
		String picSize = "";
		PageData pd = new PageData();

		if (IsNull.paramsIsNull(doctorId) || IsNull.paramsIsNull(content) || IsNull.paramsIsNull(clinicId)) {
			result = "9993";
			retMessage = "参数异常";
		} else if (files != null && files.length > 9) {
			result = "3001";
			retMessage = "图片超过了9张！";
		} else {
			pd.put("doctorId", doctorId);
			pd.put("clinicId", clinicId);
			pd.put("articleTitle", articleTitle);
			pd.put("content", content);
			pd.put("articleId", this.get32UUID());
			pd.put("createTime", DateUtil.getTime());
			PageData clinicData = clinicService.findzsClinic(pd);
			if (IsNull.paramsIsNull(clinicData) == false) {
				pd.put("clinicName", clinicData.get("clinicName"));
			} /*
				 * else { result="10000"; retMessage="发布失败"; }
				 */

			boolean isTong = true;
			if (files != null && files.length > 0) {
				MultipartFile file = files[0];
				BufferedImage image = ImageIO.read(file.getInputStream());
				if (image != null) {// 如果image=null 表示上传的不是图片格式
					picSize = image.getHeight() + "," + image.getWidth();
				}
				pathAll = UploadController.picUpCOS(files);
				isTong = UploadController.isTong(pathAll);
			}
			if (isTong) {
				int picLen = 0;
				if (files != null) {
					picLen = files.length;
				}
				pd.put("pictures", pathAll);
				pd.put("picLen", picLen);
				pd.put("picSize", picSize);
				articleService.save(pd);
				result = "0000";
				retMessage = "发布成功";

				// ==============================================
				// 统计
				pd.put("dayDate", DateUtil.getDay());
				pd.put("userId", pd.get("doctorId"));
				pd.put("fType", "1");
				pd.put("fState", "图文动态");
				PageData data = functionService.getDayUser(pd);
				if (data == null) {
					// 不存在,添加
					pd.put("fNum", "1");
					pd.put("updateTime", new Date());
					functionService.save(pd);
				} else {
					// 存在,更新数据
					int fNum = Integer.parseInt(data.get("fNum").toString());
					pd.put("fNum", fNum + 1);
					pd.put("fid", data.get("fid"));
					pd.put("updateTime", new Date());
					functionService.updateDayACt(pd);
				}
				// ==============================================
			} else {
				result = "7003";
				retMessage = CommonMessage.CODE_7003;
			}
		}
		map.put("result", result);
		map.put("retMessage", retMessage);
		return AppUtil.returnObject(new PageData(), map);
	}

	/*******************************************
	 * @author 董雪蕊
	 * @description 患者看公告列表
	 * @date 2017年11月21日
	 */
	@RequestMapping(value = "/pGetArticleList2", method = RequestMethod.POST)
	@ResponseBody
	public Object pGetArticleList2() {
		logBefore(logger, "患者看公告列表--新 ");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();

		boolean international = false;// 国内版
		if (IsNull.paramsIsNull(pd.get("international")) == false) {
			if ("1".equals(pd.get("international").toString())) {
				international = true;// 国际版
			}
		}
		String result;
		String retMessage;
		if (international) {
			result = "10000";
			retMessage = "Connection abnormality";
		} else {
			result = "10000";
			retMessage = "连接异常";
		}

		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("pageSize")) || IsNull.paramsIsNull(pd.get("pageIndex"))
					|| IsNull.paramsIsNull(pd.get("searType"))) {
				if (international) {
					result = "9993";
					retMessage = "Parameter Anomaly";
				} else {
					result = "9993";
					retMessage = "参数异常";
				}
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {

				pd = PagingUtil.addPaging(pd);

				// 查看所有公告,按时间排序
				if (CommonConfig.IsOnline) {
					pd.put("isOnline", 1);
				}
				if (international) {
					// 国际
				} else {
					// 国内
					pd.put("international", "0");
				}
				List<PageData> articleList = articleService.listBytime(pd);
				if (articleList != null && articleList.size() > 0) {
					if (IsNull.paramsIsNull(pd.get("patientId")) == false) {
						for (int i = 0; i < articleList.size(); i++) {
							PageData pageData = articleList.get(i);
							// 点赞
							List<PageData> listBydz = articleService.listBydz(pageData);
							if (listBydz != null && listBydz.size() > 0) {
								pageData.put("dzList", listBydz);
							} else {
								pageData.put("dzList", new ArrayList<>());
							}

							// 点赞状态值
							pageData.put("dzStatus", "0");
							pageData.put("userId", pd.get("patientId"));
							PageData byDz = articleService.findByDz(pageData);
							if (IsNull.paramsIsNull(byDz) == false) {
								pageData.put("dzStatus", byDz.get("dzStatus"));
							}
							// 评论
							pageData.put("isAll", "0");
							pageData.put("patientId", pd.get("patientId"));

							List<PageData> listByComment = articleService.listByComment(pageData);
							if (listByComment != null && listByComment.size() > 0) {
								pageData.put("CommentList", listByComment);
							} else {
								pageData.put("CommentList", new ArrayList<>());
							}
						}
					}
					// 更新浏览量
					articleList = upCount(articleList, pd);
				}
				map.put("searType", 0);// 默认返回,临时保留
				// 返回查询信息
				if (articleList != null && articleList.size() > 0) {
					map.put("list", articleList);
					map.put("size", articleList.size());
				} else {
					map.put("list", new ArrayList<>());
					map.put("size", articleList.size());
				}

				if (international) {
					result = "0000";
					retMessage = "Success";
				} else {
					result = "0000";
					retMessage = "查询成功";
				}
				// ==============================================
				// 统计
				if (IsNull.paramsIsNull(pd.get("patientId")) == false) {
					pd.put("dayDate", DateUtil.getDay());
					pd.put("userId", pd.get("patientId"));
					pd.put("fType", "2");
					pd.put("fState", "查看动态");
					PageData data = functionService.getDayUser(pd);
					if (data == null) {
						// 不存在,添加
						pd.put("fNum", "1");
						pd.put("updateTime", new Date());
						functionService.save(pd);
					} else {
						// 存在,更新数据
						int fNum = Integer.parseInt(data.get("fNum").toString());
						pd.put("fNum", fNum + 1);
						pd.put("fid", data.get("fid"));
						pd.put("updateTime", new Date());
						functionService.updateDayACt(pd);
					}
				}
				// ==============================================
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (international) {
				result = "9999";
				retMessage = "System Exception";
			} else {
				result = "9999";
				retMessage = "系统异常";
			}
		} finally {
			map.put("retMessage", retMessage);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 更新浏览量
	 * 
	 */
	public List<PageData> upCount(List<PageData> list, PageData pd) throws Exception {
		List<PageData> List2 = new ArrayList<PageData>();
		int j = 0;
		for (int i = 0; i < list.size(); i++) {
			PageData pageData = list.get(i);

			if (CommonConfig.IsOnline) {
				if (IsNull.paramsIsNull(pageData.get("clinicId")) == false) {
					if ("101111f601d34b2cadf6da922ff60c7f".equals(pageData.get("clinicId").toString())) {
//						list.remove(i);
//						continue;
					} else {
						// 更新浏览量
						if (IsNull.paramsIsNull(pageData.get("total")) == false) {
							int count = Integer.parseInt(pageData.get("total").toString());
							pd.put("total", count + 1);
							pd.put("articleId", pageData.get("articleId"));
							articleService.updateTotal(pd);
						}
					}
				}
			} else {
				// 更新浏览量
				if (IsNull.paramsIsNull(pageData.get("total")) == false) {
					int count = Integer.parseInt(pageData.get("total").toString());
					pd.put("total", count + 1);
					pd.put("articleId", pageData.get("articleId"));
					articleService.updateTotal(pd);
				}
			}

			List2.add(j, pageData);
			if (j >= 9) {
				break;
			}
			j++;
		}
		return List2;
	}

}
