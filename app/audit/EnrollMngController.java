package com.fh.controller.app.audit;

import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import com.fh.controller.base.BaseController;
import com.fh.entity.Page;
import com.fh.service.app.ClinicService;
import com.fh.service.app.DoctorService;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 描述：注册信息
 * 董雪蕊
 * 2017-10-22
 * @author 
 */

@Controller
@RequestMapping(value = "/enrollMng")
public class EnrollMngController extends BaseController {

	@Resource
	private DoctorService doctorService;
	
	@Resource
	private ClinicService clinicService;
	
	
	/**
	 * 每日注册列表  展示：姓名   手机号   年龄  性别  诊所  日期
	 * 董雪蕊
	 * 2017-10-22
	 * @param page
	 * @return
	 * @throws Exception
	 *  
	 */
	@RequestMapping(value = "/todayEnrollList")
	public ModelAndView sum(Page page) throws Exception {
		logBefore(logger, "每日注册列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		if (IsNull.paramsIsNull(pd.get("searchDay"))) {
			pd.put("searchDay", DateUtil.getDay());
		}
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = doctorService.enrolllistPage(page); // 列出Pictures列表
		mv.setViewName("app/enroll/todayEnroll_list");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}
	
	/**
	 * 每日被标记诊所列表  
	 * 霍学杰
	 * 2017-11-13
	 * @param page
	 * @return
	 * @throws Exception
	 *  
	 */
	@RequestMapping(value = "/todayClinicList")
	public ModelAndView sum2(Page page) throws Exception {
		logBefore(logger, "每日被标记诊所列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		if (IsNull.paramsIsNull(pd.get("searchDay"))) {
			pd.put("starTime", DateUtil.getDay()+" 00:10:00");
			pd.put("endTime", DateUtil.getDay()+" 23:59:00");
			pd.put("searchDay", DateUtil.getDay());
		}else{
			pd.put("starTime", pd.get("searchDay")+" 00:10:00");
			pd.put("endTime", pd.get("searchDay")+" 23:59:00");
		}
		page.setPd(pd);
		// 查询列表
		List<PageData> varList =clinicService.DaylistPage(page); // 列出Pictures列表
		mv.setViewName("app/clinicBiao/todayEnroll_list");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}


}
