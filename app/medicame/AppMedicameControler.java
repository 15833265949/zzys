package com.fh.controller.app.medicame;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.config.CommonConfig;
import com.fh.config.CommonMessage;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ClinicDrugSortService;
import com.fh.service.app.ClinicService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.DrugOutRecordService;
import com.fh.service.app.DrugclinicService;
import com.fh.service.app.DrugrecordService;
import com.fh.service.statistics.TjFunctionService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.GetPinyin;
import com.fh.util.PageData;
import com.fh.util.Pingyinj;
import com.fh.util.tiaoxingma.Txmfind;
import com.fh.util.tools.DoubleUtil;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.IsNumeric;
import com.fh.util.tools.MyMatcher;
import com.google.gson.JsonObject;

/**
 * 描述：诊所药库业务逻辑处理
 * 
 * @author 霍学杰
 * @date 2017.10.07
 * @version 1.2
 */

@SuppressWarnings("serial")
@Controller
@RequestMapping(value = "/api/medicame")
@JsonIgnoreProperties(value = { "list" })
public class AppMedicameControler extends BaseController implements java.io.Serializable {

	@Resource
	private DoctorService doctorService;
	@Resource
	private DrugclinicService drugclinicService;
	@Resource
	private DrugrecordService drugrecordService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private DrugOutRecordService drugOutRecordService;
	@Resource
	private TjFunctionService functionService;
	@Resource
	private ClinicDrugSortService clinicDrugSortService;

	/**
	 * 1.描述：诊所端 药库管理员 扫码入库
	 * 
	 * @author 霍学杰
	 * @date 2017年10月7日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	@RequestMapping(value = "/findDrugsj", method = RequestMethod.POST)
	@ResponseBody
	public Object findDrugsj() {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 诊断结果
			if (IsNull.paramsIsNull(pd.get("code")) || IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else if (IsNumeric.isNumeric(pd.get("code").toString()) == false
					|| pd.get("code").toString().length() != 13) {
				result = "5006";
				message = "请扫描正确的条形码！";
			} else {
				// 1.先在 诊所药库查询是否有该药品
				PageData findcodeDrug = drugclinicService.findcodeDrug(pd);
				if (findcodeDrug != null) {// 诊所药库有直接返回数据
					findcodeDrug.put("img", "");
					findcodeDrug.put("xingZhuang", "");
					findcodeDrug.put("useMethod", "");
					findcodeDrug.put("price", "");
					findcodeDrug.put("prodAddr", "");
					map.put("drug", findcodeDrug);
					result = "5005";
					message = "诊所药品查询成功";
				} else {
					// 2.诊所药库 查询药品为空 查询系统药库是否存在该药品
					PageData findsysDrug = drugclinicService.findsysDrug(pd);
					if (findsysDrug != null) {// 诊所库查不到 系统库存在 直接返回药品信息
						findsysDrug.put("inventory", "0");
						findsysDrug.put("indate", "");
						findsysDrug.put("upinventory", "0");
						findsysDrug.put("upindate", "");
						findsysDrug.put("inprice", "");
						findsysDrug.put("sellprice", "");
						findsysDrug.put("packSellFlag", "0");
						map.put("drug", findsysDrug);
						result = "0000";
						message = "成功";
					} else {
						// 3.系统库也为空 调用api 根据条码查询药品信息
						String code = pd.get("code").toString();
						// 得到用哪家的药品数据 1：易源数据 2京东万象数据
						Boolean isFind = false;
						PageData dd = new PageData();
						if (CommonConfig.GET_FLAG == 1) {
							JsonObject getafdxq = Txmfind.gettxmxq(code);

						} else {
							JsonObject getafdxq = Txmfind.getafdxq(code);
							String flag = getafdxq.get("code").getAsString();
							if (flag.equals("10000")) {
								JsonObject yaopin = (JsonObject) getafdxq.get("result");
								// showapi_res_code
								int asInt = Integer.parseInt(yaopin.get("showapi_res_code").getAsString());
								if (asInt == 0) {
									System.out.println("收费");
								}
								JsonObject xiangqing = (JsonObject) yaopin.get("showapi_res_body");
								if (xiangqing.get("ret_code").getAsInt() == 0) {// 返回成功信息
									dd = getMedicineXiangQing(xiangqing);// 得到药品具体数据*********
									isFind = true;
								}
							}
						}
						if (isFind) {
							PageData isHaveData = drugclinicService.findByLicense(dd);
							if (IsNull.paramsIsNull(isHaveData)) {
								drugclinicService.Syssave(dd);// 添加系统药库数据
							} else {
								drugclinicService.editByLicense(pd);
							}

							dd.put("inventory", "0");// 库存
							dd.put("indate", "");// 保质期至 有效期
							dd.put("upinventory", "0");// 上一批次药 库存剩余
							dd.put("upindate", "");// 上一批药 保质期
							dd.put("inprice", "");// 进价
							dd.put("sellprice", "");// 售价
							dd.put("vipprice", "");// 售价
							dd.put("packSellFlag", "0");// 售卖的状态 0没有选择 1 小单位
														// 2中单位 3大单位 【默认为0】
							dd.remove("sysdrugId");
							map.put("drug", dd);// 返回数据
							result = "0000";
							message = "成功";
						} else {
							map.put("code", pd.get("code"));
							result = "10003";
							message = "查询不到相应的数据";
						}
					}

				}

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
	 * 4.描述：诊所端 药库管理员 模糊搜索药品 是药品入库时使用
	 * 
	 * @author 霍学杰
	 * @date 2017年10月8日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/finddimZSDRug2", method = RequestMethod.POST)
	@ResponseBody
	public Object finddimZSDRug2() {
		logBefore(logger, "诊所端 药库管理员 模糊搜索药品");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "查询成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("sysdrugName"))
					|| IsNull.paramsIsNull(pd.get("goodsType"))) {
				result = "9993";
				message = "参数异常";
			} else {
				List<PageData> list = drugclinicService.finddimZSDRug(pd);
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
				} else {
					list = drugclinicService.dimSearch(pd);// 模糊搜索了系统药库
					if (list != null && list.size() > 0) {
						PageData addData = new PageData();
						addData.put("inventory", "0");// 库存
						addData.put("indate", "");// 保质期至 有效期
						addData.put("upinventory", 0);// 上一批次药 库存剩余
						addData.put("upindate", "");// 上一批药 保质期
						addData.put("inprice", "");// 进价
						addData.put("sellprice", "");// 售价
						addData.put("vipprice", "");// 进价
						addData.put("packSellFlag", "0");// 售卖的状态 0没有选择 1 小单位
															// 2中单位 3大单位 【默认为0】
						for (int i = 0; i < list.size(); i++) {
							PageData data = list.get(i);
							data.putAll(addData);
						}
						map.put("list", list);
						map.put("size", list.size());
					} else {
						map.put("list", new ArrayList<PageData>());
						map.put("size", 0);
					}
				}
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
	 * 4.描述：诊所端 药库管理员 模糊搜索药品 是库存管理时使用
	 * 
	 * @author 霍学杰
	 * @date 2017年10月8日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/finddimZSDRug", method = RequestMethod.POST)
	@ResponseBody
	public Object finddimZSDRug() {
		logBefore(logger, "诊所端 药库管理员 模糊搜索药品  库存管理");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "查询成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				List<PageData> list = drugclinicService.finddimZSDRug(pd);
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
					result = "0000";
					message = "成功";
				} else {
					result = "0000";
					map.put("list", new ArrayList<PageData>());
					map.put("size", 0);
					message = "成功";
				}
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
	 * 5.描述：诊所端 药库管理员 药品入库 第三版
	 * 
	 * @author 董雪蕊
	 * @date 2018年1月6日
	 * @version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	@RequestMapping(value = "/saveclinicDRug3", method = RequestMethod.POST)
	@ResponseBody
	public Object saveclinicDRug3() {
		logBefore(logger, "诊所端 药库管理员 药品入库");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "查询成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("sysdrugName"))
					|| IsNull.paramsIsNull(pd.get("goodsType")) || IsNull.paramsIsNull(pd.get("sortInfo"))
					|| IsNull.paramsIsNull(pd.get("packSellFlag")) || IsNull.paramsIsNull(pd.get("units"))
					|| IsNull.paramsIsNull(pd.get("sellprice")) || IsNull.paramsIsNull(pd.get("rukunum"))
					|| IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else if ((!pd.get("goodsType").toString().equals("中药"))
					&& (IsNull.paramsIsNull(pd.get("specD")) || IsNull.paramsIsNull(pd.get("packMiddle"))
							|| IsNull.paramsIsNull(pd.get("packMiddleNum")) || (IsNull.paramsIsNull(pd.get("spec"))))) {// 判断不是中药必传项
				result = "9993";
				message = "参数异常";
			} else {
				/**
				 * 添加VIP逻辑--不是VIP只能添加100条
				 */
				if (!isSaveMedi(pd)) {
					result = "3100";
					message = CommonMessage.CODE_3100;// 您不是VIP，只能入库100种药品！
					map.put("message", message);
					map.put("result", result);
					return AppUtil.returnObject(new PageData(), map);
				}

				// 下面是正常逻辑
				pd.put("rukunum", DoubleUtil.douSou(Double.parseDouble(pd.get("rukunum").toString())));
				// 换算售卖价格
				int packSellFlag = Integer.parseInt(pd.get("packSellFlag").toString());//
				double totalPrice = Double.parseDouble(pd.get("sellprice").toString());
				double totalSell = 0;
				double packMiddleNum = 0;
				if (IsNull.paramsIsNull(pd.get("packMiddleNum")) == false) {
					packMiddleNum = Double.parseDouble(pd.get("packMiddleNum").toString());
				}
				double packSellPrice = 0f;// 小价格
				double packSellPriceM = 0f;// 中价格
				if (IsNull.paramsIsNull(pd.get("packSmallNum")) == false) {// 得到小单位价格
					double packSmallNum = Double.parseDouble(pd.get("packSmallNum").toString());
					totalSell = packSmallNum * packMiddleNum;
					packSellPrice = DoubleUtil.div(totalPrice, totalSell, 2);
				}
				if (IsNull.paramsIsNull(pd.get("packMiddleNum")) == false) {// 得到中单位价格
					packSellPriceM = DoubleUtil.div(totalPrice, (double) packMiddleNum, 2);
				}
				pd.put("totalSell", totalSell);
				pd.put("packSellPrice", packSellPrice);
				pd.put("packSellPriceM", packSellPriceM);
				// 添加
				PageData findcodeDrug = drugclinicService.findIsEqualDrug(pd);
				PageData findById = doctorService.findById(pd);// 查询医生信息
				if (IsNull.paramsIsNull(findcodeDrug)) {
					pd.put("clinicdrugId", this.get32UUID());
					pd.put("engName", GetPinyin.getPingYin(pd.getString("sysdrugName")));
					pd.put("logogram", Pingyinj.getFirstSpell(pd.getString("sysdrugName")).toUpperCase());
					pd.put("inventory", pd.getString("rukunum"));
					pd.put("creatTime", DateUtil.getTime());
					pd.put("editTime", DateUtil.getTime());
					pd.put("upinventory", 0);
					pd.put("upindate", "");
					String flag = pd.getString("goodsType");
					if (flag.equals("中成药")) {
						System.out.println("中成药");
					} else if (flag.equals("西药")) {
						System.out.println("西药");
					} else if (flag.equals("中药")) {
						System.out.println("中药");
						pd.put("spec", pd.get("units"));
					}
					drugclinicService.clinicsave(pd);// 添加诊所药品数据
					// 查询信息
					PageData data = drugclinicService.findById(pd);
					data.put("num", pd.get("rukunum"));
					// 添加入库信息
					ruku(data);
				} else {
					pd.put("clinicdrugId", findcodeDrug.get("clinicdrugId"));
					double num = Double.parseDouble(pd.getString("rukunum"));
					double inventory = Double.parseDouble(findcodeDrug.getString("inventory"));
					int totalSellOld = Integer.parseInt(findcodeDrug.get("totalSell").toString());
					pd.put("totalSell", totalSellOld + totalSell);
					pd.put("inventory", inventory + num);
					pd.put("upinventory", inventory);
					pd.put("upindate", findcodeDrug.getString("indate"));
					pd.put("editTime", DateUtil.getTime());
					drugclinicService.editdrugKC(pd);
					// 将分类表先删除旧的
					clinicDrugSortService.del(pd);
					// 查询信息
					PageData data = drugclinicService.findById(pd);
					data.put("num", pd.get("rukunum"));
					// 添加入库信息
					ruku(data);
				}
				// 添加分类表
				net.sf.json.JSONArray jsonArray = net.sf.json.JSONArray.fromObject(pd.get("sortInfo").toString());
				PageData sortPd = new PageData();
				for (int i = 0; i < jsonArray.size(); i++) {
					net.sf.json.JSONObject sObject = jsonArray.getJSONObject(i);
					String bigSortId = sObject.get("bigSortId").toString();
					String clinicAllSortId = sObject.get("clinicAllSortId").toString();
					sortPd.put("bigSortId", bigSortId);
					sortPd.put("clinicAllSortId", clinicAllSortId);
					sortPd.put("clinicId", pd.get("clinicId"));
					sortPd.put("clinicdrugId", pd.get("clinicdrugId"));
					clinicDrugSortService.save(sortPd);
				}
				result = "0000";
				message = "成功";
				// ==============================================
				// 统计
				pd.put("dayDate", DateUtil.getDay());
				pd.put("userId", pd.get("doctorId"));
				pd.put("fType", "1");
				pd.put("fState", "药品入库");
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
	 * @author 董雪蕊 还在使用（2018-06-25）
	 * @description 一种药 当天出库明细
	 * @date 2017年11月15日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "medInfoDay")
	@ResponseBody
	public Object medInfoDay() {
		logBefore(logger, " 一种药 当天出库明细");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicdrugId")) || IsNull.paramsIsNull(pd.get("outDate"))) {
				result = "9993";
				retMessage = "参数异常";
				
			} else {
				List<PageData> list = drugrecordService.medInfoDayList(pd);
				if (IsNull.paramsIsNull(list) == false) {
					map.put("list", list);
				} else {
					map.put("list", new ArrayList<>());
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
	 * @description 药品
	 * @date 2017年11月5日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unused" })
	@RequestMapping(value = "/unitList")
	@ResponseBody
	public Object unitList() {
		logBefore(logger, "药品description ");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			String s = "片、粒、袋、瓶、包、支、盒、大盒、板、g、mg、kg、ml、L、ug、IU、U、丸、贴、颗、滴、次、个、副、箱、把、对、膏、IIU、筒、条、cm、dag、根、双、卷、排、封、罐、套、单位、万单位、揿、枚、只、台、吸";
			String sS[] = s.split("、");
			List unitList = java.util.Arrays.asList(sS);
			map.put("list", unitList);
			result = "0000";
			message = "成功";
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("retMessage", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 解析药品详情
	 * 
	 * @param xiangqing
	 * @return
	 */
	private PageData getMedicineXiangQing(JsonObject xiangqing) {
		PageData dd = new PageData();
		dd.put("sysdrugId", this.get32UUID());

		// code// 药品条码
		dd.put("code", "");
		if (IsNull.paramsIsNull(xiangqing.get("code")) == false) {
			dd.put("code", xiangqing.get("code").getAsString());// 药品条码
		}
		// goodsName// 药品名称
		dd.put("sysdrugName", "");
		if (IsNull.paramsIsNull(xiangqing.get("goodsName")) == false) {
			String goodsName = xiangqing.get("goodsName").getAsString().replaceAll("^[\u4e00-\u9fa5]", "");
			dd.put("sysdrugName", xiangqing.get("goodsName").getAsString());//
			try {
				dd.put("engName", Pingyinj.getFullSpell(goodsName));
				dd.put("logogram", Pingyinj.getFirstSpell(goodsName));
			} catch (Exception e) {
				dd.put("engName", "");
				dd.put("logogram", "");
			}

		}
		// 药品type // 类型 中成药 西药
		dd.put("goodsType", "");
		dd.put("purpose", "");// 功能主治
		if (IsNull.paramsIsNull(xiangqing.get("goodsType")) == false) {
			String goodsType = xiangqing.get("goodsType").getAsString();
			dd.put("goodsType", goodsType);
			String[] type = goodsType.split(">>");
			if (type.length >= 2) {
				dd.put("goodsType", type[0]);// 类型 中成药 西药
				dd.put("purpose", type[1]);// 功能主治
			}
		}
		if (dd.get("goodsType").toString().equals("中药") || dd.get("goodsType").toString().equals("中成药")
				|| dd.get("goodsType").toString().equals("西药")) {

		} else {
			dd.put("goodsType", "西药");
		}
		// license 国字药
		dd.put("license", "");
		if (IsNull.paramsIsNull(xiangqing.get("license")) == false) {
			dd.put("license", xiangqing.get("license").getAsString());
		}
		// 单位
		dd.put("units", "");
		if (IsNull.paramsIsNull(xiangqing.get("units")) == false) {
			dd.put("units", xiangqing.get("units").getAsString());
		}
		// 规格 spec
		dd.put("spec", "");
		dd.put("specD", "");
		dd.put("packSmall", "");// 包装-小单位
		dd.put("packMiddle", "");// 包装-中单位
		dd.put("packSmallNum", "");// 包装-小单位数量
		dd.put("packMiddleNum", "");// 包装-中单位数量
		if (IsNull.paramsIsNull(xiangqing.get("spec")) == false) {
			dd.put("specAll", xiangqing.get("spec").getAsString());// "\\*|/"
			String specAll = xiangqing.get("spec").getAsString();
			String all[] = specAll.split("\\*|/|×|X");
			if (all.length == 3) {
				// 小单位
				String packSmallNum = MyMatcher.GetMatherString(all[0], "[\\d+(\\.\\d)?]*");
				if (IsNull.paramsIsNull(packSmallNum)) {
					packSmallNum = "1";
					dd.put("packSmall", all[0]);
				} else {
					dd.put("packSmallNum", packSmallNum);
					dd.put("packSmall", all[0].replace(packSmallNum, ""));
				}
				// 中单位
				String packMiddleNum = MyMatcher.GetMatherString(all[1], "[\\d+(\\.\\d)?]*");
				if (IsNull.paramsIsNull(packMiddleNum)) {
					packMiddleNum = "1";
					dd.put("packMiddle", all[1]);
				} else {
					dd.put("packMiddleNum", packMiddleNum);
					dd.put("packMiddle", all[1].replace(packMiddleNum, ""));
				}
				// 大单位
				if (IsNull.paramsIsNull(dd.get("units"))) {
					dd.put("unit", all[2].replace("[\\d+(\\.\\d)?]*", ""));
				}
			} else if (all.length == 2) {
				// 中单位
				String packMiddleNum = MyMatcher.GetMatherString(all[0], "[\\d+(\\.\\d)?]*");
				if (IsNull.paramsIsNull(packMiddleNum)) {
					packMiddleNum = "1";
					dd.put("packMiddle", all[0]);
				} else {
					dd.put("packMiddleNum", packMiddleNum);
					dd.put("packMiddle", all[0].replace(packMiddleNum, ""));
				}
				// 大单位
				if (IsNull.paramsIsNull(dd.get("units"))) {
					dd.put("unit", all[1].replace("[\\d+(\\.\\d)?]*", ""));
				}
			} else if (all.length == 1) {
				// 大单位
				if (IsNull.paramsIsNull(dd.get("units"))) {
					dd.put("unit", all[0].replace("[\\d+(\\.\\d)?]*", ""));
				}
			}
		}
		// 生产厂家
		dd.put("manufacturer", "");
		if (IsNull.paramsIsNull(xiangqing.get("manuName")) == false) {
			dd.put("manufacturer", xiangqing.get("manuName").getAsString());
		}

		// trademark 商标
		dd.put("trademark", "");
		if (IsNull.paramsIsNull(xiangqing.get("trademark")) == false) {
			dd.put("trademark", xiangqing.get("trademark").getAsString());
		}
		// price 价格
		dd.put("price", "");
		if (IsNull.paramsIsNull(xiangqing.get("price")) == false) {
			dd.put("price", xiangqing.get("price").getAsString());
		}
		// 产品地址 prodAddr
		dd.put("prodAddr", "");
		if (IsNull.paramsIsNull(xiangqing.get("prodAddr")) == false) {
			dd.put("prodAddr", xiangqing.get("prodAddr").getAsString());
		}
		// 图片 img
		dd.put("img", "");
		if (IsNull.paramsIsNull(xiangqing.get("img")) == false) {
			dd.put("img", xiangqing.get("img").getAsString());
		}
		//// 说明
		dd.put("ingredients", "");// 主要成分
		dd.put("indications", "");// 适应症
		dd.put("xingZhuang", "");// 性状
		dd.put("useMethod", "");// useMethod 用法用量
		// 有效期
		dd.put("useFulDay", "");
		if (IsNull.paramsIsNull(xiangqing.get("note")) == false) {
			String note = xiangqing.get("note").toString();
			note = note.replace("\\", "");
			String[] notes = note.split("rn");
			if (notes.length > 0) {
				for (int i = 0; i < notes.length; i++) {
					if (notes[i].contains("【产品名称】")) {
						// dd.put("sysdrugName",notes[i].replace("【产品名称】", ""));
					} else if (notes[i].contains("【商标】")) {
						dd.put("trademark", notes[i].replace("【商品名/商标】", ""));
					} else if (notes[i].contains("【规格】")) {
						dd.put("spec", notes[i].replace("【规格】", ""));
					} else if (notes[i].contains("【主要成份】")) {
						dd.put("ingredients", notes[i].replace("【主要成份】", ""));
					} else if (notes[i].contains("【性状】")) {
						dd.put("xingZhuang", notes[i].replace("【性状】", ""));
					} else if (notes[i].contains("【适应症】")) {
						dd.put("indications", notes[i].replace("【适应症】", ""));
					} else if (notes[i].contains("【生产厂家】")) {
						dd.put("manufacturer", notes[i].replace("【生产厂家】", ""));
					} else if (notes[i].contains("【批准文号】")) {
						dd.put("license", notes[i].replace("【批准文号】", ""));
					} else if (notes[i].contains("【用法用量】")) {
						String useMethod = notes[i].replace("【用法用量】", "");
						dd.put("useMethod", useMethod.replaceAll("\\s*", ""));
						PageData useMethodData = getXiangUseMethod(useMethod);
						dd.put("useFangShi", useMethodData.get("useFangShi"));
						dd.put("useDanWei", useMethodData.get("useDanWei"));
						dd.put("useChengReShu", useMethodData.get("useChengReShu"));
						dd.put("useChengReCi", useMethodData.get("useChengReCi"));
					}
				}

			}

		}

		return dd;
	}

	/**
	 * 得到用法用量的详细数据 董雪蕊 2017-10-13
	 * 
	 * @param useMethod
	 * @return
	 */
	public static PageData getXiangUseMethod(String useMethod) {
		useMethod = useMethod.replaceAll("\\s*", "");
		PageData xiangUseData = new PageData();
		// 用药方式 口服、肌肉注射给药、白天服用、直肠给药、温开水冲服、外用、含服、空腹服
		String useFangShi = MyMatcher.GetMatherString(useMethod,
				"冲服|口服|肌肉注射给药|白天服用|直肠给药|温开水冲服|外用|含服|空腹服|肌内注射或静脉注射|肌内注射|静脉注射|温水冲服|喷雾吸入");
		// 用药单位 片、包、g、mg、 ml、支、粒、袋
		String useDanWei = MyMatcher.GetMatherString(useMethod, "枚|片|包|g|克|mg|毫克|ml|毫升|支|粒|袋");
		if (useDanWei.equals("克")) {
			useDanWei = "g";
		} else if (useDanWei.equals("毫克")) {
			useDanWei = "mg";
		} else if (useDanWei.equals("毫升")) {
			useDanWei = "ml";
		}
		// 成人每次用药数量
		String useChengReShu = MyMatcher.GetMatherString(useMethod, "(一次|每次)\\d+(\\.\\d)?").replace("每次", "")
				.replace("一次", "");
		// 成人每天用药次数
		String useChengReCi = MyMatcher.GetMatherString(useMethod, "((日|天)\\d)|(\\d+次/日)").replace("次/日", "")
				.replace("日", "").replace("天", "");

		xiangUseData.put("useFangShi", useFangShi);
		xiangUseData.put("useDanWei", useDanWei);
		xiangUseData.put("useChengReShu", useChengReShu);
		xiangUseData.put("useChengReCi", useChengReCi);
		return xiangUseData;

	}

	/**
	 * 查询用户是否可以保存药品
	 * 
	 * @param pd
	 * @return
	 * @throws Exception
	 */
	public Boolean isSaveMedi(PageData pd) throws Exception {
		Boolean isCan = false;
		PageData clinicData = clinicService.findzsClinic(pd);
		if (IsNull.paramsIsNull(clinicData) == false) {
			if (IsNull.paramsIsNull(clinicData.get("vipEndTime")) == false
					&& DateUtil.compareDate(clinicData.get("vipEndTime").toString(), DateUtil.getTime())) {
				isCan = true;
			} else {
				PageData countData = drugclinicService.findCount(pd);
				if (IsNull.paramsIsNull(countData) == false) {
					int count = Integer.parseInt(countData.get("count").toString());
					if (count < CommonConfig.ruMedicNum) {
						isCan = true;
					}
				}
			}
		}

		return isCan;
	}

	/**
	 * 3.描述：诊所端 药库管理员 库存调整
	 * 
	 * @author 董雪蕊
	 * @date 2018年1月15日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/updatedrugnum2", method = RequestMethod.POST)
	@ResponseBody
	public Object updatedrugnum2() {
		logBefore(logger, "库存调整");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "查询成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicdrugId")) || IsNull.paramsIsNull(pd.get("inventory"))
					|| IsNull.paramsIsNull(pd.get("sellprice"))) {
				result = "9993";
				message = "参数异常";
			} else {

				// ***判断出入库，并存入记录
				PageData clinicDrugData = drugclinicService.findById(pd);// 查询药品信息 根据ID
				if (IsNull.paramsIsNull(clinicDrugData) == false
						&& IsNull.paramsIsNull(clinicDrugData.get("inventory")) == false) {
					double inventory = Double.parseDouble(clinicDrugData.getString("inventory"));
					double newInventory = Double.parseDouble(pd.getString("inventory"));
					pd.put("inventory", DoubleUtil.douSou(newInventory));
					PageData inOutPd = new PageData();
					inOutPd.put("drugrecordId", this.get32UUID());
					inOutPd.put("clinicdrugId", pd.get("clinicdrugId").toString());
					inOutPd.put("code", clinicDrugData.get("code").toString());
					inOutPd.put("sysdrugName", clinicDrugData.get("sysdrugName").toString());
					inOutPd.put("spec", clinicDrugData.get("spec").toString());
					inOutPd.put("manufacturer", clinicDrugData.get("manufacturer").toString());
					inOutPd.put("units", clinicDrugData.get("units").toString());
					if (IsNull.paramsIsNull(pd.get("clinicId"))) {
						inOutPd.put("clinicId", clinicDrugData.get("clinicId").toString());
						pd.put("clinicId", clinicDrugData.get("clinicId"));
					} else {
						inOutPd.put("clinicId", pd.get("clinicId").toString());
					}
					inOutPd.put("price", "");
					if (IsNull.paramsIsNull(pd.get("sellprice")) == false) {
						inOutPd.put("price", pd.get("sellprice"));// 新价格
					}
					inOutPd.put("OldPrice", clinicDrugData.get("sellprice"));// 原价格
					inOutPd.put("createTime", DateUtil.getTime());
					inOutPd.put("license", clinicDrugData.get("license").toString());
					inOutPd.put("outPatientId", "");
					inOutPd.put("outPatientName", "库存调整");
					inOutPd.put("outDoctorId", "");
					inOutPd.put("outDoctorName", "");
					if (IsNull.paramsIsNull(pd.get("doctorId")) == false) {
						PageData docData = doctorService.findById(pd);// 查询医生信息
						if (docData != null) {
							inOutPd.put("outDoctorId", docData.get("doctorId"));
							inOutPd.put("outDoctorName", docData.get("trueName"));
							int authPlat = Integer.parseInt(docData.get("authPlat").toString());
							if (authPlat == 1) {
								inOutPd.put("outAuthPlat", "医生");
							} else if (authPlat == 2) {
								inOutPd.put("outAuthPlat", "医生/收银员");
							} else if (authPlat == 3) {
								inOutPd.put("outAuthPlat", "收银员");
							} else if (authPlat == 4) {
								inOutPd.put("outAuthPlat", "收银员/药剂师");
							} else if (authPlat == 5) {
								inOutPd.put("outAuthPlat", "药剂师");
							} else if (authPlat == 6) {
								inOutPd.put("outAuthPlat", "全职");
							}
						}
					}
					inOutPd.put("goodsType", clinicDrugData.get("goodsType").toString());
					inOutPd.put("inventory", inventory);
					inOutPd.put("newInventory", newInventory);
					if (newInventory > inventory) {// 入库
						inOutPd.put("num", DoubleUtil.sub(newInventory, inventory));
						inOutPd.put("flag", 1);
						drugrecordService.save(inOutPd);
					} else if (newInventory < inventory) {// 出库
						inOutPd.put("num", DoubleUtil.sub(inventory, newInventory));
						inOutPd.put("flag", 2);
						drugrecordService.save(inOutPd);
						// 添加到出库信息统计表
						// 1.先根据clinicdrugId、日期查询这个药是否有数据，如果有-->这个记录+1。。。。。月记录不修改，
						pd.put("outDate", DateUtil.getDay());
						PageData dateData = drugOutRecordService.isHaveDrug(pd);
						// 2.如果没有-->查询年月是否有数据，
						if (IsNull.paramsIsNull(dateData)) {
							inOutPd.put("outDate", DateUtil.getDay());
							inOutPd.put("outYear", DateUtil.getYear());
							inOutPd.put("outMonth", DateUtil.getMonth());
							PageData monData = drugOutRecordService.isHaveByCId(inOutPd);
							// 3.如果没有-->插入两条记录
							if (IsNull.paramsIsNull(monData)) {
								PageData mData = new PageData();
								mData.put("drugOutRecordId", this.get32UUID());
								mData.put("clinicId", clinicDrugData.get("clinicId"));
								mData.put("outYear", DateUtil.getYear());
								mData.put("outMonth", DateUtil.getMonth());
								mData.put("outDay", "32");
								mData.put("outDate", DateUtil.getDay());
								mData.put("outWeek", "0");
								mData.put("kind", "1");
								mData.put("number", 1);
								mData.put("units", "种");
								mData.put("clinicdrugId", "");
								mData.put("drugName", "");
								mData.put("manufacturer", "");
								drugOutRecordService.save(mData);// 这个月的
							} else {// 如果月的有-->查询是否这个月有了这条药的记录，判断是否加 1 种药
								inOutPd.put("day", DateUtil.getDay());
								PageData drData = drugOutRecordService.isHaveDrugM(inOutPd);
								if (IsNull.paramsIsNull(drData)) {
									monData.put("num", 1);
									drugOutRecordService.editById(monData);// 修改这个月的
								}
							}

							inOutPd.put("drugOutRecordId", this.get32UUID());
							inOutPd.put("clinicdrugId", clinicDrugData.get("clinicdrugId").toString());
							inOutPd.put("outDay", new SimpleDateFormat("d").format(new Date()));
							inOutPd.put("outWeek", DateUtil.getWeek() + "");
							inOutPd.put("kind", "0");
							inOutPd.put("number", DoubleUtil.douSou(Double.parseDouble(inOutPd.get("num").toString())));
							inOutPd.put("units", clinicDrugData.get("units").toString());
							inOutPd.put("clinicdrugId", clinicDrugData.get("clinicdrugId").toString());
							inOutPd.put("drugName", clinicDrugData.get("sysdrugName").toString());
							inOutPd.put("manufacturer", clinicDrugData.get("manufacturer").toString());
							drugOutRecordService.save(inOutPd);// 这个药的
						} else {// 这个记录+1。。。。。月记录不修改，
							inOutPd.put("drugOutRecordId", dateData.get("drugOutRecordId"));
							inOutPd.put("outWeek", DateUtil.getWeek() + "");
							drugOutRecordService.editById(inOutPd);// 修改这种
						}
					}
				}
				if (IsNull.paramsIsNull(clinicDrugData) == false) {
					double totalPrice = Double.parseDouble(pd.get("sellprice").toString());
					double totalSell = 0;
					double packSellPrice = 0f;// 小价格
					double packSellPriceM = 0f;// 中价格
					if (IsNull.paramsIsNull(clinicDrugData.get("packMiddleNum")) == false) {
						double packMiddleNum = Double.parseDouble(clinicDrugData.get("packMiddleNum").toString());
						packSellPriceM = DoubleUtil.div(totalPrice, (double) packMiddleNum, 2);// 得到中单位价格
						if (IsNull.paramsIsNull(clinicDrugData.get("packSmallNum")) == false) {// 得到小单位价格
							double packSmallNum = Double.parseDouble(clinicDrugData.get("packSmallNum").toString());
							totalSell = packSmallNum * packMiddleNum;
							packSellPrice = DoubleUtil.div(totalPrice, totalSell, 2);
						}
					}
					pd.put("totalSell", totalSell);
					pd.put("packSellPrice", packSellPrice);
					pd.put("packSellPriceM", packSellPriceM);
				}

				// 处理
				pd.put("upinventory", "0");
				pd.put("editTime", DateUtil.getTime());
				drugclinicService.editdrugKC(pd);

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
	 * 添加入库
	 * 
	 * @author 王立飞
	 * @date 2018年8月3日 下午6:03:03
	 */
	public void ruku(PageData data) throws Exception {

		PageData outPd = new PageData();
		outPd.put("drugrecordId", this.get32UUID());
		outPd.put("clinicdrugId", data.get("clinicdrugId"));
		outPd.put("code", data.get("code"));
		outPd.put("sysdrugName", data.get("sysdrugName"));
		outPd.put("spec", data.get("spec"));
		outPd.put("manufacturer", data.get("manufacturer"));
		outPd.put("units", data.get("units"));
		outPd.put("clinicId", data.get("clinicId"));
		outPd.put("num", data.get("num"));
		outPd.put("flag", 1);
		outPd.put("price", data.get("sellprice"));
		outPd.put("createTime", DateUtil.getTime());
		outPd.put("license", data.get("license"));
		outPd.put("goodsType", data.get("goodsType"));
		drugclinicService.clinicjlsave(outPd);
	}
}
