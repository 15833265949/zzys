package com.fh.controller.app.newRecipel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ClinicCheckinDetailService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.NewRecipelDetailService;
import com.fh.service.app.NewRecipelService;
import com.fh.service.statistics.TjFunctionService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.DoubleUtil;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PagingUtil;

/**
 * 描述：诊所医生处方逻辑处理
 * 
 * @author 霍学杰
 * @date 20180612
 * @version 1.0
 */

@Controller
@RequestMapping(value = "/api/newRecipel")
public class AppNewRecipelController extends BaseController {

	@Resource
	ClinicCheckinDetailService clinicCheckinDetailService;
	@Resource
	Clinic_checkinService clinic_checkinService;
	@Resource
	NewRecipelDetailService newRecipelDetailService;
	@Resource
	NewRecipelService newRecipelService;
	@Resource
	private TjFunctionService functionService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 保存处方(开方用)
	 * @date 2018年1月23日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/save")
	@ResponseBody
	public Object save() {
		logBefore(logger, "保存处方(开方用)");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("recipelName")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("checkinId")) || IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				// 根据id查询挂号详情
				PageData checInData = clinic_checkinService.findByidxq(pd);
				if (IsNull.paramsIsNull(checInData) == false) {
					// 查询就诊药品详情
					List<PageData> drugList = clinicCheckinDetailService.listByCheckIn(checInData);
					// 保存到处方中
					PageData reciPd = new PageData();
					reciPd.put("newRecipelId", this.get32UUID());
					reciPd.put("recipelName", pd.get("recipelName"));
					reciPd.put("clinicId", pd.get("clinicId"));
					reciPd.put("doctorId", pd.get("doctorId"));
					reciPd.put("doctorprescribe", checInData.get("doctorprescribe"));
					newRecipelService.save(reciPd);
					// 将药品保存到处方详细表中
					for (int i = 0; i < drugList.size(); i++) {
						PageData data = drugList.get(i);
						data.put("newRecipelId", reciPd.get("newRecipelId"));
						newRecipelDetailService.save(data);
					}
				}
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
	 * @description 删除处方
	 * @date 2018年1月23日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/delRecipel")
	@ResponseBody
	public Object delRecipel() {
		logBefore(logger, "删除处方");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("newRecipelIdS")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				retMessage = "参数异常";
			} else {
				String newRecipelIdArray[] = pd.get("newRecipelIdS").toString().split(",");
				pd.put("newRecipelIdArray", newRecipelIdArray);
				newRecipelService.del(pd);// 删除处方表
				newRecipelDetailService.delByNewId(pd);// 删除处方详情表
				result = "0000";
				retMessage = "成功";

				// ==============================================
				// 统计
				pd.put("dayDate", DateUtil.getDay());
				pd.put("userId", pd.get("doctorId"));
				pd.put("fType", "1");
				pd.put("fState", "删除处方");
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
				// ==============================================
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
	 * @description 修改处方
	 * @date 2018年1月23日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/editRecipel")
	@ResponseBody
	public Object editRecipel() {
		logBefore(logger, "修改处方");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("newRecipelId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				retMessage = "参数异常";
			} else {
				newRecipelService.editDo(pd);// 1.修改处方表*************处方表不用改，医嘱不用存
				newRecipelDetailService.delByNewId(pd);// 2.删除处方详情表
				// 3.解析数据然后保存
				if (IsNull.paramsIsNull(pd.get("xDrugList")) == false) {
					drugListSave(pd.get("xDrugList"), "xList", pd.get("newRecipelId").toString(), 1);
				}
				if (IsNull.paramsIsNull(pd.get("zDrugList")) == false) {
					drugListSave(pd.get("zDrugList"), "zList", pd.get("newRecipelId").toString(), 0);
				}

				result = "0000";
				retMessage = "成功";

				// ==============================================
				// 统计
				pd.put("dayDate", DateUtil.getDay());
				pd.put("userId", pd.get("doctorId"));
				pd.put("fType", "1");
				pd.put("fState", "修改处方");
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
				// ==============================================
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
	 * 去保存处方详情
	 * 
	 * @return
	 * @throws Exception
	 */
	public String drugListSave(Object drugList, String listName, String newRecipelId, int isWe) throws Exception {
		String money = "0.0";
		if (IsNull.paramsIsNull(drugList) == false) {
			JSONObject xDrug = JSONObject.fromObject(drugList);
			JSONArray jsonArray = JSONArray.fromObject(xDrug.get(listName));
			if (jsonArray.size() > 0) {
				for (int i = 0; i < jsonArray.size(); i++) {
					PageData dataPd = new PageData();
					dataPd.put("isSelf", 0);
					JSONObject data = JSONObject.fromObject(jsonArray.get(i));
					// 诊所药库ID
					if (IsNull.paramsIsNull(data.get("clinicdrugId")) == false) {
						dataPd.put("isSelf", 1);
						dataPd.put("clinicdrugId", data.get("clinicdrugId").toString());
					} else {
						dataPd.put("clinicdrugId", "");
					}
					// 售卖数量，只能是整数
					if (IsNull.paramsIsNull(data.get("count")) == false) {
						dataPd.put("count", Integer.parseInt(data.get("count").toString()));
					} else {
						dataPd.put("count", 0);
					}
					// 厂商
					if (IsNull.paramsIsNull(data.get("manufacturer")) == false) {
						dataPd.put("manufacturer", data.get("manufacturer").toString());
					} else {
						dataPd.put("manufacturer", "");
					}

					// 小单位售卖价格
					if (IsNull.paramsIsNull(data.get("packSellPrice")) == false) {
						dataPd.put("packSellPrice", data.get("packSellPrice").toString());
					} else {
						dataPd.put("packSellPrice", "0");
					}
					// 中单位售卖价格
					if (IsNull.paramsIsNull(data.get("packSellPriceM")) == false) {
						dataPd.put("packSellPriceM", data.get("packSellPriceM").toString());
					} else {
						dataPd.put("packSellPriceM", "0");
					}
					// 中 单位
					if (IsNull.paramsIsNull(data.get("packMiddle")) == false) {
						dataPd.put("packMiddle", data.get("packMiddle").toString());
					} else {
						dataPd.put("packMiddle", "");
					}
					// 小单位
					if (IsNull.paramsIsNull(data.get("packSmall")) == false) {
						dataPd.put("packSmall", data.get("packSmall").toString());
					} else {
						dataPd.put("packSmall", "");
					}
					// 售卖大单位价格
					if (IsNull.paramsIsNull(data.get("sellprice")) == false) {
						dataPd.put("sellprice", data.get("sellprice").toString());
					} else {
						dataPd.put("sellprice", "0");
					}
					// 名称
					if (IsNull.paramsIsNull(data.get("sysdrugName")) == false) {
						dataPd.put("sysdrugName", data.get("sysdrugName").toString());
					} else {
						dataPd.put("sysdrugName", "");
					}
					// 大单位
					if (IsNull.paramsIsNull(data.get("units")) == false) {
						dataPd.put("units", data.get("units").toString());
					} else {
						dataPd.put("units", "");
					}
					// 新加，当是诊所药库时，为空串；当是系统药库时，有值
					if (IsNull.paramsIsNull(data.get("sysdrugId")) == false) {
						dataPd.put("sysdrugId", data.get("sysdrugId").toString());
					} else {
						dataPd.put("sysdrugId", "");
					}
					// 新加，规格
					if (IsNull.paramsIsNull(data.get("spec")) == false) {
						dataPd.put("spec", data.get("spec").toString());
					} else {
						dataPd.put("spec", "");
					}
					// 售卖的状态 0没有选择 1 小单位 2中单位 3大单位 【默认为0】
					if (IsNull.paramsIsNull(data.get("packSellFlag")) == false) {
						dataPd.put("packSellFlag", Integer.parseInt(data.get("packSellFlag").toString()));
					} else {
						dataPd.put("packSellFlag", 0);
					}

					// ********************* 计算价格 0没有选择 1 小单位 2中单位 3大单位 【默认为0】
					if ((int) dataPd.get("isSelf") == 1) {
						int packSellFlag = Integer.parseInt(data.get("packSellFlag").toString());
						int count = Integer.parseInt(dataPd.get("count").toString());
						double price = 0.0f;
						switch (packSellFlag) {
						case 1:
							double packSellPrice = Double.parseDouble(dataPd.get("packSellPrice").toString());
							price = DoubleUtil.mul(packSellPrice, (double) count);
							dataPd.put("buyUnit", data.get("packSmall"));
							break;
						case 2:
							double packSellPriceM = Double.parseDouble(dataPd.get("packSellPriceM").toString());
							price = DoubleUtil.mul(packSellPriceM, (double) count);
							dataPd.put("buyUnit", data.get("packMiddle"));
							break;
						case 3:
							double sellprice = Double.parseDouble(dataPd.get("sellprice").toString());
							price = DoubleUtil.mul(sellprice, (double) count);
							dataPd.put("buyUnit", data.get("units"));
							break;
						default:
							dataPd.put("buyUnit", data.get("units"));
							break;
						}
						// 将价钱相加转2位小数点
						money = DoubleUtil.douSou(DoubleUtil.add(Double.parseDouble(money), price));
					}
					dataPd.put("newRecipelId", newRecipelId);
					dataPd.put("isWe", isWe);
					newRecipelDetailService.save(dataPd);
				}

			}
		}

		return money;
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 查询我的处方和搜索
	 * @date 2018年1月23日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/selRecipelByName")
	@ResponseBody
	public Object selRecipelByName() {
		logBefore(logger, "查询我的处方和搜索");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("doctorId"))
					|| IsNull.paramsIsNull(pd.get("pageSize")) || IsNull.paramsIsNull(pd.get("pageIndex"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd = PagingUtil.addPaging(pd);
				// 根据处方名称查询
				List<PageData> list = newRecipelService.listBySearName(pd);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						PageData data = list.get(i);
						List<PageData> drugList = newRecipelDetailService.findByRecipId(data);
						if (drugList != null) {
							data.put("drugList", com.alibaba.fastjson.JSON.toJSON(drugList));
						}
						list.set(i, data);
					}
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
	 * 描述:新增处方(处方管理用)
	 * 
	 * @author 王立飞
	 * @date 2018年8月5日 上午11:24:48
	 */
	@RequestMapping(value = "/add")
	@ResponseBody
	public Object add() {
		logBefore(logger, "新增处方---新版(处方管理用)");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("recipelName")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {

				// 保存到处方中
				PageData reciPd = new PageData();
				reciPd.put("newRecipelId", this.get32UUID());
				reciPd.put("recipelName", pd.get("recipelName"));
				reciPd.put("clinicId", pd.get("clinicId"));
				reciPd.put("doctorId", pd.get("doctorId"));
				if (IsNull.paramsIsNull(pd.get("doctorprescribe"))) {
					reciPd.put("doctorprescribe", "");
				} else {
					reciPd.put("doctorprescribe", pd.get("doctorprescribe"));
				}
				newRecipelService.save(reciPd);

				// 存药品详情
				if (IsNull.paramsIsNull(pd.get("xDrugList")) == false) {
					drugListSave(pd.get("xDrugList"), "xList", reciPd.get("newRecipelId").toString(), 1);
				}
				if (IsNull.paramsIsNull(pd.get("zDrugList")) == false) {
					drugListSave(pd.get("zDrugList"), "zList", reciPd.get("newRecipelId").toString(), 0);
				}

				// ==============================================
				// 统计
				pd.put("dayDate", DateUtil.getDay());
				pd.put("userId", pd.get("doctorId"));
				pd.put("fType", "1");
				pd.put("fState", "保存处方");
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
				// ==============================================

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
