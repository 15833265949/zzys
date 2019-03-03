package com.fh.controller.app.friend;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.controller.base.BaseController;
import com.fh.service.app.FriendsService;
import com.fh.service.statistics.TjFunctionService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 描述：好友类
 * 
 * @author 董雪蕊
 * @date 2017.10.16 版本：1.0
 */

@Controller
@RequestMapping(value = "/api/friends")
public class FriendsControler extends BaseController {

	@Resource
	private FriendsService friendsService;
	@Resource
	private TjFunctionService functionService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 得到我的医生列表与搜索
	 * @date 2017年11月22日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/myDocList", method = RequestMethod.POST)
	@ResponseBody
	public Object myDocList() {
		logBefore(logger, "得到我的医生列表与搜索");
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
			if (IsNull.paramsIsNull(pd.get("patientId"))) {
				if (international) {
					result = "9993";
					retMessage = "Parameter Anomaly";
				} else {
					result = "9993";
					retMessage = "参数异常";
				}
			} else {
				pd.put("toUserId", pd.get("patientId"));
				// 查询我的医生
				List<PageData> list = friendsService.myDocList(pd);
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
				} else {
					map.put("list", new ArrayList<>());
					map.put("size", 0);
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
				if (IsNull.paramsIsNull(pd.get("SearchCont")) == false) {
					pd.put("dayDate", DateUtil.getDay());
					pd.put("userId", pd.get("patientId"));
					pd.put("fType", "2");
					pd.put("fState", "搜索我的医生");
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

}
