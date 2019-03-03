package com.fh.controller.app.crashLog;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import com.fh.controller.base.BaseController;
import com.fh.service.app.CrashLogService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;

/**
 * 描述：系统崩溃日志处理
 * 
 * @author 霍学杰 修改
 * @date 20180531
 */

@Controller
@RequestMapping(value = "/api/crash")
public class AppCrashLogControler extends BaseController implements HandlerExceptionResolver {

	@Resource
	private CrashLogService crashLogService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 保存崩溃日志
	 * @date 2017年11月26日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/saveCrashLog", method = RequestMethod.POST)
	@ResponseBody
	public Object saveCrashLog() {
		logBefore(logger, "保存崩溃日志 ");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			crashLogService.save(pd);
			result = "0000";
			retMessage = "成功";

		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			retMessage = "系统异常";
		} finally {
			map.put("retMessage", retMessage);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	@Override
	public ModelAndView resolveException(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2,
			Exception arg3) {
		// 添加自己的异常处理逻辑，如日志记录等
		logBefore(logger, "服务器端异常处理 ");
		PageData pd = new PageData();
		pd = this.getPageData();
		try {
			pd.put("clinicId", "");
			pd.put("doctorId", "");
			pd.put("logType", "5");// 服务器端错误
			pd.put("modVerion", "知心诊所服务端");// 服务器端错误
			pd.put("content", arg3.toString());
			pd.put("createTime", DateUtil.getTime());
			pd.put("version", "");
			pd.put("shuoMing", "");
			crashLogService.save(pd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
