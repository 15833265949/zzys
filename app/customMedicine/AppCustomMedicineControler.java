package com.fh.controller.app.customMedicine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.controller.base.BaseController;
import com.fh.service.app.CustomMedicineService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 描述：自定义用药
 * 
 * @author 董雪蕊
 * @date 2018-1-5
 */

@Controller
@RequestMapping(value = "/api/customMedicine")
public class AppCustomMedicineControler extends BaseController {

	@Resource
	CustomMedicineService customMedicineService;
	/**
	 * 
	 * @author 董雪蕊
	 * @description 保存自定义用药
	 * @date 2018年1月5日
	 * @version 1.0
	 * @param pd
	 * @return 
	 */
	@RequestMapping(value = "/save",method=RequestMethod.POST)
	@ResponseBody
	public Object save() {
		logBefore(logger, "保存自定义用药");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			//第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId")) || IsNull.paramsIsNull(pd.get("isOpen"))
					|| IsNull.paramsIsNull(pd.get("alertTimeHm"))|| IsNull.paramsIsNull(pd.get("isEveryDay"))
					|| IsNull.paramsIsNull(pd.get("chongStr"))|| IsNull.paramsIsNull(pd.get("isMon"))
					|| IsNull.paramsIsNull(pd.get("isTues"))|| IsNull.paramsIsNull(pd.get("isWed"))
					|| IsNull.paramsIsNull(pd.get("isThur"))|| IsNull.paramsIsNull(pd.get("isFri"))
					|| IsNull.paramsIsNull(pd.get("isSat"))|| IsNull.paramsIsNull(pd.get("isSun"))
					|| IsNull.paramsIsNull(pd.get("remarks"))|| IsNull.paramsIsNull(pd.get("alertDate"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("createTime", DateUtil.getTime());
				pd.put("alertDate", DateUtil.stampToDate(pd.get("alertDate").toString()));
				customMedicineService.save(pd);
				
				int customMedicineId = Integer.parseInt(pd.get("customMedicineId").toString());
				map.put("customMedicineId", customMedicineId+"");
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
	 * @description 修改自定义用药 
	 * @date 2018年1月5日
	 * @version 1.0
	 * @param pd
	 * @return 
	 */
	@RequestMapping(value = "/editSet" ,method=RequestMethod.POST)
	@ResponseBody
	public Object editSet() {
		logBefore(logger, "修改自定义用药");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			//第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("customMedicineId")) || IsNull.paramsIsNull(pd.get("isOpen"))
					|| IsNull.paramsIsNull(pd.get("alertTimeHm"))|| IsNull.paramsIsNull(pd.get("isEveryDay"))
					|| IsNull.paramsIsNull(pd.get("chongStr"))|| IsNull.paramsIsNull(pd.get("isMon"))
					|| IsNull.paramsIsNull(pd.get("isTues"))|| IsNull.paramsIsNull(pd.get("isWed"))
					|| IsNull.paramsIsNull(pd.get("isThur"))|| IsNull.paramsIsNull(pd.get("isFri"))
					|| IsNull.paramsIsNull(pd.get("isSat"))|| IsNull.paramsIsNull(pd.get("isSun"))
					|| IsNull.paramsIsNull(pd.get("remarks"))|| IsNull.paramsIsNull(pd.get("alertDate"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("alertDate", DateUtil.stampToDate(pd.get("alertDate").toString()));
				customMedicineService.editSetter(pd);
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
	 * @description 得到患者的自定义用药列表
	 * @date 2018年1月5日
	 * @version 1.0
	 * @param pd
	 * @return 
	 */
	@RequestMapping(value = "/getCustList")
	@ResponseBody
	public Object getCustList() {
		logBefore(logger, "得到患者的自定义用药列表");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			//第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				List<PageData> list = customMedicineService.listById(pd);
				if (list!=null && list.size()>0) {
					map.put("size", list.size());
					map.put("list", list);
				}else {
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
	 * @description 删除自定义用药
	 * @date 2018年1月5日
	 * @version 1.0
	 * @param pd
	 * @return 
	 */
	@RequestMapping(value = "/delSet")
	@ResponseBody
	public Object delSet() {
		logBefore(logger, "删除自定义用药");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			//第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("customMedicineId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				customMedicineService.del(pd);
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
