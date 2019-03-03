package com.fh.controller.app.audit;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fh.controller.base.BaseController;
import com.fh.entity.Page;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.MyUserService;
import com.fh.util.ObjectExcelView;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;


/**
 * 描述：审核诊所
 * 
 * @author 霍学杰 2017.10.22
 */

@Controller
@RequestMapping(value = "/recvClinic")
public class RecvClinicController extends BaseController {

	@Resource(name = "MyUserService")
	private MyUserService MyUserService;

	@Resource
	private DoctorService doctorService;

	@Resource
	private ClinicUserService clinicUserService;

	@Resource
	private ClinicService clinicService;

	@Resource
	private MonthCountService monthCountService;

	@Resource
	private DayCountService dayCountService;
	@Resource
	private Clinic_checkinService clinic_checkinService;

	/**
	 * 1.描述：展示审核诊所列表 霍学杰
	 * 
	 * @param page
	 * @return
	 * @throws Exception
	 *             2017.10.22
	 */
	@SuppressWarnings("unused")
	@RequestMapping(value = "/validate")
	@ResponseBody
	public List<PageData> findBranchByBrandName(HttpServletResponse response) throws Exception {

		PageData pd = new PageData();
		pd = this.getPageData();
		int size = 10;
		pd.put("size", size);

		List<PageData> varList = null;
		List<PageData> List = new ArrayList<PageData>();
		if (IsNull.paramsIsNull(pd.get("latitude")) == false) {
			varList = clinicService.findRecClinic(pd);
		}

		PageData dd = new PageData();
		int num = 0;
		if (varList.size() > 40) {
			num = 40;
		}
		int flag = 0;
		String isSham ="0";
		for (int i = 0; i < num; i++) {
			dd = varList.get(i);
			flag = Integer.parseInt(dd.get("isSham").toString());
			if (flag == 0) {
				dd.put("state", "未修改标记");
			} else {
				dd.put("state", "修改标记");
			}
//			isSham= dd.get("isSham").toString();
//			if (isSham =="0") {
//				dd.put("isSham", "未改");
//			} else {
//				dd.put("isSham", "已改为标记");
//			}
			List.add(i, dd);
		}
		return List;
	}

	/**
	 * 2.描述：审核 诊所地图
	 * 
	 * @author 霍学杰
	 * @param doctorId
	 * @date 2017.9.3
	 */
	@RequestMapping(value = "/goditu")
	public ModelAndView sum(Page page) throws Exception {
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		pd.put("longitude", 114.450837);
		pd.put("latitude", 38.073638);

		List<PageData> varList = clinicService.findRecClinic(pd);
		List<PageData> List = new ArrayList<PageData>();
		PageData dd = new PageData();
		int num = 0;
		if (varList.size() > 40) {
			num = 15;
		}
		int flag = 0;
		for (int i = 0; i < num; i++) {
			dd = varList.get(i);
			flag = Integer.parseInt(dd.get("isSham").toString());
			if (flag == 0) {
				dd.put("state", "未修改标记");
			} else {
				dd.put("state", "已修改标记");
			}
			List.add(i, dd);
		}
		mv.setViewName("app/review/Revclinic");
		mv.addObject("varList", List);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}

	/**
	 * 3.描述：审核通过 霍学杰
	 * 
	 * @param doctorId
	 *            clinicId 2017.10.23
	 */
	@RequestMapping(value = "/adopt")
	public void adopt(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			PageData clinic = clinicService.findzsClinic(pd);// 查询诊所
			if (clinic != null) {
				pd.put("state", 4);
				clinicService.updateClinic(pd);
			}
			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

	@RequestMapping(value = "/adopt2")
	public void adopt2(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();

			if (IsNull.paramsIsNull(pd.get("clinicId")) == false) {

				String[] split = pd.get("clinicId").toString().split(",");
				if (split != null) {
					for (int i = 0; i < split.length; i++) {
						pd.put("state", 4);
						pd.put("clinicId", split[i]);
						clinicService.updateClinic(pd);
					}
				}
			}
			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

	/**
	 * 4.描述：审核不通过 霍学杰
	 * 
	 * @param doctorId
	 *            clinicId 2017.10.23
	 */
	@RequestMapping(value = "/noAdopt")
	public void noAdopt(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			PageData clinic = clinicService.findzsClinic(pd);// 查询诊所
			if (clinic != null) {
				pd.put("state", 3);
				clinicService.updateClinic(pd);
			}
			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

	@RequestMapping(value = "/noAdopt2")
	public void noAdopt2(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();

			if (IsNull.paramsIsNull(pd.get("clinicId")) == false) {

				String[] split = pd.get("clinicId").toString().split(",");
				if (split != null) {
					for (int i = 0; i < split.length; i++) {
						pd.put("state", 3);
						pd.put("clinicId", split[i]);
						clinicService.updateClinic(pd);
					}
				}
			}
			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

	/**
	 * 5.描述：导出 霍学杰
	 * 
	 * @param doctorId
	 *            clinicId 2017.10.24
	 */
	@RequestMapping(value = "/expolExcel")
	public ModelAndView expolExcel(PrintWriter out) {
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			
			if (IsNull.paramsIsNull(pd.get("clinicId")) == false) {
				Map<String, Object> dataMap = new HashMap<String, Object>();
				List<String> titles = new ArrayList<String>();

				titles.add("编号"); // 1
				titles.add("诊所名称"); // 2
				titles.add("电话"); // 3
				titles.add("市"); // 4
				titles.add("区县"); // 5
				titles.add("地址"); // 6
			

				dataMap.put("titles", titles);

				List<PageData> varList = new ArrayList<PageData>();
				
				String[] split = pd.get("clinicId").toString().split(",");
				if (split != null) {
					for (int i = 0; i < split.length; i++) {
					
						pd.put("clinicId", split[i]);
						PageData findzsClinic = clinicService.findzsClinic(pd);						
						PageData vpd = new PageData();
						vpd.put("var1", i+1); // 1
						vpd.put("var2", findzsClinic.getString("clinicName")); // 2
						vpd.put("var3", findzsClinic.getString("clinicphone")); // 3
						vpd.put("var4", findzsClinic.getString("cityname")); // 4
						vpd.put("var5", findzsClinic.getString("adname")); // 5
						vpd.put("var6", findzsClinic.getString("clinicAddress")); // 6
						varList.add(vpd);
					}
				}
				dataMap.put("varList", varList);
				ObjectExcelView erv = new ObjectExcelView(); // 执行excel操作
				mv=new ModelAndView(erv, dataMap);

			}

			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
		return mv;

	}
/******************************以下为重写通过、不通过，，因为做的假数据不能和真数据混了*********************************************************/
	/**
	 * 3.描述：审核通过 霍学杰
	 * 
	 * @param doctorId
	 *            clinicId 2017.10.23
	 */
	@RequestMapping(value = "/adopt_D")
	public void adopt_D(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			PageData clinic = clinicService.findzsClinic(pd);// 查询诊所
			if (clinic != null) {
				pd.put("isSham", "1");
				clinicService.updateClinic(pd);
			}
			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

	@RequestMapping(value = "/adopt2_D")
	public void adopt2_D(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();

			if (IsNull.paramsIsNull(pd.get("clinicId")) == false) {

				String[] split = pd.get("clinicId").toString().split(",");
				if (split != null) {
					for (int i = 0; i < split.length; i++) {
						pd.put("isSham", "1");
						pd.put("clinicId", split[i]);
						clinicService.updateClinic(pd);
					}
				}
			}
			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

	/**
	 * 4.描述：审核不通过 霍学杰
	 * 
	 * @param doctorId
	 *            clinicId 2017.10.23
	 */
	@RequestMapping(value = "/noAdopt_D")
	public void noAdopt_D(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			PageData clinic = clinicService.findzsClinic(pd);// 查询诊所
			if (clinic != null && Integer.parseInt(clinic.get("state").toString())==3) {
				clinic_checkinService.delSham(pd);
				pd.put("isSham", "0");
				clinicService.updateClinic(pd);
			}
			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}
	@RequestMapping(value = "/noAdopt2_D")
	public void noAdopt2_D(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();

			if (IsNull.paramsIsNull(pd.get("clinicId")) == false) {

				String[] split = pd.get("clinicId").toString().split(",");
				if (split != null) {
					for (int i = 0; i < split.length; i++) {
						pd.put("isSham", "0");
						pd.put("clinicId", split[i]);
						PageData clinic = clinicService.findzsClinic(pd);// 查询诊所
						if (clinic != null && Integer.parseInt(clinic.get("state").toString())==3) {
							clinic_checkinService.delSham(pd);
							pd.put("isSham", "0");
							clinicService.updateClinic(pd);
						}
						
					}
				}
			}
			out.write("success");
			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}
}
