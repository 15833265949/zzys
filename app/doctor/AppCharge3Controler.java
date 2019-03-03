package com.fh.controller.app.doctor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fh.config.CommonConfig;
import com.fh.controller.app.alipaypay.AppPatiPayController;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ChargeService;
import com.fh.service.app.ClinicCheckinDetailService;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.DrugOutRecordService;
import com.fh.service.app.DrugclinicService;
import com.fh.service.app.FriendsService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.NewsService;
import com.fh.service.app.OrderPatiService;
import com.fh.service.app.SortService;
import com.fh.service.app.SysDrugService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.DoubleUtil;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PagingUtil;
import com.fh.util.tools.jPush.examples.jPush;

/**
 * 描述：接诊流程 --新版--第三版
 * 
 * @author 董雪蕊
 * @date 2018-1-15
 */

@Controller
@RequestMapping(value = "/api/charge3")
public class AppCharge3Controler extends BaseController {

	@Resource
	private DrugclinicService drugclinicService;
	@Resource
	private SortService sortService;
	@Resource
	private SysDrugService sysDrugService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private DoctorService doctorService;
	@Resource
	private ClinicCheckinDetailService clinicCheckinDetailService;
	@Resource
	private ChargeService chargeService;
	@Resource
	private ClinicUserService clinicUserService;
	@Resource
	private NewsService newsService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private DayCountService dayCountService;
	@Resource
	private MonthCountService monthCountService;
	@Resource
	private DrugOutRecordService drugOutRecordService;
	@Resource
	private FriendsService friendsService;
	@Resource
	private OrderPatiService orderPatiService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 选药展示默认数据 如果诊所有药，按a-z排序；如果没药，按诊断结果查询
	 * @date 2018年1月15日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/showDrugFirst")
	@ResponseBody
	public Object showDrugFirst() {
		logBefore(logger, "选药展示默认数据");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("pageSize"))
					|| IsNull.paramsIsNull(pd.get("pageIndex"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd = PagingUtil.addPaging(pd);
				int sumAll = drugclinicService.sumAll(pd);
				List<PageData> clinicDrugList = drugclinicService.listByClinicId(pd);
				if (clinicDrugList != null && clinicDrugList.size() > 0) {// 有药直接返回
					map.put("list", clinicDrugList);
					map.put("size", clinicDrugList.size());
				} else if (sumAll == 0) {// 没有，根据诊断结果查询(当诊所库确定没有的时候，即查询页为0没有的时候，若为1，则代表有数据)
					String smallSortName = sortService.stringByResult(pd);
					if (IsNull.paramsIsNull(smallSortName) == false) {
						pd.put("smallSortName", smallSortName);
						List<PageData> sysDrugList = sysDrugService.listBySmallName(pd);
						if (sysDrugList != null && sysDrugList.size() > 0) {
							map.put("list", sysDrugList);
							map.put("size", sysDrugList.size());
						}
					}
				}

				if (IsNull.paramsIsNull(map.get("list"))) {
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
	 * @description 根据输入框搜索查询药品（可根据药品名称、全拼、简拼、小类型）-->先搜诊所库，如果诊所库里没有药，用系统库
	 * @date 2018年1月16日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/searchByInput")
	@ResponseBody
	public Object searchByInput() {
		logBefore(logger, "根据输入框搜索查询药品");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("inputContent"))
					|| IsNull.paramsIsNull(pd.get("pageSize")) || IsNull.paramsIsNull(pd.get("pageIndex"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd = PagingUtil.addPaging(pd);
				// 先查询自己的诊所药库里是不是有数据
				int sumAll = drugclinicService.intBySearInput(pd);
				List<PageData> drugList = drugclinicService.listBySearInput(pd);
				if (drugList != null && drugList.size() > 0) {
					map.put("list", drugList);
					map.put("size", drugList.size());
				} else if (sumAll == 0) {
					List<PageData> sysList = sysDrugService.listBySearInput(pd);
					if (sysList != null && sysList.size() > 0) {
						map.put("list", sysList);
						map.put("size", sysList.size());
					}
				}

				if (IsNull.paramsIsNull(map.get("list"))) {
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
	 * @description 根据选择的类型查询药品
	 * @date 2018年1月17日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/searBySort")
	@ResponseBody
	public Object searBySort() {
		logBefore(logger, "根据选择的类型查询药品");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("pageSize"))
					|| IsNull.paramsIsNull(pd.get("pageIndex"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd = PagingUtil.addPaging(pd);
				int sumAll = drugclinicService.intByClIdAndSort(pd);
				List<PageData> clinicDrugList = drugclinicService.listByClIdAndSort(pd);
				if (clinicDrugList != null && clinicDrugList.size() > 0) {// 有药直接返回
					map.put("list", clinicDrugList);
					map.put("size", clinicDrugList.size());
				} else if (sumAll == 0) {// 没有，根据诊断结果查询
					List<PageData> sysDrugList = sysDrugService.listBySort(pd);
					if (sysDrugList != null && sysDrugList.size() > 0) {
						map.put("list", sysDrugList);
						map.put("size", sysDrugList.size());
					}
				}

				if (IsNull.paramsIsNull(map.get("list"))) {
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
	 * 描述：医生完成接诊 （新增患者-2018.10.7）
	 * 
	 * @author 董雪蕊
	 * @date 2018年1月17日
	 * @version 1.01
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/completeDiagnosis", method = RequestMethod.POST)
	@ResponseBody
	public Object completeDiagnosis() {
		logBefore(logger, "医生完成接诊");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("checkinId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "您还有项没有填写";
			} else {
				// ***1.先保存到医嘱药详细表
				// 1）先删除之前保存 药品明细 的数据 ，怕返回再保存，有重复数据
				clinicCheckinDetailService.delByChinId(pd);
				String moneyWe = drugListSave(pd.get("xDrugList"), "xList", pd.get("checkinId").toString(), 1);
				String moneyCh = drugListSave(pd.get("zDrugList"), "zList", pd.get("checkinId").toString(), 0);
				pd.put("moneyWe", moneyWe);
				pd.put("moneyCh", moneyCh);
				pd.put("money", DoubleUtil.add(Double.parseDouble(moneyWe), Double.parseDouble(moneyCh)));
				// ***2.再保存到医嘱表
				pd.put("state", 2);
				pd.put("createTime", DateUtil.getTime());
				PageData doctorInfo = doctorService.findById(pd);
				pd.put("visible", doctorInfo.get("visible"));
				if (IsNull.paramsIsNull(pd.get("xDrugList"))) {
					pd.put("xDrugList", "");
				}
				if (IsNull.paramsIsNull(pd.get("zDrugList"))) {
					pd.put("zDrugList", "");
				}
				if (IsNull.paramsIsNull(doctorInfo.get("trueName")) == false) {
					pd.put("trueName", doctorInfo.get("trueName").toString());
				} else {
					pd.put("trueName", "");
				}
				clinic_checkinService.saveYZ(pd);// （下医嘱）
				// 查询挂号详情
				PageData checkInData = clinic_checkinService.findByidxq(pd);
				// 为两个人加上好友关系
				if (IsNull.paramsIsNull(checkInData.get("clinicId")) == false
						&& IsNull.paramsIsNull(checkInData.get("doctorId")) == false
						&& IsNull.paramsIsNull(checkInData.get("patientId")) == false) {
					addFriend(checkInData.get("clinicId").toString(), checkInData.get("doctorId").toString(),
							checkInData.get("patientId").toString());
				}
				// 更新医生接诊总数 和诊所接诊总数 都放在所有的流程都接完
				allCount(checkInData);
				// 保存到收费表 为了适配旧版
				PageData chargData = chargeService.findByID(pd);
				if (IsNull.paramsIsNull(chargData)) {
					CreateCharge(map, pd, doctorInfo);
					result = "0000";
					message = "提交成功";
				} else {
					chargeService.updateCharge2(pd);
					map.put("chargeId", chargData.get("chargeId"));
					result = "0000";
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

	private void CreateCharge(Map<String, Object> map, PageData pd, PageData doctorInfo) throws Exception {
		PageData findByidxq = clinic_checkinService.findByidxq(pd);// 得到接诊记录信息
		// 添加收费信息--------------------
		PageData charg = new PageData();
		try {
			charg.put("pdNum", findByidxq.get("number"));
		} catch (Exception e) {
			pd.put("time", DateUtil.getDay() + " 01:00:00");
			pd.put("pdNum", "T");
			PageData findBynum = chargeService.findBynum(pd);
			String snum = "";
			if (findBynum != null) {
				snum = findBynum.get("pdNum").toString();
				snum = snum.replaceAll("T", "");
				charg.put("pdNum", "T" + Integer.parseInt(snum) + 1);
			} else {
				charg.put("pdNum", "T1");
			}

		}
		charg.put("chargeId", this.get32UUID());
		charg.put("checkinId", pd.get("checkinId"));
		charg.put("clinicId", pd.get("clinicId"));
		charg.put("clinicName", findByidxq.get("clinicName"));

		if (IsNull.paramsIsNull(findByidxq.get("doctorprescribe"))) {
			charg.put("doctorprescribe", "");
		} else {
			charg.put("doctorprescribe", findByidxq.get("doctorprescribe"));// 接诊人姓名
		}
		if (IsNull.paramsIsNull(findByidxq.get("doctorName"))) {
			charg.put("resultdoctor", doctorInfo.get("trueName"));// 接诊人姓名
		} else {
			charg.put("resultdoctor", findByidxq.get("doctorName"));// 接诊人姓名
		}
		if (IsNull.paramsIsNull(findByidxq.get("patientId")) == false) {
			charg.put("patientId", findByidxq.get("patientId"));
		} else {
			charg.put("patientId", "");
		}
		if (IsNull.paramsIsNull(findByidxq.get("headUrl"))) {
			charg.put("headUrl", "");
		} else {
			charg.put("headUrl", findByidxq.get("headUrl"));
		}
		charg.put("patientName", findByidxq.get("patientName"));
		charg.put("chargeState", 0);
		charg.put("medicameState", 0);
		charg.put("patientVisit", findByidxq.get("patientVisit"));
		charg.put("pAge", findByidxq.get("patientAge"));
		charg.put("pSex", findByidxq.get("patientSex"));
		if (IsNull.paramsIsNull(pd.get("xDrugList"))) {
			charg.put("xDrugList", " ");
		} else {
			charg.put("xDrugList", pd.get("xDrugList"));
		}
		if (IsNull.paramsIsNull(pd.get("zDrugList"))) {
			charg.put("zDrugList", " ");
		} else {
			charg.put("zDrugList", pd.get("zDrugList"));
		}
		charg.put("createTime", DateUtil.getTime());
		chargeService.save(charg);
		// -----------------------------以上
		// 添加消息表数据====================================================
		PageData pdf = new PageData();
		pdf.put("title", "未付费提醒");
		pdf.put("type", 13);
		String sex = "";
		if ("0".equals(findByidxq.get("patientSex").toString())) {
			sex = "女";
		} else {
			sex = "男";
		}
		String cfz = "";
		if ("0".equals(findByidxq.get("patientVisit").toString())) {
			cfz = "初诊";
		} else {
			cfz = "复诊";
		}
		pdf.put("messageContent", "患者：" + findByidxq.get("patientName") + " " + sex + " " + findByidxq.get("patientAge")
				+ " " + cfz + " 未交费用，请查看具体情况");
		pdf.put("fromRoleFlag", 0);
		pdf.put("skitId", pd.get("checkinId"));
		pdf.put("toRoleFlag", 1);
		pdf.put("creatDate", DateUtil.getDay());
		pdf.put("creatTime", DateUtil.getTime());
		List<PageData> findcylist = clinicUserService.findcylist(pd);
		PageData ddr = new PageData();
		for (int i = 0; i < findcylist.size(); i++) {
			ddr = findcylist.get(i);
			if (ddr.get("authPlat").toString().equals("6") || ddr.get("authPlat").toString().equals("3")
					|| ddr.get("roleFlag").toString().equals("1")) {
				pdf.put("toUserId", ddr.get("doctorId"));
				pdf.put("headUrlNew", ddr.get("headUrl"));
				newsService.save(pdf);
			}
		}
		// 添加消息表数据====================================================
		// 判断医生平台权限 平台权限 0.没有权限 1.医生 2.医生、收费员 3.收费员 4.收费员、药剂员 5.药剂员
		// 6.药库管理员
		int authPlat = Integer.parseInt(doctorInfo.get("authPlat").toString());
		int roleFlag = Integer.parseInt(doctorInfo.get("roleFlag").toString());
		if (authPlat == 2 || roleFlag == 1 || authPlat == 6) {
			map.put("chargeId", charg.get("chargeId"));
		}
	}

	/**
	 * 去存挂号药品详情
	 * 
	 * @return
	 * @throws Exception
	 */
	public String drugListSave(Object drugList, String listName, String checkinId, int isWe) throws Exception {
		String money = "0.00";
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
					} else {
						dataPd.put("buyUnit", data.get("units"));
					}
					dataPd.put("checkinId", checkinId);
					dataPd.put("isWe", isWe);
					clinicCheckinDetailService.save(dataPd);
				}

			}
		}

		return money;
	}

	/**
	 * 将所有的医生 就诊数量+1，重复的医生不加 将诊所的接诊量+1 将所有医生加上就诊记录 添加后台统计
	 * 
	 * @throws Exception
	 */
	public void allCount(PageData checkInPd) throws Exception {
		// 1.先把诊所的就诊量加上1
		PageData clinicPd = new PageData();
		clinicPd.put("patientAll", 1);
		clinicPd.put("clinicId", checkInPd.get("clinicId"));
		if (IsNull.paramsIsNull(checkInPd.get("patientId")) == false) {// 患者数据
			PageData havePageData = clinic_checkinService.getIsHave(checkInPd);
			if (IsNull.paramsIsNull(havePageData)) {
				clinicPd.put("patientNum", 1);
			}
		}
		clinicService.updClinNum(clinicPd);
		// 2.给所有医生加上数量，然后加上记录
		Set<String> docIdSet = new HashSet<>();
		if (IsNull.paramsIsNull(checkInPd.get("doctorId")) == false) {// 接诊医生
			docIdSet.add(checkInPd.get("doctorId").toString());
		}
		if (IsNull.paramsIsNull(checkInPd.get("chargeDoctorId")) == false) {// 收费医生
			docIdSet.add(checkInPd.get("chargeDoctorId").toString());
		}
		if (IsNull.paramsIsNull(checkInPd.get("drugDoctorId")) == false) {// 拿药医生
			docIdSet.add(checkInPd.get("drugDoctorId").toString());
		}
		if (docIdSet.size() > 0) {
			for (String doctorId : docIdSet) {
				PageData docPd = new PageData();
				docPd.put("doctorId", doctorId);
				PageData docData = doctorService.findById(docPd);
				// 修改个人接诊数量
				int encounters = Integer.parseInt(docData.get("encounters").toString());
				docData.put("encounters", encounters + 1);
				doctorService.editDoctorInfo(docData);
				// 添加记录
				addRecord(checkInPd, doctorId);
			}
		}
	}

	public void addRecord(PageData checkInPd, String doctorId) throws Exception {
		String str = checkInPd.get("patientVisit").toString();
		int flag = 3;
		if (str.equals("1")) {
			flag = 1;
		} else if (str.equals("0")) {
			flag = 0;
		}
		PageData dd = new PageData();
		dd.put("year", DateUtil.getYear());
		dd.put("clinicId", checkInPd.get("clinicId"));
		dd.put("doctorId", doctorId);
		dd.put("month", DateUtil.getMonth());
		dd.put("day", DateUtil.getDay1());
		dd.put("hour", "hour" + DateUtil.getNowHour());// 不是数据库字段，只是为了添加时使用
		// 查询诊所当月
		PageData findClinicsy2 = monthCountService.findClinicsy2(dd);// 诊所月
		PageData findminthsj = monthCountService.findminthsj1(dd);// 个人月
		PageData findDaysj = dayCountService.findDaysj(dd);// 个人天数据
		PageData findDaysj2 = dayCountService.findDaysj2(dd);// 诊所天数据
		if (findClinicsy2 != null) {// 诊所月
			// 不为空更新诊所月
			dd = AppPatiPayController.editMonth(flag, findClinicsy2);
			dd.put("doctorId", doctorId);
			dd.put("cmonthId", findClinicsy2.get("cmonthId"));
			monthCountService.updateCount2(dd);
			if (findminthsj != null) {// 个人月
				// 不为空更新个人月
				dd = AppPatiPayController.editMonth(flag, findminthsj);
				dd.put("doctorId", doctorId);
				dd.put("monthId", findminthsj.get("monthId"));
				monthCountService.updateCount1(dd);
				if (findDaysj2 != null) {
					// 不为空更新诊所天
					dd = AppPatiPayController.editDay(flag, findDaysj2);
					dd.put("doctorId", doctorId);
					dd.put("cdayId", findDaysj2.get("cdayId"));
					dayCountService.updateCount2(dd);
					if (findDaysj != null) {// 不为空
						// 不为空更新个人天
						dd = AppPatiPayController.editDay(flag, findDaysj);
						dd.put("doctorId", doctorId);
						dd.put("dayId", findDaysj.get("dayId"));
						dayCountService.updateCount(dd);
					} else {// 为空
							// 个人天为空添加
						if (flag == 1) {
							dd.put("fuCount", 1);
							dd.put("sumCount", 1);
						} else if (flag == 0) {
							dd.put("chuCount", 1);
							dd.put("sumCount", 1);
						}
						dd.put("dayId", this.get32UUID());
						dd.put("doctorId", doctorId);
						dd.put("clinicId", checkInPd.get("clinicId"));
						dd.put("hour", "hour" + DateUtil.getNowHour());
						dayCountService.save(dd);
					}
				} else {// 为空
						// 诊所天为空添加 个人 诊所
					if (flag == 1) {
						dd.put("fuCount", 1);
						dd.put("sumCount", 1);
					} else if (flag == 0) {
						dd.put("chuCount", 1);
						dd.put("sumCount", 1);
					}
					dd.put("hour", "hour" + DateUtil.getNowHour());
					dd.put("dayId", this.get32UUID());
					dd.put("doctorId", doctorId);
					dd.put("clinicId", checkInPd.get("clinicId"));
					dayCountService.save(dd);
					dd.put("cdayId", this.get32UUID());
					dayCountService.save2(dd);
				}
			} else {
				// 个人月为空
				// 1.查询个人月的诊所年数据 2.新增月并把年数据带过来新增
				PageData findminthyear = monthCountService.findminthyear(dd);
				int chuYearCount = 0;
				int fuYearCount = 0;
				int sumYearCount = 0;
				if (findminthyear != null) {
					chuYearCount = Integer.parseInt(findminthyear.get("chuYearCount").toString());
					fuYearCount = Integer.parseInt(findminthyear.get("fuYearCount").toString());
					sumYearCount = Integer.parseInt(findminthyear.get("sumYearCount").toString());
				}

				if (flag == 1) {
					dd.put("fuYearCount", fuYearCount + 1);
					dd.put("chuYearCount", chuYearCount);
					dd.put("sumYearCount", sumYearCount + 1);
					dd.put("fuMonthCount", 1);
					dd.put("sumMonthCount", 1);
				} else {
					dd.put("fuYearCount", fuYearCount);
					dd.put("chuYearCount", chuYearCount + 1);
					dd.put("sumYearCount", sumYearCount + 1);
					dd.put("chuMonthCount", 1);
					dd.put("sumMonthCount", 1);
				}
				dd.put("monthId", this.get32UUID());
				dd.put("doctorId", doctorId);
				dd.put("clinicId", checkInPd.get("clinicId"));
				monthCountService.save1(dd);
				// 2判断诊所天为不为空
				if (findDaysj2 != null) {
					// 不为空更新诊所天
					dd = AppPatiPayController.editDay(flag, findDaysj2);
					dd.put("doctorId", doctorId);
					dd.put("clinicId", checkInPd.get("clinicId"));
					dd.put("cdayId", findDaysj2.get("cdayId"));
					dayCountService.updateCount2(dd);
					// 个人天为空添加
					if (flag == 1) {
						dd.put("fuCount", 1);
						dd.put("sumCount", 1);
					} else if (flag == 0) {
						dd.put("chuCount", 1);
						dd.put("sumCount", 1);
					}
					dd.put("dayId", this.get32UUID());
					dd.put("hour", "hour" + DateUtil.getNowHour());
					dayCountService.save(dd);

				} else {// 为空
						// 诊所天为空添加 个人 诊所
					if (flag == 1) {
						dd.put("fuCount", 1);
						dd.put("sumCount", 1);
					} else if (flag == 0) {
						dd.put("chuCount", 1);
						dd.put("sumCount", 1);
					}
					dd.put("hour", "hour" + DateUtil.getNowHour());
					dd.put("dayId", this.get32UUID());
					dd.put("doctorId", doctorId);
					dd.put("clinicId", checkInPd.get("clinicId"));
					dayCountService.save(dd);
					dd.put("cdayId", this.get32UUID());
					dayCountService.save2(dd);
				}
			}

		} else {
			// 诊所月为空
			// 1.查询上个月的诊所年数据 2.新增月并把年数据带过来新增
			int chuYearCount2 = 0;
			int fuYearCount2 = 0;
			int sumYearCount2 = 0;
			PageData findClinicyear = monthCountService.findClinicyear(dd);
			if (findClinicyear != null) {
				chuYearCount2 = Integer.parseInt(findClinicyear.get("chuYearCount").toString());
				fuYearCount2 = Integer.parseInt(findClinicyear.get("fuYearCount").toString());
				sumYearCount2 = Integer.parseInt(findClinicyear.get("sumYearCount").toString());
			}
			if (flag == 1) {
				dd.put("fuYearCount", fuYearCount2 + 1);
				dd.put("chuYearCount", chuYearCount2);
				dd.put("sumYearCount", sumYearCount2 + 1);
				dd.put("fuMonthCount", 1);
				dd.put("sumMonthCount", 1);
			} else {
				dd.put("fuYearCount", fuYearCount2);
				dd.put("chuYearCount", chuYearCount2 + 1);
				dd.put("sumYearCount", sumYearCount2 + 1);
				dd.put("chuMonthCount", 1);
				dd.put("sumMonthCount", 1);
			}
			dd.put("cmonthId", this.get32UUID());
			monthCountService.save2(dd);
			// 3.查询个人月的诊所年数据 4.新增月并把年数据带过来新增
			PageData findminthyear = monthCountService.findminthyear(dd);
			int chuYearCount = 0;
			int fuYearCount = 0;
			int sumYearCount = 0;
			if (findminthyear != null) {
				chuYearCount = Integer.parseInt(findminthyear.get("chuYearCount").toString());
				fuYearCount = Integer.parseInt(findminthyear.get("fuYearCount").toString());
				sumYearCount = Integer.parseInt(findminthyear.get("sumYearCount").toString());
			}
			if (flag == 1) {
				dd.put("fuYearCount", fuYearCount + 1);
				dd.put("chuYearCount", chuYearCount);
				dd.put("sumYearCount", sumYearCount + 1);
				dd.put("fuMonthCount", 1);
				dd.put("sumMonthCount", 1);
			} else {
				dd.put("fuYearCount", fuYearCount);
				dd.put("chuYearCount", chuYearCount + 1);
				dd.put("sumYearCount", sumYearCount + 1);
				dd.put("chuMonthCount", 1);
				dd.put("sumMonthCount", 1);
			}
			dd.put("monthId", this.get32UUID());
			monthCountService.save1(dd);

			// 5.新增诊所个人天数据
			if (flag == 1) {
				dd.put("fuCount", 1);
				dd.put("sumCount", 1);
			} else if (flag == 0) {
				dd.put("chuCount", 1);
				dd.put("sumCount", 1);
			}
			dd.put("hour", "hour" + DateUtil.getNowHour());
			dd.put("dayId", this.get32UUID());
			dayCountService.save(dd);
			dd.put("cdayId", this.get32UUID());
			dayCountService.save2(dd);
		}

	}

	/**
	 * 完成收费的方法 doctorId checkinId money chargeState moneyCh moneyWe
	 * 
	 * @param pd
	 * @param isPush 0不用走推送药品 1用走推送药品
	 * @return
	 */
	public String completeCharge(PageData pd, int isPush, PageData checkData) throws Exception {
		String medicamentId = "";
		// 1.修改挂号表数据
		PageData szd = new PageData();
		// 放收银医生名称
		int chargeState = 1;
		PageData doctorInfo = doctorService.findDoctorInfo(pd);
		szd.put("chargeDoctorId", pd.get("doctorId"));
		szd.put("chargeDoctorName", doctorInfo.get("trueName"));
		szd.put("checkinId", pd.get("checkinId"));
		szd.put("money", pd.get("money"));
		szd.put("chargeState", chargeState);
		szd.put("moneyCh", pd.get("moneyCh"));
		szd.put("moneyWe", pd.get("moneyWe"));
		szd.put("chargeTime", DateUtil.getTime());
		// 修改挂号表
		chargeService.updateCharge2(szd);
		// 2.修改收费拿药信息
		PageData chargeData = chargeService.findByID(pd);
		szd.put("chargeId", chargeData.get("chargeId"));
		szd.put("chargeTime", DateUtil.getTime());
		szd.put("charDoctorId", pd.get("doctorId"));
		szd.put("charDoctorName", doctorInfo.get("trueName"));
		szd.put("medFlag", "1");
		szd.put("medTime", DateUtil.getTime());
		szd.put("medDoctorName", doctorInfo.get("trueName"));
		szd.put("medDoctorId", doctorInfo.get("doctorId"));
		chargeService.updateCharge(szd);

		// 4.修改 推送消息状态----将未发送的消息删除
		PageData pd2 = new PageData();
		pd2.put("skitId", chargeData.get("checkinId"));
		pd2.put("type", 13);
		newsService.delNoPush(pd2);

		// 5. 添加消息表数据====================================================
		PageData pdf = new PageData();
		pdf.put("title", "未拿药提醒");
		pdf.put("type", 14);

		String sex = "";
		if ("0".equals(chargeData.get("pSex").toString())) {
			sex = "女";
		} else {
			sex = "男";
		}
		String cfz = "";
		if ("0".equals(chargeData.get("patientVisit").toString())) {
			cfz = "初诊";
		} else {
			cfz = "复诊";
		}

		String messageConet = "患者：" + chargeData.get("patientName") + " " + sex + " " + chargeData.get("pAge") + " "
				+ cfz + " 未领取药，请查看具体情况";
		pdf.put("skitId", chargeData.get("checkinId"));
		pdf.put("messageContent", messageConet);
		pdf.put("fromRoleFlag", 0);
		pdf.put("toRoleFlag", 1);
		pdf.put("creatDate", DateUtil.getDay());
		pdf.put("creatTime", DateUtil.getTime());

		List<PageData> findcylist = clinicUserService.findcylist(pd);
		PageData ddr = new PageData();
		for (int i = 0; i < findcylist.size(); i++) {
			ddr = findcylist.get(i);
			if ("6".equals(ddr.get("authPlat").toString()) || "5".equals(ddr.get("authPlat").toString())
					|| "4".equals(ddr.get("authPlat").toString()) || "1".equals(ddr.get("roleFlag").toString())) {
				pdf.put("toUserId", ddr.get("doctorId"));
				pdf.put("headUrlNew", ddr.get("headUrl"));
				newsService.save(pdf);
			}

		}

		// 添加消息表数据====================================================
		// 7.判断医生平台权限 平台权限 0.没有权限 1.医生 2.医生、收费员 3.收费员 4.收费员、药剂员 5.药剂员
		// 6.药库管理员
		PageData findDoctorInfo = doctorService.findDoctorInfo(pd);
		int authPlat = Integer.parseInt(findDoctorInfo.get("authPlat").toString());
		int roleFlag = Integer.parseInt(findDoctorInfo.get("roleFlag").toString());
		if (authPlat == 4 || roleFlag == 1 || authPlat == 6) {
			medicamentId = chargeData.get("chargeId").toString();
		}

		return medicamentId;

	}

	/**
	 * 完成拿药
	 * 
	 * @param pd medicamentId doctorId
	 * @throws Exception
	 */
	public void completeMedicine(PageData pd, PageData checkInData) throws Exception {
		if (IsNull.paramsIsNull(checkInData)) {
			// 查询挂号详情
			checkInData = clinic_checkinService.findByidxq(pd);
		}
		// 查询收费信息
		PageData mediData = chargeService.findByID(pd);
		if (mediData != null) {

			// 更新拿药信息
			pd.put("chargeId", mediData.get("chargeId"));
			pd.put("medFlag", "1");
			PageData doctorInfo = doctorService.findDoctorInfo(pd);
			pd.put("medTime", DateUtil.getTime());
			pd.put("medDoctorName", doctorInfo.get("trueName"));
			pd.put("medDoctorId", doctorInfo.get("doctorId"));
			chargeService.updateCharge(pd);

			// 修改 推送消息状态
			PageData pd2 = new PageData();
			pd2.put("skitId", mediData.get("checkinId"));
			pd2.put("type", 14);
			newsService.delNoPush(pd2);

			// 更新问诊表
			PageData szd = new PageData();
			szd.put("checkinId", mediData.get("checkinId"));
			szd.put("medicameState", pd.get("medFlag"));
			szd.put("drugDoctorId", pd.get("doctorId"));
			szd.put("drugDoctorName", doctorInfo.get("trueName"));
			szd.put("drugTime", DateUtil.getTime());
			chargeService.updateCharge2(szd);

			// 实时库存减少
			PageData reducePd = new PageData();
			reducePd.put("clinicName", checkInData.get("clinicName"));
			reducePd.put("outPatientId", checkInData.get("patientId"));
			reducePd.put("outPatientName", checkInData.get("patientName"));
			reducePd.put("outDoctorId", checkInData.get("doctorId"));
			reducePd.put("outDoctorName", checkInData.get("doctorName"));
			reduceIn(checkInData, reducePd);// 实时减少库存

			// 资金收益
			mediData.put("money", DoubleUtil.douSou(Double.parseDouble(mediData.get("money").toString())));
			double money = Double.parseDouble(mediData.get("money").toString());
			double moneyCh = 0;
			double moneyWe = 0;
			String clinicId = mediData.get("clinicId").toString();
			if (IsNull.paramsIsNull(mediData.get("moneyCh")) == false) {
				moneyCh = Double.parseDouble(mediData.get("moneyCh").toString());
			}
			if (IsNull.paramsIsNull(mediData.get("moneyWe")) == false) {
				moneyWe = Double.parseDouble(mediData.get("moneyWe").toString());
			}
			moneyIn(clinicId, moneyCh, moneyWe, money);// 统计资金收益 走接口 app
		}
	}

	/**
	 * 在所有的接诊流程走完后，添加好友,加1条数据 from 是医生 to 是患者
	 * 添加vip逻辑，就是如果诊所数量超过了100个，这个数据的类型isNoVipHidden为隐藏
	 * 
	 * @throws Exception
	 */
	public void addFriend(String clinicId, String doctorId, String patientId) throws Exception {
		PageData pd = new PageData();
		pd.put("clinicId", clinicId);
		pd.put("fromUserId", doctorId);
		pd.put("toUserId", patientId);
		PageData data = friendsService.findIsFriend(pd);
		if (IsNull.paramsIsNull(data)) {// 先查询诊所的好友是不是超过100了
			PageData couData = friendsService.countClinFri(pd);
			if (IsNull.paramsIsNull(couData) == false && IsNull.paramsIsNull(couData.get("cou")) == false) {
				int cou = Integer.parseInt(couData.get("cou").toString());
				if (cou >= CommonConfig.friendNum) {
					pd.put("isNoVipHidden", 1);
					pushGuan(pd);
				}
			}

			pd.put("friendId", this.get32UUID());
			pd.put("fromRoleFlag", 1);
			pd.put("toRoleFlag", 2);
			pd.put("friendRemark", "");
			pd.put("createTime", DateUtil.getTime());
			pd.put("flag", 1);
			friendsService.save(pd);
		}
	}

	/**
	 * 推送给管理员，好友数量到上线了 一天只能推一条
	 * 
	 * @throws Exception
	 */
	public void pushGuan(PageData pd) throws Exception {
		// 查询推给谁
		List<PageData> pushList = new ArrayList<>();
		PageData clinicData = clinicService.findzsClinic(pd);
		if (IsNull.paramsIsNull(clinicData) == false) {
			// 如果是vip 就返回
			String now = DateUtil.getTime();
			if (IsNull.paramsIsNull(clinicData.get("vipEndTime")) == false
					&& DateUtil.compareDate(clinicData.get("vipEndTime").toString(), now)) {
				return;
			}
			// 不是vip continue
			if (IsNull.paramsIsNull(clinicData.get("doctorId")) == false
					&& Integer.parseInt(clinicData.get("state").toString()) == 1) {
				PageData guanData = doctorService.findById(clinicData);
				pushList.add(guanData);
			} else {
				pushList = doctorService.findFriend(pd);
			}

		}

		// 每一个都去推送
		PageData news = new PageData();
		news.put("title", "患者上限提醒");
		news.put("type", 25);
		news.put("flag", 1);
		news.put("messageContent", "您的诊所患者数量已超到100位。不是vip用户只显示100位患者，如果想查看所有患者，请升级vip。");
		news.put("fromUserId", "0");
		news.put("fromRoleFlag", 0);
		news.put("toRoleFlag", 1);
		news.put("creatDate", DateUtil.getDay());
		news.put("creatTime", DateUtil.getTime());
		news.put("sysdrugName", "");
		news.put("skitId", "");
		news.put("headUrlNew", "");
		news.put("huoDongUrl", "");

		if (pushList != null && pushList.size() > 0) {
			for (int i = 0; i < pushList.size(); i++) {
				PageData pData = pushList.get(i);
				if (IsNull.paramsIsNull(pData) == false) {
					// 先查询今天是否推送了
					pData.put("toUserId", pData.get("doctorId"));
					PageData newsData = newsService.friPushGuan(pData);
					if (IsNull.paramsIsNull(newsData)) {
						news.put("toUserId", pData.getString("doctorId"));
						newsService.save(news);
						Collection<String> alias = new ArrayList<>();
						Collection<String> registrationId = new ArrayList<>();
						registrationId.add(pData.get("phoneSole").toString());
						String toUserId = pData.get("doctorId").toString();
						alias.add(toUserId);

						String phoneSole = "";
						String huaWeiToken = "";
						String miRegId = "";
						String mzPushId = "";
						phoneSole = pData.getString("phoneSole");
						huaWeiToken = pData.getString("huaWeiToken");
						miRegId = pData.getString("miRegId");
						mzPushId = pData.getString("mzPushId");

						jPush.sendAll(alias, registrationId, "患者上限提醒", "25", news.getString("messageContent"), "", "0",
								toUserId, "1", DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken,
								miRegId, mzPushId, phoneSole);
					}

				}
			}
		}

	}

	/**
	 * 统计资金收益
	 * 
	 * @throws Exception
	 */
	public void moneyIn(String clinicId, double inMoneyCh, double inMoneyWe, double inMoney) throws Exception {
		// 加数据
		PageData pd = new PageData();
		pd.put("clinicId", clinicId);
		String day = DateUtil.getDay();// 2017-01-01
		pd.put("year", day.substring(0, 4));
		pd.put("month", day.substring(5, 7));
		pd.put("inMoneyCh", inMoneyCh);
		pd.put("inMoneyWe", inMoneyWe);
		pd.put("inMoney", inMoney);
		pd.put("inMoneyYear", inMoney);
		pd.put("inMoneyChYear", inMoneyCh);
		pd.put("inMoneyWeYear", inMoneyWe);
		pd.put("day", day.substring(5));
		// 处理逻辑
		PageData dayData = dayCountService.findDaysj2(pd);
		// 天数据
		if (IsNull.paramsIsNull(dayData) == false) {
			pd.put("cdayId", dayData.get("cdayId"));
			pd.put("inMoneyCh", DoubleUtil.add(inMoneyCh, Double.parseDouble(dayData.get("inMoneyCh").toString())));
			pd.put("inMoneyWe", DoubleUtil.add(inMoneyWe, Double.parseDouble(dayData.get("inMoneyWe").toString())));
			pd.put("inMoney", DoubleUtil.add(inMoney, Double.parseDouble(dayData.get("inMoney").toString())));
			dayCountService.updateCount2(pd);
		}
		// 月数据
		PageData monData = monthCountService.findClinicsy2(pd);
		if (IsNull.paramsIsNull(monData) == false) {// 必不为空
			pd.put("cmonthId", monData.get("cmonthId"));
			pd.put("inMoneyCh", DoubleUtil.add(inMoneyCh, Double.parseDouble(monData.get("inMoneyCh").toString())));
			pd.put("inMoneyWe", DoubleUtil.add(inMoneyWe, Double.parseDouble(monData.get("inMoneyWe").toString())));
			pd.put("inMoney", DoubleUtil.add(inMoney, Double.parseDouble(monData.get("inMoney").toString())));
			pd.put("inMoneyYear", DoubleUtil.add(inMoney, Double.parseDouble(monData.get("inMoneyYear").toString())));
			pd.put("inMoneyChYear",
					DoubleUtil.add(inMoneyCh, Double.parseDouble(monData.get("inMoneyChYear").toString())));
			pd.put("inMoneyWeYear",
					DoubleUtil.add(inMoneyWe, Double.parseDouble(monData.get("inMoneyWeYear").toString())));
			monthCountService.updateCount2(pd);
		}

	}

	/**
	 * 实时减少库存
	 * 
	 * @param pd checkinId
	 * @throws Exception
	 */
	public void reduceIn(PageData pd, PageData reducePd) throws Exception {
		List<PageData> drugList = clinicCheckinDetailService.listByClinic(pd);
		for (int i = 0; i < drugList.size(); i++) {
			PageData drugData = drugList.get(i);
			String clinicdrugId = drugData.getString("clinicdrugId");
			int packSellFlag = Integer.parseInt(drugData.get("packSellFlag").toString());
			int count = Integer.parseInt(drugData.get("count").toString());
			pd.put("clinicdrugId", clinicdrugId);
			// 查询药品信息
			PageData data = drugclinicService.findById(pd);
			if (IsNull.paramsIsNull(data) == false) {
				// 1.先减少诊所药库的库存量
				double inventory = Double.parseDouble(data.get("inventory").toString());
				double upinventory = Double.parseDouble(data.get("upinventory").toString());

				if (inventory < 0.1) {
					break;
				}

				double reduceNumBig = 0f;
				switch (packSellFlag) {
				case 0:// 旧的，应该没有选这个的
					reduceNumBig = count;
					break;
				case 1:// 小单位
					double packMiddleNum = Double.parseDouble(data.get("packMiddleNum").toString());
					double packSmallNum = Double.parseDouble(data.get("packSmallNum").toString());
					reduceNumBig = DoubleUtil.div((double) count, packSmallNum * packMiddleNum, 10);
					break;
				case 2:// 中单位
					double packMiddleNum2 = Double.parseDouble(data.get("packMiddleNum").toString());
					reduceNumBig = DoubleUtil.div((double) count, packMiddleNum2, 10);
					break;
				case 3:// 大单位
					reduceNumBig = count;
					break;
				default:

				}
				// 将库里库存改了
				if (upinventory >= reduceNumBig) {// 以前的库存大于此次出的药，就减少
					upinventory = DoubleUtil.sub(upinventory, reduceNumBig);
				} else if (upinventory > 0 && upinventory < reduceNumBig) {
					double sheng = DoubleUtil.sub((double) count, upinventory);
					if (inventory > sheng) {
						upinventory = 0;
						inventory = DoubleUtil.sub(inventory, sheng);
					} else {
						upinventory = 0;
						inventory = 0;
					}

				} else if (inventory > reduceNumBig) {
					inventory = DoubleUtil.sub(inventory, reduceNumBig);
				} else {
					inventory = 0;
					upinventory = 0;
				}
				data.put("inventory", inventory + "");
				data.put("upinventory", upinventory + "");
				drugclinicService.editdrugKC(data);

				// 2.添加出库记录
				PageData outPd = new PageData();
				outPd.put("drugrecordId", this.get32UUID());
				outPd.put("clinicdrugId", clinicdrugId);
				outPd.put("code", data.get("code"));
				outPd.put("sysdrugName", data.get("sysdrugName"));
				outPd.put("spec", data.get("spec"));
				outPd.put("manufacturer", data.get("manufacturer"));
				outPd.put("units", data.get("units"));
				outPd.put("clinicId", data.get("clinicId"));
				outPd.put("num", reduceNumBig);
				outPd.put("flag", 2);
				outPd.put("price", data.get("sellprice"));
				outPd.put("createTime", DateUtil.getTime());
				outPd.put("license", data.get("license"));
				outPd.put("clinicName", reducePd.get("clinicName"));
				outPd.put("outPatientId", reducePd.get("outPatientId"));
				outPd.put("outPatientName", reducePd.get("outPatientName"));
				outPd.put("outDoctorId", reducePd.get("outDoctorId"));
				outPd.put("outDoctorName", reducePd.get("outDoctorName"));
				outPd.put("goodsType", data.get("goodsType"));
				drugclinicService.clinicjlsave(outPd);

				// 3.添加出库总统计
				PageData outAllPd = new PageData();
				outAllPd.put("clinicId", data.get("clinicId"));
				outAllPd.put("outYear", DateUtil.getYear());
				outAllPd.put("outMonth", DateUtil.getMonth());
				outAllPd.put("outDate", DateUtil.getTime());
				// 第一次要查询月统计是否有，其它次数不用查询月统计，只查询单个药的天统计
				PageData monData = drugOutRecordService.isHaveByCId(outAllPd);
				if (IsNull.paramsIsNull(monData)) {
					outAllPd.put("drugOutRecordId", this.get32UUID());
					outAllPd.put("outWeek", "0");
					outAllPd.put("kind", "1");
					outAllPd.put("number", 1);
					outAllPd.put("units", "种");
					outAllPd.put("clinicdrugId", clinicdrugId);
					outAllPd.put("drugName", data.get("sysdrugName"));
					outAllPd.put("manufacturer", data.get("manufacturer"));
					outAllPd.put("drugName", data.get("sysdrugName"));
					outAllPd.put("outDay", "32");
					drugOutRecordService.save(outAllPd);
				} else {// 如果不为空的话，查一下是否这个月已经有了这种药了
					outAllPd.put("clinicdrugId", clinicdrugId);
					outAllPd.put("day", DateUtil.getDay());
					PageData drData = drugOutRecordService.isHaveDrugM(outAllPd);
					if (IsNull.paramsIsNull(drData)) {
						monData.put("num", 1);
						drugOutRecordService.editById(monData);// 修改这个月的
					}
				}

				pd.put("outDate", DateUtil.getDay());
				PageData dateData = drugOutRecordService.isHaveDrug(pd);
				if (IsNull.paramsIsNull(dateData)) {
					// 当天有数据
					outAllPd.put("drugOutRecordId", this.get32UUID());
					outAllPd.put("outWeek", DateUtil.getWeek());
					outAllPd.put("kind", "0");
					outAllPd.put("outDay", new SimpleDateFormat("d").format(new Date()));
					outAllPd.put("number", reduceNumBig);
					outAllPd.put("units", data.get("units"));
					outAllPd.put("clinicdrugId", clinicdrugId);
					outAllPd.put("drugName", data.get("sysdrugName"));
					outAllPd.put("manufacturer", data.get("manufacturer"));
					outAllPd.put("drugName", data.get("sysdrugName"));
					drugOutRecordService.save(outAllPd);
				} else {
					outAllPd.put("drugOutRecordId", dateData.get("drugOutRecordId"));
					outAllPd.put("num", reduceNumBig);
					outAllPd.put("outWeek", DateUtil.getWeek());
					drugOutRecordService.editById(outAllPd);// 修改这种
				}
			}
		}
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 查询处方笺详情（收费端、拿药端）
	 * @date 2018年1月18日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/findCheckInInfo")
	@ResponseBody
	public Object findCheckInInfo() {
		logBefore(logger, " 查询处方笺详情");
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
				map.put("message", message);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				PageData checkData = clinic_checkinService.findByidxq(pd);
				if (checkData != null) {
					List<PageData> list = clinicCheckinDetailService.listByCheckIn(pd);
					if (list != null) {
						checkData.put("list", list);
						pd.put("conversationLastId", pd.get("checkinId"));
						PageData data = orderPatiService.findByCid(pd);
						if (data != null) {
							checkData.put("orderNumber", data.get("orderNumber"));
						} else {
							checkData.put("orderNumber", "");
						}
					} else {
						checkData.put("list", new PageData());
						checkData.put("orderNumber", "");
					}

					map.put("db", checkData);
					result = "0000";
					message = "成功";
				} else {
					map.put("db", new PageData());
					result = "1002";
					message = "暂无数据";
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
	 * @description 收银员完成收费
	 * @date 2018年1月20日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/completeC")
	@ResponseBody
	public Object completeC() {
		logBefore(logger, "收银员完成收费 ");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("moneyCh"))
					|| IsNull.paramsIsNull(pd.get("moneyWe")) || IsNull.paramsIsNull(pd.get("money"))
					|| IsNull.paramsIsNull(pd.get("checkinId")) || IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
				map.put("message", message);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				PageData checkData = clinic_checkinService.findByidxq(pd);// 查询就诊详情
				String medicamentId = completeCharge(pd, 0, checkData);
				map.put("medicamentId", medicamentId);
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
	 * @description 药剂完成拿药 medicamentId doctorId
	 * @date 2018年1月20日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/completeDrug")
	@ResponseBody
	public Object completeDrug() {
		logBefore(logger, "药剂完成拿药");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("checkinId")) || IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
				map.put("message", message);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				completeMedicine(pd, null);
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
	 * @description 拿药和收费合并接口
	 * @date 2018年1月20日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/chargeAndDrug")
	@ResponseBody
	public Object chargeAndDrug() {
		logBefore(logger, "拿药和收费合并接口");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("moneyCh"))
					|| IsNull.paramsIsNull(pd.get("moneyWe")) || IsNull.paramsIsNull(pd.get("money"))
					|| IsNull.paramsIsNull(pd.get("checkinId")) || IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
				map.put("message", message);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				PageData checkData = clinic_checkinService.findByidxq(pd);// 查询就诊详情
				String medicamentId = completeCharge(pd, 0, checkData);// 完成收费
				pd.put("medicamentId", medicamentId);
				completeMedicine(pd, checkData);// 完成拿药

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
	 * @description 查询历史记录-接诊
	 * @date 2018年1月20日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/listHistory")
	@ResponseBody
	public Object listHistory() {
		logBefore(logger, "查询历史记录-接诊");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("pageSize")) || IsNull.paramsIsNull(pd.get("pageIndex"))
					|| IsNull.paramsIsNull(pd.get("searState"))) {
				result = "9993";
				message = "参数异常";
				map.put("message", message);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				pd = PagingUtil.addPaging(pd);
				pd.put("hisState", 1);
				// 查询接诊列表
				List<PageData> histList = clinic_checkinService.listFromAll(pd);
				if (histList != null && histList.size() > 0) {
					for (int i = 0; i < histList.size(); i++) {
						PageData data = histList.get(i);
						// 查询就诊药品详情
						List<PageData> drugList = clinicCheckinDetailService.listByCheckIn(data);
						if (drugList != null) {
							data.put("drugList", com.alibaba.fastjson.JSON.toJSON(drugList));
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
	 * 9 描述：诊所端 收银员 取消收费 修改状态 新版的重新规划，旧版的逻辑加进去
	 * 
	 * @author 董雪蕊
	 * @date 2018年1月22日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/qxSFedit", method = RequestMethod.POST)
	@ResponseBody
	public Object qxSFedit() {
		logBefore(logger, "收银员-取消收费");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 该接口 修改付费状态
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("checkinId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData doctorInfo = doctorService.findDoctorInfo(pd);
				pd.put("chargeDoctorId", pd.get("doctorId"));
				pd.put("chargeDoctorName", doctorInfo.get("trueName"));
				pd.put("chargeTime", DateUtil.getTime());
				clinic_checkinService.cancleCharge(pd);
				// *******************以下为旧逻辑
				pd.put("chargeTime", DateUtil.getTime());
				pd.put("charDoctorId", doctorInfo.get("trueName"));
				pd.put("charDoctorName", doctorInfo.get("doctorId"));
				chargeService.updateCharge(pd);
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
	 * @description 根据药名和处方名称查询历史记录（三角色）
	 * @date 2018年1月20日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/listHistBySear")
	@ResponseBody
	public Object listHistBySear() {
		logBefore(logger, "根据药名和处方名称查询历史记录（三角色）");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("pageSize")) || IsNull.paramsIsNull(pd.get("pageIndex"))
					|| IsNull.paramsIsNull(pd.get("searState")) || IsNull.paramsIsNull(pd.get("inputContent"))) {
				result = "9993";
				message = "参数异常";
				map.put("message", message);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				pd = PagingUtil.addPaging(pd);
				List<PageData> histList = clinic_checkinService.listFromAllSear(pd);
				if (histList != null && histList.size() > 0) {
					for (int i = 0; i < histList.size(); i++) {
						PageData data = histList.get(i);
						List<PageData> drugList = clinicCheckinDetailService.listByCheckIn(data);
						if (drugList != null) {
							data.put("drugList", com.alibaba.fastjson.JSON.toJSON(drugList));
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
}
