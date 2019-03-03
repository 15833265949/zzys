package com.fh.controller.app.news;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fh.controller.base.BaseController;
import com.fh.service.app.NewsService;
import com.fh.util.AppUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 描述：消息类
 * 
 * @author 董雪蕊
 * @date 2017.10.16 版本：1.0
 */

@Controller
@RequestMapping(value = "/api/news")
public class NewsControler extends BaseController {

	@Resource
	private NewsService newsService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 消息记录接口
	 * @date 2017年11月5日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/newsList")
	@ResponseBody
	public Object newsList() {
		logBefore(logger, "消息记录");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				List<PageData> list = newsService.list(pd);
				if (list != null && list.size() > 0) {
					map.put("size", list.size());
					map.put("list", list);
				} else {
					map.put("size", 0);
					map.put("list", new ArrayList<>());
				}

				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 消息列表-点进去查询互动、系统消息
	 * @date 2017年11月5日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/newsListIn")
	@ResponseBody
	public Object newsListIn() {
		logBefore(logger, "消息列表-点进去查询互动、系统消息");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("pageSize"))
					|| IsNull.paramsIsNull(pd.get("pageIndex")) || IsNull.paramsIsNull(pd.get("searchFlag"))) {
				result = "9993";
				message = "参数异常";
			} else {
				int pageSize = Integer.parseInt(pd.get("pageSize").toString());
				int pageIndex = Integer.parseInt(pd.get("pageIndex").toString());
				int min = pageIndex * pageSize - pageSize;
				pd.put("min", min);
				pd.put("max", pageSize);
				List<PageData> list = newsService.listIn(pd);
				if (list != null && list.size() > 0) {
					map.put("size", list.size());
					map.put("list", list);
				} else {
					map.put("size", 0);
					map.put("list", new ArrayList<>());
				}
				if (pd.get("searchFlag").toString().equals("1") || pd.get("searchFlag").toString().equals("2")
						|| pd.get("searchFlag").toString().equals("3")) {
					newsService.updateFlag3(pd);
				}

				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 将消息改为已读
	 * @date 2017年11月12日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/editRead")
	@ResponseBody
	public Object editRead() {
		logBefore(logger, "将消息改为已读");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("newId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("state", 1);
				newsService.updateFlag(pd);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 查询是否有未读消息
	 * @date 2017年11月12日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/isHaveR")
	@ResponseBody
	public Object isHaveR() {
		logBefore(logger, "查询是否有未读消息 ");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData data = newsService.isHaveR(pd);
				boolean isHave = true;
				if (IsNull.paramsIsNull(data)) {
					isHave = false;
				}
				map.put("isHave", isHave);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 患者得到我的消息列表
	 * @date 2017年11月23日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/patGetList")
	@ResponseBody
	public Object patGetList() {
		logBefore(logger, "患者得到我的消息列表");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId")) || IsNull.paramsIsNull(pd.get("pageSize"))
					|| IsNull.paramsIsNull(pd.get("pageIndex"))) {
				result = "9993";
				message = "参数异常";
			} else {
				int pageSize = Integer.parseInt(pd.get("pageSize").toString());
				int pageIndex = Integer.parseInt(pd.get("pageIndex").toString());
				int min = pageIndex * pageSize - pageSize;
				pd.put("min", min);
				pd.put("max", pageSize);
				pd.put("toUserId", pd.get("patientId"));
				List<PageData> list = newsService.patGetList(pd);
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
				} else {
					map.put("list", new ArrayList<>());
					map.put("size", list.size());
				}
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 将患者消息改为已读
	 * @date 2017年11月12日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/editReadAll")
	@ResponseBody
	public Object editReadAll() {
		logBefore(logger, "将患者消息改为已读");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("newId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("state", 1);
				newsService.updateFlag(pd);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 查询是否有未读消息
	 * @date 2017年11月12日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/isHaveRP")
	@ResponseBody
	public Object isHaveRP() {
		logBefore(logger, "查询是否有未读消息 ");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("toUserId", pd.get("patientId"));
				PageData data = newsService.isHaveRP(pd);
				if (IsNull.paramsIsNull(data)) {
					data = new PageData();
				}
				map.put("db", data);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 批量删除接口
	 * @date 2018年1月20日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/batchDel")
	@ResponseBody
	public Object batchDel() {
		logBefore(logger, "批量删除接口");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("toUserId")) || IsNull.paramsIsNull(pd.get("newIdS"))) {
				result = "9993";
				message = "参数异常";
			} else {
				String newIdArray[] = pd.get("newIdS").toString().split(",");
				pd.put("newIdArray", newIdArray);
				newsService.batchDel(pd);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 批量已读接口
	 * @date 2018年1月22日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/batchRead")
	@ResponseBody
	public Object batchRead() {
		logBefore(logger, "批量已读接口");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("toUserId")) || IsNull.paramsIsNull(pd.get("newIdS"))) {
				result = "9993";
				message = "参数异常";
			} else {
				String newIdArray[] = pd.get("newIdS").toString().split(",");
				pd.put("newIdArray", newIdArray);
				pd.put("state", 1);
				newsService.batchRead(pd);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 将患者消息改为已读
	 * @date 2018年1月23日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/editReadAllDoc")
	@ResponseBody
	public Object editReadAllDoc() {
		logBefore(logger, "将患者消息改为已读");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("toUserId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("state", 1);
				newsService.updateAll(pd);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 将消息全部删除(根据类型)
	 * @date 2018年1月23日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/delAllDoc")
	@ResponseBody
	public Object delAllDoc() {
		logBefore(logger, "将消息全部删除");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("toUserId")) || IsNull.paramsIsNull(pd.get("searchFlag"))) {
				result = "9993";
				message = "参数异常";
			} else {
				newsService.delAll(pd);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 查询是否有未读消息
	 * @date 2018年1月23日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/isHaveNoRead")
	@ResponseBody
	public Object isHaveNoRead() {
		logBefore(logger, "查询是否有未读消息 ");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("toUserId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData retData = new PageData();
				// 查互动消息
				pd.put("searchFlag", 1);
				PageData data = newsService.isHaveNoRead(pd);
				if (IsNull.paramsIsNull(data)) {
					retData.put("flag1", false);
				} else {
					retData.put("flag1", true);
				}
				// 查系统消息
				pd.put("searchFlag", 2);
				PageData data2 = newsService.isHaveNoRead(pd);
				if (IsNull.paramsIsNull(data2)) {
					retData.put("flag2", false);
				} else {
					retData.put("flag2", true);
				}
				// 查添加消息
				pd.put("searchFlag", 3);
				PageData data3 = newsService.isHaveNoRead(pd);
				if (IsNull.paramsIsNull(data3)) {
					retData.put("flag3", false);
				} else {
					retData.put("flag3", true);
				}
				// 查互动消息
				pd.put("searchFlag", 4);
				PageData data4 = newsService.isHaveNoRead(pd);
				if (IsNull.paramsIsNull(data4)) {
					retData.put("flag4", false);
				} else {
					retData.put("flag4", true);
				}
				map.put("db", retData);
				result = "0000";
				message = "成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}
}
