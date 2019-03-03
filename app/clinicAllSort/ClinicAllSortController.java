package com.fh.controller.app.clinicAllSort;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.fh.config.CommonMessage;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ClinicAllSortService;
import com.fh.service.app.ClinicDrugSortService;
import com.fh.service.statistics.TjFunctionService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 诊所分类处理类 董雪蕊 2018-01-06
 */

@Controller
@RequestMapping(value = "/api/clinicAllSort")
public class ClinicAllSortController extends BaseController {

	@Resource
	ClinicAllSortService clinicAllSortService;
	@Resource
	ClinicDrugSortService clinicDrugSortService;
	@Resource
	TjFunctionService functionService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 得到诊所药品分类
	 * @date 2018年1月6日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(value = "/getClinList")
	@ResponseBody
	public Object getClinList() {
		logBefore(logger, "得到诊所药品分类");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				List<PageData> list = clinicAllSortService.listById(pd);

				// 2.将list转为固定格式的json
				// 先定义一个map，储存
				Map<String, List> cunMap = new LinkedHashMap<>();
				for (int i = 0; i < list.size(); i++) {
					PageData data = list.get(i);
					int isBig = Integer.parseInt(data.get("isBig").toString());
					String clinicAllSortId = data.get("clinicAllSortId").toString();// 自己类的ID
					String bigSortId = data.get("bigSortId").toString();
					String bigSortName = data.get("bigSortName").toString();
					String smallSortName = data.get("smallSortName").toString();
					if (isBig == 1) {// 不是大类 -->判断有没有大类
						if (IsNull.paramsIsNull(cunMap.get(bigSortName + "," + bigSortId))) {// key值用name,iD拼接
							// 没有大类
							List<PageData> smallList = new ArrayList<>();
							smallList.add(data);
							cunMap.put(bigSortName + "," + bigSortId, smallList);
						} else {
							// 有大类的时候判断是否有小类？不用判断，小类是唯一的
							List<PageData> smallList = cunMap.get(bigSortName + "," + bigSortId);
							smallList.add(data);
							cunMap.put(bigSortName + "," + bigSortId, smallList);
						}
					} else {// 大类
						if (IsNull.paramsIsNull(cunMap.get(bigSortName + "," + bigSortId))) {// key值用name,iD拼接
							// 没有大类
							List<PageData> smallList = new ArrayList<>();
							cunMap.put(smallSortName + "," + clinicAllSortId, smallList);
						}
					}
				}
				// 解析到前端需要的格式
				List retList = new ArrayList<>();
				for (Map.Entry<String, List> entry : cunMap.entrySet()) {
					Map<String, Object> rMap = new HashMap<>();
					String[] bigS = entry.getKey().split(",");
					rMap.put("bigSortId", bigS[1]);
					rMap.put("bigSortName", bigS[0]);
					rMap.put("smallSortNameList", entry.getValue());
					retList.add(rMap);
				}

				Object jsonArray = JSON.toJSON(retList);
				map.put("list", jsonArray);
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
	 * @description 诊所新增一个小分类（不能再大分类下有重名）（大分类不能新增）
	 * @date 2018年1月6日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/saveSmallSort")
	@ResponseBody
	public Object saveSmallSort() {
		logBefore(logger, "诊所新增一个小分类");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("bigSortId"))
					|| IsNull.paramsIsNull(pd.get("bigSortName")) || IsNull.paramsIsNull(pd.get("smallSortName"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData haveData = clinicAllSortService.smallSortIsHave(pd);
				if (IsNull.paramsIsNull(haveData)) {
					pd.put("clinicAllSortId", this.get32UUID());
					pd.put("isBig", 1);
					pd.put("isWhole", 1);
					clinicAllSortService.save(pd);
					result = "0000";
					message = "成功";

					// ==============================================
					// 统计
					if (IsNull.paramsIsNull(pd.get("doctorId")) == false) {
						pd.put("dayDate", DateUtil.getDay());
						pd.put("userId", pd.get("doctorId"));
						pd.put("fType", "1");
						pd.put("fState", "新增小分类");
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
				} else {
					result = "3101";
					message = CommonMessage.CODE_3101;// 该分类已存在，请换个分类名！
				}
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
	 * @description 搜索诊所自己的类型
	 * @date 2018年1月7日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/searchClinicSort")
	@ResponseBody
	public Object searchClinicSort() {
		logBefore(logger, "搜索诊所自己的类型");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("searchName"))) {
				result = "9993";
				message = "参数异常";
			} else {
				List<PageData> list = clinicAllSortService.listByDimSort(pd);
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
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
	 * @description 诊所删除小分类
	 * @date 2018年1月7日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/delSmallSort")
	@ResponseBody
	public Object delSmallSort() {
		logBefore(logger, "诊所删除小分类");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("clinicAllSortId"))
					|| IsNull.paramsIsNull(pd.get("smallSortName"))) {
				result = "9993";
				message = "参数异常";
			} else {
				// 1.先查询诊所药库里是否有这个分类
				// 有的话处理：当删除某项新建分类的时候，该分类下的药品如果只被标记了被删除的这项分类，那么该药品移动到其他的分类里面：（西药的其他这个大类下面有一个其他的小类；中药—其他）；
				// 当该分类下的药品除了这个分类还有其他分类的时候，那么只是减去这个分类

				List<PageData> drugList = clinicDrugSortService.ListFromSmallSortId(pd);
				for (int i = 0; i < drugList.size(); i++) {
					PageData drugData = drugList.get(i);
					int num = clinicDrugSortService.intByDrugId(drugData);
					if (num == 1) {// 只有这一个分类，移动到其它
						drugData.put("bigSortId", 4);
						drugData.put("clinicAllSortId", 102);
						clinicDrugSortService.save(drugData);
					}
					// 删除
					clinicDrugSortService.del2(drugData);
				}
				// 2.删除分类表里数据
				clinicAllSortService.delId(pd);
				result = "0000";
				message = "成功";

				// ==============================================
				// 统计
				if (IsNull.paramsIsNull(pd.get("doctorId")) == false) {
					pd.put("dayDate", DateUtil.getDay());
					pd.put("userId", pd.get("doctorId"));
					pd.put("fType", "1");
					pd.put("fState", "删除小分类");
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

}
