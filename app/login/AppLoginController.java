package com.fh.controller.app.login;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.controller.base.BaseController;
import com.fh.service.app.DoctorService;
import com.fh.service.app.LoginLogService;
import com.fh.service.app.PatientService;
import com.fh.util.ditu.DituUtil;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 登录日志处理类
 */

@Controller
@RequestMapping(value = "/api/login")
public class AppLoginController extends BaseController {

	@Resource(name = "loginLogService")
	private LoginLogService loginLogService;

	@Resource
	private PatientService patientService;
	@Resource
	private DoctorService doctorService;

	/**
	 * 
	 * @Description: 添加登陆日志统计
	 * @author: 王立飞
	 * @date: 2018年11月8日 上午10:34:57
	 *
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Object save() {
		logBefore(logger, "添加登陆日志统计");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("loginId")) || IsNull.paramsIsNull(pd.get("appType"))
					|| IsNull.paramsIsNull(pd.get("loginType"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("IP", getRemortIP());
				// 登录日志
				pd.put("loginTime", DateUtil.getTime());
				pd = putPd(pd);
				loginLogService.save(pd);
				pd.put("state", pd.get("loginType"));// 1安卓 2 IOS 3小程序
				pd.put("adname", pd.get("area"));// 区域
				// 更新用户表信息
				if ("1".equals(pd.get("appType").toString())) {
					// 医生端
					pd.put("doctorId", pd.get("loginId"));
					doctorService.editAdress(pd);
				} else {
					// 患者端
					pd.put("patientId", pd.get("loginId"));
					patientService.editAdress(pd);
				}
				result = "0000";
				message = "成功";
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
	 * 将登录的所有信息封装下
	 * 
	 * @return
	 */
	public PageData putPd(PageData pd) {

		pd.put("province", "");
		pd.put("city", "");
		pd.put("area", "");
		pd.put("detailAddress", "");

		if (IsNull.paramsIsNull(pd.get("IP"))) {
			pd.put("IP", "");
		} else {
			String[] getprovince = DituUtil.getprovince(pd.getString("IP"));
			if (IsNull.paramsIsNull(getprovince[0]) == false) {
				pd.put("province", getprovince[0]);
			}
			if (IsNull.paramsIsNull(getprovince[1]) == false) {
				pd.put("city", getprovince[1]);
			}
		}
		// 添加地址
		if (IsNull.paramsIsNull(pd.get("longitude")) == false && IsNull.paramsIsNull(pd.get("latitude")) == false) {
			String location = pd.get("longitude").toString() + "," + pd.get("latitude").toString();
			PageData data = DituUtil.GPSToaddress(location);
			pd.putAll(data);
		} else {
			pd.put("longitude", "");
			pd.put("latitude", "");
		}
		return pd;
	}

	/**
	 * 获取登录用户的IP
	 * 
	 * @throws Exception
	 */
	public String getRemortIP() throws Exception {
		HttpServletRequest request = this.getRequest();
		String ip = "";
		if (request.getHeader("x-forwarded-for") == null) {
			ip = request.getRemoteAddr();
		} else {
			ip = request.getHeader("x-forwarded-for");
		}
		System.out.println(ip);
		return ip;
	}

}
