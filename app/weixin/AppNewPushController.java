package com.fh.controller.app.weixin;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fh.controller.base.BaseController;
import com.fh.service.app.NewPushService;
import com.fh.util.AppUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 董雪蕊 小程序段添加formId
 */

@Controller
@RequestMapping(value = "/api/newPush")
public class AppNewPushController extends BaseController {

	@Resource
	private NewPushService newPushService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 保存患者的formId
	 * @date 2017年12月27日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/saveFormId")
	@ResponseBody
	public Object name() {
		logBefore(logger, "保存患者的formId");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("userId")) || IsNull.paramsIsNull(pd.get("wxopenId"))
					|| IsNull.paramsIsNull(pd.get("formId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("state", 0);
				if(pd.get("formId").toString().length() < 15) {
					newPushService.save(pd);
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
}
