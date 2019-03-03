package com.fh.controller.app.office;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.controller.base.BaseController;
import com.fh.service.app.DoctorBigSortService;
import com.fh.service.app.OfficeService;
import com.fh.util.AppUtil;
import com.fh.util.PageData;



@Controller
@RequestMapping(value = "/api/office")
public class AppOfficeController   extends BaseController{

	@Resource
	private OfficeService officeService ;
	@Resource
	DoctorBigSortService doctorBigSortService;
	
	/**
	 * 
	 * @author 董雪蕊
	 * @description 得到科室的列表及ID
	 * @date 2017年12月27日
	 * @version 1.0
	 * @param pd
	 * @return 
	 */
	@RequestMapping(value = "/getOfficeList")
	@ResponseBody
	public Object name() {
		logBefore(logger, "得到科室的列表及ID");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			pd.put("international", "0");
			List<PageData> officeList = officeService.findList(pd);
			map.put("list", officeList);
			map.put("size", officeList.size());
			result = "0000";
			message = "成功";
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
