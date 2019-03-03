package com.fh.controller.app.clinic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ArticleService;
import com.fh.service.app.ClinicService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.CrashLogService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.DrugOutRecordService;
import com.fh.service.app.DrugclinicService;
import com.fh.service.app.DrugrecordService;
import com.fh.service.app.MonthCountService;
import com.fh.service.back.RecheckService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 作者：霍学杰 类：诊所处理类
 */

@Controller
@RequestMapping(value = "/api/clinic")
public class AppClinicController extends BaseController {

	@Resource
	private ClinicService clinicService;
	@Resource
	private DoctorService doctorService;
	@Resource
	private ArticleService articleService;
	@Resource
	private MonthCountService monthCountService;
	@Resource
	private DrugclinicService drugclinicService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private DrugOutRecordService drugOutRecordService;
	@Resource
	private DrugrecordService drugrecordService;
	@Resource
	private DayCountService dayCountService;
	@Resource
	private CrashLogService crashLogService;
	@Resource
	private RecheckService recheckService;

	/**
	 * 7.描述：诊所端 设置诊所营业时间
	 * 
	 * @auther 霍学杰
	 * @date 2017年11月2日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/setOpenHours", method = RequestMethod.POST)
	@ResponseBody
	public Object setOpenHours() {
		logBefore(logger, "诊所端 设置诊所营业时间");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 判断参数 为不为空
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.remove("state");
				clinicService.editClinic(pd);// 完善诊所信息
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
	 * 
	 * @author 董雪蕊
	 * @description 修改诊所主治方向
	 * @date 2017年11月6日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/editMajor")
	@ResponseBody
	public Object name() {
		logBefore(logger, "修改诊所主治方向");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("majorDirection"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				pd.remove("state");
				clinicService.editClinic(pd);
				result = "0000";
				retMessage = "成功";
			}
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

	/**
	 * 
	 * @author 董雪蕊
	 * @description 得到诊所的预约时间（明后两天的）
	 * @date 2017年12月21日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/getCliYuTime")
	@ResponseBody
	public Object getCliYuTime() {
		logBefore(logger, "得到诊所的预约时间（明后两天的）");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				String tomorrow = DateUtil.getAfterDayDate("1");
				String tomorrowAfter = DateUtil.getAfterDayDate("2");
				Boolean isTomWo = DateUtil.getTimeIsWork(tomorrow);// 明天是否工作日
				Boolean isTomWoAf = DateUtil.getTimeIsWork(tomorrowAfter);// 后台是否是工作日
				List<String> tomList = new ArrayList<String>();
				List<String> tomAftList = new ArrayList<String>();
				PageData clinData = clinicService.findzsClinic(pd);
				if (IsNull.paramsIsNull(clinData) == false) {
					if (isTomWo) {// 明天是工作日
						if (Integer.parseInt(clinData.get("openState").toString()) == 0) {// 未开启，按全部时间
							tomList = DateUtil.getTenInternalTime("00:00", "23:41");
						} else {
							String nmOpenS[] = clinData.get("nmOpen").toString().split("-");
							String pmOpenS[] = clinData.get("pmOpen").toString().split("-");
							String amOpenS[] = clinData.get("amOpen").toString().split("-");
							tomList.addAll(DateUtil.getTenInternalTime(nmOpenS[0], nmOpenS[1]));
							tomList.addAll(DateUtil.getTenInternalTime(pmOpenS[0], pmOpenS[1]));
							tomList.addAll(DateUtil.getTenInternalTime(amOpenS[0], amOpenS[1]));
						}
					} else {
						if (Integer.parseInt(clinData.get("jhOpenState").toString()) == 0) {// 未开启，按全部时间
							tomList = DateUtil.getTenInternalTime("00:00", "23:41");
						} else {
							String jhNopenS[] = clinData.get("jhNopen").toString().split("-");
							String jhPopenS[] = clinData.get("jhPopen").toString().split("-");
							String jhAopenS[] = clinData.get("jhAopen").toString().split("-");
							tomList.addAll(DateUtil.getTenInternalTime(jhNopenS[0], jhNopenS[1]));
							tomList.addAll(DateUtil.getTenInternalTime(jhPopenS[0], jhPopenS[1]));
							tomList.addAll(DateUtil.getTenInternalTime(jhAopenS[0], jhAopenS[1]));
						}
					}
					if (isTomWoAf) {// 后天是工作日
						if (Integer.parseInt(clinData.get("openState").toString()) == 0) {// 未开启，按全部时间
							tomAftList = DateUtil.getTenInternalTime("00:00", "23:41");
						} else {
							String nmOpenS[] = clinData.get("nmOpen").toString().split("-");
							String pmOpenS[] = clinData.get("pmOpen").toString().split("-");
							String amOpenS[] = clinData.get("amOpen").toString().split("-");
							tomAftList.addAll(DateUtil.getTenInternalTime(nmOpenS[0], nmOpenS[1]));
							tomAftList.addAll(DateUtil.getTenInternalTime(pmOpenS[0], pmOpenS[1]));
							tomAftList.addAll(DateUtil.getTenInternalTime(amOpenS[0], amOpenS[1]));
						}
					} else {
						if (Integer.parseInt(clinData.get("jhOpenState").toString()) == 0) {// 未开启，按全部时间
							tomAftList = DateUtil.getTenInternalTime("00:00", "23:41");
						} else {
							String jhNopenS[] = clinData.get("jhNopen").toString().split("-");
							String jhPopenS[] = clinData.get("jhPopen").toString().split("-");
							String jhAopenS[] = clinData.get("jhAopen").toString().split("-");
							tomAftList.addAll(DateUtil.getTenInternalTime(jhNopenS[0], jhNopenS[1]));
							tomAftList.addAll(DateUtil.getTenInternalTime(jhPopenS[0], jhPopenS[1]));
							tomAftList.addAll(DateUtil.getTenInternalTime(jhAopenS[0], jhAopenS[1]));
						}
					}

					// 之后将得到的list<String> 换格式
					map.put("tomList", liToMap(tomList));
					map.put("tomAftList", liToMap(tomAftList));
				}
				// map.put("db", userData);
				result = "0000";
				retMessage = "成功";
			}
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

	/**
	 * 将获取的list 转为map格式为
	 * 
	 * "tomList":{ "01":[ "10", "20", "30", ], "02":[ "10", "20", "30", ] }
	 * 
	 * @param list
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Set> liToMap(List<String> list) {
		Map<String, Set> retMap = new HashMap<>();
		for (int i = 0; i < list.size(); i++) {
			String HHmmS[] = list.get(i).split(":");// 将时间分为 【时 分】
			if (IsNull.paramsIsNull(retMap.get(HHmmS[0]))) {// 如果map里没有，就加入
				Set<String> setmm = new TreeSet<>();
				setmm.add(HHmmS[1]);
				retMap.put(HHmmS[0], setmm);
			} else {
				Set<String> setmm = retMap.get(HHmmS[0]);
				setmm.add(HHmmS[1]);
				retMap.put(HHmmS[0], setmm);
			}
		}
		return retMap;
	}

}
