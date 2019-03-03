package com.fh.controller.app.version;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.config.CommonMessage;
import com.fh.controller.base.BaseController;
import com.fh.service.app.VersionService;
import com.fh.util.AppUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 
 * Title:系统版本控制
 * 
 * @author 霍学杰
 * @date 20180531
 * @version 1.0
 */
@Controller
@RequestMapping(value = "/api/version")
public class AppVersionController extends BaseController {

	@Resource(name = "versionService")
	private VersionService versionService;

	/**
	 * 
	 * 描述：版本更新
	 * 
	 * @author 霍学杰
	 * @date 20180531
	 * @param page
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	@ResponseBody
	public Object version() {
		logBefore(logger, "安卓端版本更新");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData(); // 获取请求参数
		String result = "7001"; // 正常返回
		String retMessage = CommonMessage.CODE_7001;
		try {
			/** 1：必传参数不可为空 */
			if (IsNull.paramsIsNull(pd.get("version")) || IsNull.paramsIsNull(pd.get("appType"))) {
				result = "9993";
				retMessage = CommonMessage.CODE_9993;
			} else {
				/** 2:查询服务器版本表信息 */
				PageData versionNew = versionService.findVersion(pd);
				if (versionNew != null) {
					/** 3:拿到服务器的最新版本信息和客户端传来的版本号对比 */
					// 版本号可能是1.2.1.1，带小数点的不能直接转换成数值型（把.替换成""）
					String version = pd.get("version").toString();// 客户端参数
					version = version.replaceAll("\\.", "");
					int versionClient = Integer.parseInt(version);
					String version2 = versionNew.get("version").toString();// 服务器参数
					version2 = version2.replaceAll("\\.", "");
					int versions = Integer.parseInt(version2);
					/** 4：判断服务器版本是否大于客户端版本 */
					if (versionClient < versions) {
						/** 5:如果服务器版本大，则返回版本库中的下载地址给客户端 */
						map.put("db", versionNew);
						result = "7000";
						retMessage = CommonMessage.CODE_7000;
						/** 6:如果客户端版本不大于服务器版本则返回无需更新版本状态 */
					} else {
						result = "7001";
						retMessage = CommonMessage.CODE_7001;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			logger.error(e.getMessage());
		}
		map.put("result", result);
		map.put("retMessage", retMessage);
		logAfter(logger);
		return AppUtil.returnObject(new PageData(), map);
	}

}
