package com.fh.controller.app.doctor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ClinicCheckinDetailService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.statistics.TjFunctionService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PagingUtil;

/**
 * 描述：接诊流程 --新版--第三版---患者端
 * 
 * @author 董雪蕊
 * @date 2018-1-15
 */

@Controller
@RequestMapping(value = "/api/chargePati")
public class AppChargePatiController extends BaseController {

	@Resource
	private TjFunctionService functionService;
	@Resource
	Clinic_checkinService clinic_checkinService;
	@Resource
	ClinicCheckinDetailService clinicCheckinDetailService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 根据诊所名称 患者端查询就诊记录
	 * @date 2018年1月20日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/listHistByCliNam")
	@ResponseBody
	public Object listHistByCliNam() {
		logBefore(logger, "根据诊所名称  患者端查询就诊记录");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId")) || IsNull.paramsIsNull(pd.get("pageSize"))
					|| IsNull.paramsIsNull(pd.get("pageIndex")) || IsNull.paramsIsNull(pd.get("inputContent"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd = PagingUtil.addPaging(pd);
				// 查询挂号所有的数据
				List<PageData> histList = clinic_checkinService.listHistPati(pd);
				if (histList != null && histList.size() > 0) {
					for (int i = 0; i < histList.size(); i++) {
						PageData data = histList.get(i);
						// 查询就诊详情
						List<PageData> drugList = clinicCheckinDetailService.listByCheckIn(data);
						if (drugList != null) {
							data.put("drugList", com.alibaba.fastjson.JSON.toJSON(drugList));
						}

						// 加上等待人数
						if (Integer.parseInt(data.get("state").toString()) == 0
								&& "false".equals(data.get("isYu").toString())) {// 当是未接诊时，给出前面还有几个人
							int waitNum = clinic_checkinService.intFromManBefore(data);
							data.put("waitNum", waitNum);
							histList.set(i, data);
						}
						histList.set(i, data);
					}
					map.put("list", histList);
					map.put("size", histList.size());
				} else {
					map.put("list", new ArrayList<>());
					map.put("size", 0);
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
	 * @description 患者端就诊记录与搜索
	 * @date 2018年3月1日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@SuppressWarnings("static-access")
	@RequestMapping(value = "/listHist2")
	@ResponseBody
	public Object listHist2() {
		logBefore(logger, "患者端就诊记录与搜索");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId")) || IsNull.paramsIsNull(pd.get("pageSize"))
					|| IsNull.paramsIsNull(pd.get("pageIndex"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd = PagingUtil.addPaging(pd);
				List<PageData> histList = clinic_checkinService.listHistPati(pd);
				if (histList != null && histList.size() > 0) {
					for (int i = 0; i < histList.size(); i++) {
						PageData data = histList.get(i);
						// 加上等待人数
						if (Integer.parseInt(data.get("state").toString()) == 0
								&& "false".equals(data.get("isYu").toString())) {// 当是未接诊时，给出前面还有几个人
							int waitNum = clinic_checkinService.intFromManBefore(data);
							data.put("waitNum", waitNum);
						}

						histList.set(i, data);
					}
					map.put("list", histList);
					map.put("size", histList.size());
				} else {
					map.put("list", new ArrayList<>());
					map.put("size", 0);
				}
				result = "0000";
				message = "成功";

				// ==============================================
				// 统计
				if (IsNull.paramsIsNull(pd.get("patientId")) == false
						&& IsNull.paramsIsNull(pd.get("searchDOC")) == false) {
					pd.put("dayDate", DateUtil.getDay());
					pd.put("userId", pd.get("patientId"));
					pd.put("fType", "2");
					pd.put("fState", "搜索历史记录");
					PageData data = functionService.getDayUser(pd);
					if (data == null) {
						// 不存在,添加
						pd.put("fNum", "1");
						pd.put("updateTime", new Date());
						functionService.save(pd);
					} else {
						// 存在,更新数据
						int fNum = Integer.parseInt(data.get("fNum").toString());
						pd.put("fNum", fNum + 1);
						pd.put("fid", data.get("fid"));
						pd.put("updateTime", new Date());
						functionService.updateDayACt(pd);
					}
				}
				// ==============================================

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
	 * @description 查询处方笺详情
	 * @date 2018年3月1日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/visitDetail")
	@ResponseBody
	public Object visitDetail() {
		logBefore(logger, "查询处方笺详情");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("checkinId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData data = clinic_checkinService.findByidxq(pd);
				if (IsNull.paramsIsNull(data) == false) {
					List<PageData> drugList = clinicCheckinDetailService.listByCheckIn(data);
					if (drugList != null) {
						data.put("drugList", com.alibaba.fastjson.JSON.toJSON(drugList));
					} else {
						data.put("drugList", "");
					}
					map.put("db", data);
				} else {
					map.put("db", new PageData());
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

}
