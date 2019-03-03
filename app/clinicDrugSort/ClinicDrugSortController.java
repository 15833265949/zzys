package com.fh.controller.app.clinicDrugSort;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.alibaba.fastjson.JSON;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ClinicDrugSortService;
import com.fh.service.app.DrugclinicService;
import com.fh.service.statistics.TjFunctionService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * Title:药库模块类 董雪蕊 2018-01-07
 * 
 */
@Controller
@RequestMapping(value = "/api/clinicDrugSort")
public class ClinicDrugSortController extends BaseController {

	@Resource
	private DrugclinicService drugclinicService;
	@Resource
	private ClinicDrugSortService clinicDrugSortService;
	@Resource
	private TjFunctionService functionService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 库存管理首页列表
	 * @date 2018年1月7日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(value = "/getAllList")
	@ResponseBody
	public Object getAllList() {
		logBefore(logger, "库存管理首页列表");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				List<PageData> sortList = clinicDrugSortService.ListGetAll(pd);// 从数据库里得到的都是小科室

				Map<String, Object> rMap = new LinkedHashMap<>();
				if (sortList != null && sortList.size() > 0) {
					for (int i = 0; i < sortList.size(); i++) {
						PageData data = sortList.get(i);
						String bigSortName = data.getString("bigSortName");
						if (IsNull.paramsIsNull(rMap.get(bigSortName))) {
							List oList = new ArrayList<>();
							oList.add(data);
							rMap.put(bigSortName, oList);
						} else {
							List oList = (List) rMap.get(bigSortName);
							oList.add(data);
							rMap.put(bigSortName, oList);
						}
					}
					// 解析格式
					List retList = new ArrayList<>();
					for (Entry<String, Object> entry : rMap.entrySet()) {
						PageData da = new PageData();
						da.put("bigSortName", entry.getKey());
						da.put("bigList", entry.getValue());
						retList.add(da);
					}
					map.put("list", JSON.toJSON(retList));
				} else {
					map.put("list", new ArrayList<>());
				}
				// 西药的种类数
				pd.put("goodsType", "西药");
				List<PageData> Xlist = clinicDrugSortService.findBySum(pd);//查询诊所药类的种类数
				if (Xlist != null && Xlist.size() > 0) {
					map.put("XNum", Xlist.size());
					map.put("Xlist", Xlist);
				} else {
					map.put("XNum", 0);
					map.put("Xlist", new ArrayList<>());
				}

				// 中成药的种类数
				pd.put("goodsType", "中成药");
				List<PageData> ZClist = clinicDrugSortService.findBySum(pd);//查询诊所药类的种类数
				if (ZClist != null && ZClist.size() > 0) {
					map.put("ZCNum", ZClist.size());
					map.put("ZClist", ZClist);
				} else {
					map.put("ZCNum", 0);
					map.put("ZClist", new ArrayList<>());
				}

				// 中药的种类数
				pd.put("goodsType", "中药");
				List<PageData> ZYlist = clinicDrugSortService.findBySum(pd);//查询诊所药类的种类数
				if (ZYlist != null && ZYlist.size() > 0) {
					map.put("ZYNum", ZYlist.size());
					map.put("ZYlist", ZYlist);
				} else {
					map.put("ZYNum", 0);
					map.put("ZYlist", new ArrayList<>());
				}

				// 查询诊所药库存不足数
				pd.put("num", 5);
				List<PageData> LessList = clinicDrugSortService.lackOfStock(pd);
				if (LessList != null && LessList.size() > 0) {
					map.put("lackOfStock", LessList.size());
					map.put("LessList", LessList);
				} else {
					map.put("lackOfStock", 0);
					map.put("LessList", new ArrayList<>());
				}
				// 查询诊所药库存为0
				pd.put("num", 0);
				List<PageData> ZeroList = clinicDrugSortService.lackOfStock(pd);
				if (ZeroList != null && ZeroList.size() > 0) {
					map.put("stockZero", ZeroList.size());
					map.put("ZeroList", ZeroList);
				} else {
					map.put("stockZero", 0);
					map.put("ZeroList", new ArrayList<>());
				}
				result = "0000";
				message = "成功";
				// ==============================================
				// 统计
				if (IsNull.paramsIsNull(pd.get("doctorId")) == false) {
					pd.put("dayDate", DateUtil.getDay());
					pd.put("userId", pd.get("doctorId"));
					pd.put("fType", "1");
					pd.put("fState", "库存管理");
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
	 * @description 查询分类下的药品
	 * @date 2018年1月8日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/getSmallDrugDetail")
	@ResponseBody
	public Object getSmallDrugDetail() {
		logBefore(logger, "查询分类下的药品");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicAllSortId")) || IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				List<PageData> drugList = clinicDrugSortService.ListFromSmallSort(pd);
				if (drugList != null && drugList.size() > 0) {
					map.put("list", drugList);
				} else {
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
	 * @description 查询药品的详情
	 * @date 2018年1月8日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/getDrugDetail")
	@ResponseBody
	public Object getDrugDetail() {
		logBefore(logger, "查询药品的详情");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicdrugId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData data = drugclinicService.findById(pd);
				if (IsNull.paramsIsNull(data)) {
					map.put("db", new PageData());
				} else {
					map.put("data", data);
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
