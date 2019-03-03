package com.fh.controller.app.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fh.controller.base.BaseController;
import com.fh.entity.Page;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.DrugrecordService;
import com.fh.util.DateUtil;
import com.fh.util.ObjectExcelView;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 描述：每日挂号信息
 * 霍学杰
 * 2017-10-24
 * @author 
 */

@Controller
@RequestMapping(value = "/ghaoerll")
public class GhaoController extends BaseController {

	@Resource
	private Clinic_checkinService clinic_checkinService;

	@Resource
	private DrugrecordService drugrecordService;
	
	
	/**
	 * 1.描述：每日挂号列表  展示： 诊所名称    患者姓名  手机号   症状  既往史    日期
	 * 霍学杰
	 * 2017-10-24
	 * @param page
	 * @return
	 * @throws Exception
	 *  
	 */
	@RequestMapping(value = "/todayGhaoList")
	public ModelAndView sum(Page page) throws Exception {
		logBefore(logger, "每日挂号列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		if (IsNull.paramsIsNull(pd.get("checkinTime"))) {
			pd.put("startime", DateUtil.getDay()+" 00:00:00");
			pd.put("endtime", DateUtil.getDay()+" 23:50:00");
		}else{
			pd.put("startime",pd.get("checkinTime").toString()+" 00:00:00");
			pd.put("endtime", pd.get("checkinTime").toString()+" 23:50:00");
		}
		pd.put("ss", 2);
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = clinic_checkinService.GhaolistPage(page); // 列出Pictures列表
		mv.setViewName("app/enroll/todayGhao_list");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}
	
	/**
	 * 导出挂号信息到EXCEL
	 * @author 霍学杰
	 * @return
	 */
	@RequestMapping(value = "/ghexcel")
	public ModelAndView ghexcel() {
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		try {
			Map<String, Object> dataMap = new HashMap<String, Object>();
			List<String> titles = new ArrayList<String>();

			titles.add("诊所名称"); // 1
			titles.add("患者名称"); // 2
			titles.add("症状"); // 3
			titles.add("既往史"); // 4
			titles.add("手机号"); // 5
			titles.add("接诊人"); // 6
			titles.add("初复诊"); // 7
			titles.add("时间"); // 8

			dataMap.put("titles", titles);
			List<PageData> userList = clinic_checkinService.Ghaolists(pd);
			List<PageData> varList = new ArrayList<PageData>();
			for (int i = 0; i < userList.size(); i++) {
				PageData vpd = new PageData();
				vpd.put("var1", userList.get(i).getString("clinicName")); // 1
				vpd.put("var2", userList.get(i).getString("patientName")); // 2
				vpd.put("var3", userList.get(i).getString("patientCondition")); // 4
				vpd.put("var4", userList.get(i).getString("patientMedicalHistory")); // 5
				vpd.put("var5", userList.get(i).getString("phone")); // 6
				vpd.put("var6", userList.get(i).getString("doctorName")); // 7	
				int state = Integer.parseInt(userList.get(i).get("patientVisit").toString());
				if(state==0){
					vpd.put("var7", "初诊");
				}else{
					vpd.put("var7", "复诊");
				}
			    vpd.put("var8", userList.get(i).getString("checkinTime"));

				varList.add(vpd);
			}
			dataMap.put("varList", varList);
			ObjectExcelView erv = new ObjectExcelView(); // 执行excel操作
			mv = new ModelAndView(erv, dataMap);

		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
		return mv;
	}

	
	
	

	/**
	 * 2.描述：每日接诊列表  展示： 诊所名称    患者姓名  手机号   症状  既往史    日期
	 * 霍学杰
	 * 2017-10-24
	 * @param page
	 * @return
	 * @throws Exception
	 *  
	 */
	@RequestMapping(value = "/todayJzhenList")
	public ModelAndView sum1(Page page) throws Exception {
		logBefore(logger, "每日挂号列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		if (IsNull.paramsIsNull(pd.get("createTime"))) {
			pd.put("startime", DateUtil.getDay()+" 00:00:00");
			pd.put("endtime", DateUtil.getDay()+" 23:50:00");
		}else{
			pd.put("startime",pd.get("createTime").toString()+" 00:00:00");
			pd.put("endtime", pd.get("createTime").toString()+" 23:50:00");
		}
		pd.put("ss", 1);
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = clinic_checkinService.GhaolistPage(page); // 列出Pictures列表
		mv.setViewName("app/enroll/todayJzhen_list");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}

	/**
	 * 3.描述：每日药库列表  展示： 诊所名称    患者姓名  手机号   症状  既往史    日期
	 * 霍学杰
	 * 2017-10-24
	 * @param page
	 * @return
	 * @throws Exception
	 *  
	 */
	@RequestMapping(value = "/todayYkuList")
	public ModelAndView sum2(Page page) throws Exception {
		logBefore(logger, "每日挂号列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		if (IsNull.paramsIsNull(pd.get("createTime"))) {
			pd.put("startime", DateUtil.getDay()+" 00:00:00");
			pd.put("endtime", DateUtil.getDay()+" 23:50:00");
		}else{
			pd.put("startime",pd.get("createTime").toString()+" 00:00:00");
			pd.put("endtime", pd.get("createTime").toString()+" 23:50:00");
		}
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = drugrecordService.YkulistPage(page); // 列出Pictures列表
		mv.setViewName("app/enroll/todayYku_list");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}
	
	/**
	 * 导出信息到EXCEL
	 * @author 霍学杰
	 * @return
	 */
	@RequestMapping(value = "/excel")
	public ModelAndView exportExcel() {
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		try {
			Map<String, Object> dataMap = new HashMap<String, Object>();
			List<String> titles = new ArrayList<String>();

			titles.add("诊所名称"); // 1
			titles.add("药品名称"); // 2
			titles.add("出入库"); // 3
			titles.add("操作人"); // 4
			titles.add("诊所状态"); // 5
			titles.add("时间"); // 6
			titles.add("药品分类"); // 7
			titles.add("数量"); // 8

			dataMap.put("titles", titles);
			pd.put("state", 1);
			List<PageData> userList = drugrecordService.Ykulists(pd);
			List<PageData> varList = new ArrayList<PageData>();
			for (int i = 0; i < userList.size(); i++) {
				PageData vpd = new PageData();
				vpd.put("var1", userList.get(i).getString("clinicName")); // 1
				vpd.put("var2", userList.get(i).getString("sysdrugName")); // 2
				int state = Integer.parseInt(userList.get(i).getString("flag"));
				if(state==1){
					vpd.put("var3", "入库");
				}else{
					vpd.put("var3", "出库");
				}
				vpd.put("var4", userList.get(i).getString("outDoctorName")); // 4
				vpd.put("var5", userList.get(i).getString("clinicFlag")); // 5
				vpd.put("var6", userList.get(i).getString("createTime")); // 6
				vpd.put("var7", userList.get(i).getString("goodsType")); // 7		
			    vpd.put("var8", userList.get(i).getString("num"));

				varList.add(vpd);
			}
			dataMap.put("varList", varList);
			ObjectExcelView erv = new ObjectExcelView(); // 执行excel操作
			mv = new ModelAndView(erv, dataMap);

		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
		return mv;
	}


}
