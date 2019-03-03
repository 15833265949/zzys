package com.fh.controller.app.audit;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import com.fh.controller.base.BaseController;
import com.fh.entity.Page;
import com.fh.service.app.ClinicService;
import com.fh.service.app.RegionService;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 描述：每日数据查询处理
 * 
 * @author 霍学杰 2017.9.2
 */

@Controller
@RequestMapping(value = "/clinicMng")
public class ClinicMngController extends BaseController {

	@Resource
	private ClinicService clinicService;
	@Resource
	private RegionService regionService;


	/**
	 * 每日标记诊所信息  展示：姓名   电话   诊所名称  诊所地址  日期  加上三级联动，查询省市区
	 * 董雪蕊
	 * 2017-10-22
	 * @param page
	 * @return
	 * @throws Exception
	 *  
	 */
	@RequestMapping(value = "/todayBiaoList")
	public ModelAndView sum(Page page) throws Exception {
		logBefore(logger, "每日标记诊所信息");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		if (IsNull.paramsIsNull(pd.get("searchDay"))) {
			pd.put("searchDay", DateUtil.getDay());
		}
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = clinicService.biaolistPage(page); // 列出今日标记的诊所列表
		List<PageData> provinceList = regionService.provinceList(pd); // 列出今日标记的诊所列表
		mv.setViewName("app/clinicBiao/todayClinicBiao_list");
		mv.addObject("varList", varList);
		mv.addObject("provinceList", provinceList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}
	
	/**
	 * 得到下属城市或区 HttpServletResponse response
	 */
	@SuppressWarnings("rawtypes")
	@RequestMapping(value="/getCityOrQu")
	@ResponseBody
	public List searchList()throws Exception{
		logBefore(logger, "得到下属城市或区");
		PageData pd = new PageData();
		pd=this.getPageData();
		List list =new ArrayList<>();
		try{
			list=regionService.cityOrQuList(pd);
		} catch(Exception e){
			logger.error(e.toString(), e);
		}
		return list;
	}
}
