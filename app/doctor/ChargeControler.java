package com.fh.controller.app.doctor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.config.CommonConfig;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ChargeService;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.NewPushService;
import com.fh.service.app.NewsService;
import com.fh.service.app.PatientService;
import com.fh.service.app.WxTokenService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.DoubleUtil;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PhoneCheck;
import com.fh.util.tools.jPush.examples.jPush;
import com.fh.util.weixin.openId;

/**
 * 描述：收费员和药剂
 * 
 * @author 霍学杰
 * @date 2017.10.25 版本：1.2
 */

@Controller
@RequestMapping(value = "/api/charMed")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ChargeControler extends BaseController {

	@Resource
	private DoctorService doctorService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private PatientService patientService;
	@Resource
	private ChargeService chargeService;
	@Resource
	private ClinicUserService clinicUserService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private NewsService newsService;
	@Resource
	private NewPushService newPushService;
	@Resource
	WxTokenService wxTokenService;

	@Resource
	private DayCountService dayCountService;
	@Resource
	private MonthCountService monthCountService;

	/**
	 * 4 描述：诊所端 医生管理员退费
	 * 
	 * @author 霍学杰
	 * @date 2017年10月28日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/returnChar", method = RequestMethod.POST)
	@ResponseBody
	public Object returnChar() {
		logBefore(logger, "诊所端 医生管理员退费");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 该接口 修改付费状态
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("checkinId"))
					|| IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData doctorInfo = doctorService.findById(pd);
				if (doctorInfo == null) {
					result = "1010";
					message = "医生信息为空";
					map.put("result", result);
					map.put("message", message);
					logAfter(logger);
					return AppUtil.returnObject(new PageData(), map);
				}

				// 查询收费记录
				PageData findByID = chargeService.findByID(pd);
				if (findByID != null) {
					// 添加红票记录
					findByID.put("isRed", "-1");
					findByID.put("retChargeId", findByID.get("chargeId"));
					findByID.put("chargeId", this.get32UUID());
					findByID.put("retDoctorId", pd.get("doctorId"));
					findByID.put("retTime", DateUtil.getTime());
					findByID.put("createTime", DateUtil.getTime());
					findByID.put("retDoctorName", doctorInfo.get("trueName"));
					chargeService.save(findByID);
					moneyOut(pd.get("clinicId").toString(), Double.parseDouble(findByID.get("moneyCh").toString()),
							Double.parseDouble(findByID.get("moneyWe").toString()),
							Double.parseDouble(findByID.get("money").toString()));

					// 修改接诊记录状态
					PageData ss = new PageData();
					ss.put("checkinId", findByID.get("checkinId"));
					ss.put("state", "5");
					clinic_checkinService.updatestate(ss);
					result = "0000";
					message = "成功";
					// 推送 退费
					String sex = "";
					if ("0".equals(findByID.get("pSex").toString())) {
						sex = "女";
					} else {
						sex = "男";
					}
					String cfz = "";
					if ("0".equals(findByID.get("patientVisit").toString())) {
						cfz = "初诊";
					} else {
						cfz = "复诊";
					}
					String messageConet = "患者：" + findByID.getString("patientName") + " " + sex + " "
							+ findByID.get("pAge") + " " + cfz + " 申请退费,已由" + doctorInfo.getString("trueName")
							+ "医生完成该患者退费流程";
					List<PageData> findcylist = clinicUserService.findcylist(pd);
					PageData dd = new PageData();
					if (findcylist != null && findcylist.size() > 0) {
						for (int j = 0; j < findcylist.size(); j++) {
							dd = findcylist.get(j);//
							Collection<String> alias = new ArrayList<>();
							Collection<String> registrationId = new ArrayList<>();
							registrationId.add(dd.get("phoneSole").toString());
							String toUserId = dd.get("doctorId").toString();
							alias.add(toUserId);
							String title = "退费提醒";

							String phoneSole = "";
							String huaWeiToken = "";
							String miRegId = "";
							String mzPushId = "";
							phoneSole = dd.getString("phoneSole");
							huaWeiToken = dd.getString("huaWeiToken");
							miRegId = dd.getString("miRegId");
							mzPushId = dd.getString("mzPushId");

							jPush.sendAll(alias, registrationId, title, "17", messageConet, "", "0", toUserId, "1",
									DateUtil.getDay(), DateUtil.getTime(), "", findByID.get("checkinId").toString(), "",
									"", huaWeiToken, miRegId, mzPushId, phoneSole);

							PageData pp = new PageData();
							pp.put("title", title);
							pp.put("type", "17");//
							pp.put("headUrlNew", dd.get("headUrl"));
							pp.put("flag", 1);
							pp.put("messageContent", messageConet);
							pp.put("fromRoleFlag", 0);
							pp.put("skitId", findByID.get("checkinId"));
							pp.put("toUserId", toUserId);
							pp.put("toRoleFlag", 1);
							pp.put("creatDate", DateUtil.getDay());
							pp.put("creatTime", DateUtil.getTime());

							newsService.save(pp);

						}
					}
					// 给患者添加消息
					// patientId
					PageData news = new PageData();
					news.put("title", "退费提醒");
					news.put("type", 18);
					news.put("flag", 1);
					news.put("messageContent", "您(" + DateUtil.getZYear() + ")于" + findByID.getString("clinicName")
							+ "诊所申请退费，接诊医生" + findByID.getString("resultdoctor") + "已审核通过。");
					news.put("fromUserId", pd.get("doctorId"));
					news.put("fromRoleFlag", 1);
					news.put("toRoleFlag", 2);
					news.put("creatDate", DateUtil.getDay());
					news.put("creatTime", DateUtil.getTime());
					news.put("sysdrugName", "");
					news.put("skitId", pd.get("chargeId"));
					news.put("headUrlNew", "");
					news.put("huoDongUrl", "");
					news.put("toUserId", findByID.getString("patientId"));
					newsService.save(news);
				} else {
					result = "1002";
					message = "信息不存在";
				}

			}
		} catch (Exception e) {
			result = "9999";
			message = "服务器异常";
			e.printStackTrace();
		}
		map.put("result", result);
		map.put("message", message);
		logAfter(logger);
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 统计资金收益--退费
	 * 
	 * @throws Exception
	 */
	public void moneyOut(String clinicId, double inMoneyCh, double inMoneyWe, double inMoney) throws Exception {
		// 加数据
		PageData pd = new PageData();
		pd.put("clinicId", clinicId);
		String day = DateUtil.getDay();// 2017-01-01
		pd.put("year", day.substring(0, 4));
		pd.put("month", day.substring(5, 7));
		pd.put("inMoneyCh", inMoneyCh);// 中药费用
		pd.put("inMoneyWe", inMoneyWe);// 西药费用
		pd.put("inMoney", inMoney);// 总费用
		pd.put("inMoneyYear", inMoney);
		pd.put("inMoneyChYear", inMoneyCh);
		pd.put("inMoneyWeYear", inMoneyWe);
		pd.put("day", day.substring(5));
		// 处理逻辑
		PageData dayData = dayCountService.findDaysj2(pd);
		// 天数据
		if (IsNull.paramsIsNull(dayData) == false) {
			pd.put("cdayId", dayData.get("cdayId"));
			pd.put("inMoneyCh", DoubleUtil.sub(Double.parseDouble(dayData.get("inMoneyCh").toString()), inMoneyCh));
			pd.put("inMoneyWe", DoubleUtil.sub(Double.parseDouble(dayData.get("inMoneyWe").toString()), inMoneyWe));
			pd.put("inMoney", DoubleUtil.sub(Double.parseDouble(dayData.get("inMoney").toString()), inMoney));
			dayCountService.updateCount2(pd);
		}
		// 月数据
		PageData monData = monthCountService.findClinicsy2(pd);
		if (IsNull.paramsIsNull(monData) == false) {// 必不为空
			pd.put("cmonthId", monData.get("cmonthId"));
			pd.put("inMoneyCh", DoubleUtil.sub(Double.parseDouble(monData.get("inMoneyCh").toString()), inMoneyCh).toString());
			pd.put("inMoneyWe", DoubleUtil.sub(Double.parseDouble(monData.get("inMoneyWe").toString()), inMoneyWe).toString());
			pd.put("inMoney", DoubleUtil.sub(Double.parseDouble(monData.get("inMoney").toString()), inMoney).toString());
			pd.put("inMoneyYear", DoubleUtil.sub(Double.parseDouble(monData.get("inMoneyYear").toString()), inMoney).toString());
			pd.put("inMoneyChYear",
					DoubleUtil.sub(Double.parseDouble(monData.get("inMoneyChYear").toString()), inMoneyCh).toString());
			pd.put("inMoneyWeYear",
					DoubleUtil.sub(Double.parseDouble(monData.get("inMoneyWeYear").toString()), inMoneyWe).toString());
			monthCountService.updateCount2(pd);
		}

	}

	/**
	 * 27.描述：医生端 保存诊断结果 （新增患者-2018.10.7）
	 * 
	 * @auther 霍学杰
	 * @date 2017年11月7日*
	 * @version 1.3
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/saveZDresult", method = RequestMethod.POST)
	@ResponseBody
	public Object saveZDresult() {
		logBefore(logger, "医生端 保存诊断结果");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常，请查看您的网络";
		try {
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("doctorId"))
					|| IsNull.paramsIsNull(pd.get("doctorResult")) || IsNull.paramsIsNull(pd.get("saveType"))) {
				result = "9993";
				message = "还有项没有填写";
			} else {
				if (IsNull.paramsIsNull(pd.get("checkinId")) == false && "1".equals(pd.get("saveType").toString())) {
					// 新增患者
					clinic_checkinService.saveYZ(pd);
					map.put("checkinId", pd.get("checkinId"));
					result = "0000";
					message = "成功";
				} else {
					if (IsNull.paramsIsNull(pd.get("clinicName")) || IsNull.paramsIsNull(pd.get("patientName"))
							|| IsNull.paramsIsNull(pd.get("phone")) || IsNull.paramsIsNull(pd.get("patientSex"))
							|| IsNull.paramsIsNull(pd.get("patientAge")) || IsNull.paramsIsNull(pd.get("patientVisit"))
							|| IsNull.paramsIsNull(pd.get("patientMedicalHistory"))
							|| IsNull.paramsIsNull(pd.get("patientCondition"))
							|| IsNull.paramsIsNull(pd.get("doctorResult"))) {
						result = "9993";
						message = "还有项没有填写";
					} else if (!PhoneCheck.isMobile(pd.get("phone").toString())) {
						result = "9993";
						message = "请填写正确手机号";
					} else {

						if (IsNull.paramsIsNull(pd.get("patientId")) == false) {
							PageData findById = patientService.findById(pd);
							if (findById != null) {
								pd.put("headUrl", findById.get("headUrl"));
							}
						}

						if (IsNull.paramsIsNull(pd.get("checkinId")) == false) {// 是有这条数据的，修改
							pd.put("state", 1);
							clinic_checkinService.saveYZ(pd);
						} else {
							pd.put("checkinId", this.get32UUID());
							pd.put("state", 1);
							pd.put("checkinTime", DateUtil.getTime());
							pd.put("checktime", DateUtil.getTime1());
							pd.put("time", DateUtil.getDay() + " 01:00:00");
							PageData num = clinic_checkinService.findGHnum(pd);
							int number = 0;
							if (num != null) {
								number = Integer.parseInt(num.get("number").toString());
							}
							pd.put("number", number + 1);// 当前号码加1
							clinic_checkinService.savepat(pd);
						}
						map.put("checkinId", pd.get("checkinId"));
						result = "0000";
						message = "成功";
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
	 * 
	 * @author 董雪蕊
	 * @description 医生同意预约
	 * @date 2017年12月19日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/agreeYu")
	@ResponseBody
	public Object agreeYu() {
		logBefore(logger, "医生同意预约接诊");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("checkinId")) || IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				retMessage = "参数异常";
			} else {
				pd.remove("state");
				// 1.更新接诊表
				PageData docData = doctorService.findById(pd);
				if (IsNull.paramsIsNull(docData) == false) {
					pd.put("doctorName", docData.get("trueName"));
					pd.put("yuState", 4);
					pd.put("weight", "1");
					clinic_checkinService.updatestate(pd);
				}
				// 2.推送给患者预约成功了
				PageData cliInData = clinic_checkinService.findByidxq(pd);
				PageData patientData = patientService.findById(cliInData);
				if (IsNull.paramsIsNull(cliInData) == false) {
					// 小程序
					// 发模板消息--成功--挂号成功通知--模板2
					String yuTime = cliInData.get("yuTime").toString();// 挂号时间
					String checkinTime = cliInData.get("checkinTime").toString();// 就诊时间
					PageData data = new PageData();
					data.put("yuTime", yuTime);
					data.put("checkinTime", checkinTime);
					openId.sendPatientMu(cliInData.getString("patientId"), cliInData.getString("clinicId"),
							pd.get("doctorId").toString(), 2, "", patientService, newPushService, wxTokenService,
							clinicService, doctorService, "", "", data);
					// app
					String title = "预约挂号成功通知";
					String type = "27";
					String messageContent = "您已成功预约" + checkinTime.substring(0, 16) + "于"
							+ cliInData.getString("clinicName") + "诊所就诊。";
					String fromUserId = "";
					String fromRoleFlag = "0";
					String toUserId = cliInData.getString("patientId");
					String toRoleFlag = "2";
					String creatDate = DateUtil.getDay();
					String creatTime = DateUtil.getTime();
					String sysdrugName = "";
					String skitId = pd.get("checkinId").toString();
					String headUrlNew = "";
					String huoDongUrl = "";
					PageData news = new PageData();
					news.put("title", title);
					news.put("type", type);
					news.put("flag", 1);
					news.put("messageContent", messageContent);
					news.put("fromUserId", fromUserId);
					news.put("fromRoleFlag", fromRoleFlag);
					news.put("toUserId", toUserId);
					news.put("toRoleFlag", toRoleFlag);
					news.put("creatDate", creatDate);
					news.put("creatTime", creatTime);
					news.put("sysdrugName", sysdrugName);
					news.put("skitId", skitId);
					news.put("headUrlNew", headUrlNew);
					news.put("huoDongUrl", huoDongUrl);
					newsService.save(news);

					Collection<String> alias = new ArrayList();
					Collection<String> registrationId = new ArrayList();

					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					phoneSole = patientData.getString("phoneSole");
					huaWeiToken = patientData.getString("huaWeiToken");
					miRegId = patientData.getString("miRegId");
					mzPushId = patientData.getString("mzPushId");
					alias.add(toUserId);
					registrationId.add(patientData.getString("phoneSole"));
					jPush.sendAllPat(alias, registrationId, title, type, messageContent, fromUserId, fromRoleFlag,
							toUserId, toRoleFlag, creatDate, creatTime, sysdrugName, skitId, headUrlNew, huoDongUrl,
							huaWeiToken, miRegId, mzPushId, phoneSole);
					// 3.将20分钟的提醒停了
					pd.put("skitId", pd.get("checkinId"));
					newsService.delNoPush2(pd);
					// 4.插入提醒医生的消息 10分钟一个 30分钟一个
					PageData news2 = new PageData();
					int fen = CommonConfig.TIME30;
					String time = DateUtil.getPastTime(fen, checkinTime);// 得到的格式：2017-12-17 00:01
					String pushTime = time + ":00";// 插入推送时间 ：2017-12-17 00:01：00
					String paName = "有患者";
					if (IsNull.paramsIsNull(patientData.get("trueName")) == false) {
						paName = patientData.get("trueName").toString();
					}
					news2.put("title", "预约就诊通知");
					news2.put("type", 28);
					news2.put("flag", 0);
					news2.put("messageContent",
							paName + "预约今天" + checkinTime.substring(11, 16) + "就诊，还有" + fen + "分钟到达预约时间，您可提前做好准备。");
					news2.put("fromUserId", "");
					news2.put("fromRoleFlag", 0);
					news2.put("toUserId", pd.get("doctorId"));
					news2.put("toRoleFlag", 1);
					news2.put("creatDate", creatDate);
					news2.put("creatTime", creatTime);
					news2.put("sysdrugName", "");
					news2.put("skitId", skitId);
					news2.put("headUrlNew", headUrlNew);
					news2.put("huoDongUrl", huoDongUrl);
					news2.put("pushTime", pushTime);
					newsService.save(news2);
					// 10分钟的
					fen = CommonConfig.TIME10;
					time = DateUtil.getPastTime(fen, checkinTime);// 得到的格式：2017-12-17 00:01
					pushTime = time + ":00";// 插入推送时间 ：2017-12-17 00:01：00
					news2.put("messageContent",
							paName + "预约今天" + checkinTime.substring(11, 16) + "就诊，还有" + fen + "分钟到达预约时间，您可提前做好准备。");
					news2.put("pushTime", pushTime);
					news2.put("type", 29);
					newsService.save(news2);
					// 插入提醒结束消息
					news2.put("messageContent",
							paName + "预约今天" + checkinTime.substring(11, 16) + "就诊，现已到预约时间，请确认患者是否到诊。");
					news2.put("pushTime", checkinTime);
					news2.put("type", 35);
					newsService.save(news2);
					// 5.插入提醒患者的消息
					PageData news3 = new PageData();
					news3.put("title", "日程提醒");
					news3.put("type", 30);
					news3.put("flag", 0);
					news3.put("messageContent", "您预约今天" + checkinTime.substring(11, 16) + "就诊，还有30分钟到达预约时间，您可提前做好准备。");
					news3.put("fromUserId", "");
					news3.put("fromRoleFlag", 0);
					news3.put("toUserId", patientData.get("patientId"));
					news3.put("toRoleFlag", 2);
					news3.put("creatDate", creatDate);
					news3.put("creatTime", creatTime);
					news3.put("sysdrugName", sysdrugName);
					news3.put("skitId", skitId);
					news3.put("headUrlNew", headUrlNew);
					news3.put("huoDongUrl", huoDongUrl);
					news3.put("pushTime", DateUtil.getPastTime(CommonConfig.TIME30, checkinTime) + ":00");
					newsService.save(news3);
					// 插入提醒结束消息
					news3.put("messageContent", "您预约今天" + checkinTime.substring(11, 16) + "就诊，现已到预约时间，请您尽快前往就医。");
					news3.put("pushTime", checkinTime);
					news3.put("type", 34);
					newsService.save(news3);
					result = "0000";
					retMessage = "成功";
				}
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
	 * @description 医生取消预约接诊---拒绝
	 * @date 2017年12月20日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/cancelYu")
	@ResponseBody
	public Object cancelYu() {
		logBefore(logger, "医生取消预约接诊");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("checkinId")) || IsNull.paramsIsNull(pd.get("doctorId"))
					|| IsNull.paramsIsNull(pd.get("refuseReason"))) {
				result = "9993";
				retMessage = "参数异常";
			} else {
				// 1.更新接诊记录状态、拒绝原因
				pd.put("state", 3);
				pd.put("yuState", 3);
				pd.put("doctorId", pd.get("doctorId"));
				clinic_checkinService.updatestate(pd);
				// 2.给患者推送拒绝
				PageData cliInData = clinic_checkinService.findByidxq(pd);
				PageData patientData = patientService.findById(cliInData);
				if (IsNull.paramsIsNull(patientData) == false) {
					// 小程序
					// 发模板消息--失败--模板1
					String yuTime = cliInData.get("yuTime").toString();// 挂号时间
					String checkinTime = cliInData.get("checkinTime").toString();// 就诊时间
					PageData data = new PageData();
					data.put("yuTime", yuTime);
					data.put("checkinTime", checkinTime);
					data.put("refuseReason", pd.get("refuseReason").toString());
					openId.sendPatientMu(cliInData.getString("patientId"), cliInData.getString("clinicId"),
							pd.get("doctorId").toString(), 1, "", patientService, newPushService, wxTokenService,
							clinicService, doctorService, "", "", data);
					// app
					String title = "预约挂号失败通知";
					String type = "31";
					String messageContent = cliInData.getString("clinicName") + "诊所拒绝了您的本次预约挂号，原因："
							+ pd.get("refuseReason").toString();
					String fromUserId = "";
					String fromRoleFlag = "0";
					String toUserId = patientData.getString("patientId");
					String toRoleFlag = "2";
					String creatDate = DateUtil.getDay();
					String creatTime = DateUtil.getTime();
					String sysdrugName = "";
					String skitId = pd.get("checkinId").toString();
					String headUrlNew = "";
					String huoDongUrl = "";
					PageData news = new PageData();
					news.put("title", title);
					news.put("type", type);
					news.put("flag", 1);
					news.put("messageContent", messageContent);
					news.put("fromUserId", fromUserId);
					news.put("fromRoleFlag", fromRoleFlag);
					news.put("toUserId", toUserId);
					news.put("toRoleFlag", toRoleFlag);
					news.put("creatDate", creatDate);
					news.put("creatTime", creatTime);
					news.put("sysdrugName", sysdrugName);
					news.put("skitId", skitId);
					news.put("headUrlNew", headUrlNew);
					news.put("huoDongUrl", huoDongUrl);
					newsService.save(news);

					Collection<String> alias = new ArrayList();
					Collection<String> registrationId = new ArrayList();

					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					phoneSole = patientData.getString("phoneSole");
					huaWeiToken = patientData.getString("huaWeiToken");
					miRegId = patientData.getString("miRegId");
					mzPushId = patientData.getString("mzPushId");

					alias.add(toUserId);
					registrationId.add(patientData.getString("phoneSole"));
					jPush.sendAllPat(alias, registrationId, title, type, messageContent, fromUserId, fromRoleFlag,
							toUserId, toRoleFlag, creatDate, creatTime, sysdrugName, skitId, headUrlNew, huoDongUrl,
							huaWeiToken, miRegId, mzPushId, phoneSole);

				}
				// 3.删20分钟提醒
				pd.put("skitId", pd.get("checkinId"));
				newsService.delNoPush2(pd);
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
	 * 27.描述：医生端 保存患者的档案
	 * 
	 * @author 董雪蕊
	 * @date 2018年1月9日
	 * @version 1.3
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/savePatiFile", method = RequestMethod.POST)
	@ResponseBody
	public Object savePatiFile() {
		logBefore(logger, "医生端 保存患者的档案");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常，请查看您的网络";
		try {
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("doctorId"))
					|| IsNull.paramsIsNull(pd.get("doctorResult")) || IsNull.paramsIsNull(pd.get("clinicName"))
					|| IsNull.paramsIsNull(pd.get("patientName")) || IsNull.paramsIsNull(pd.get("phone"))
					|| IsNull.paramsIsNull(pd.get("patientSex")) || IsNull.paramsIsNull(pd.get("patientAge"))
					|| IsNull.paramsIsNull(pd.get("patientVisit"))
					|| IsNull.paramsIsNull(pd.get("patientMedicalHistory"))
					|| IsNull.paramsIsNull(pd.get("patientCondition")) || IsNull.paramsIsNull(pd.get("doctorResult"))) {
				result = "9993";
				message = "还有项没有填写";
			} else if (!PhoneCheck.isMobile(pd.get("phone").toString())) {
				result = "9993";
				message = "请填写正确手机号";
			} else {
				// 查询医生信息
				PageData docData = doctorService.findById(pd);
				if (IsNull.paramsIsNull(docData) == false
						&& IsNull.paramsIsNull(docData.getString("trueName")) == false) {
					pd.put("doctorName", docData.getString("doctorName"));
				} else {
					pd.put("doctorName", "");
				}
				pd.put("checkinTime", DateUtil.getTime());
				pd.put("state", 6);//保存患者档案
				if (IsNull.paramsIsNull(pd.get("checkinId")) == false) {
					// 医生添加医嘱
					clinic_checkinService.saveYZ(pd);
				} else {
					// 医生添加患者
					pd.put("checkinId", this.get32UUID());
					pd.put("checktime", DateUtil.getTime1());
					pd.put("time", DateUtil.getDay() + " 01:00:00");
					pd.put("createTime", DateUtil.getTime());
					pd.put("number", 0);
					pd.put("isFile", 1);
					clinic_checkinService.savepat(pd);
				}
				map.put("checkinId", pd.get("checkinId"));
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
	 * 20. 描述：根据 当前时间获取前几天时间
	 * 
	 * @auther 霍学杰
	 * @date 2017年11月11日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/getPastDate", method = RequestMethod.POST)
	@ResponseBody
	public Object getPastDate() {
		logBefore(logger, "根据 当前时间获取前几天时间");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接错误，请检查您的网络";
		try {
			if (IsNull.paramsIsNull(pd.get("pastName"))) {
				result = "9993";
				message = "您还有项没有完成";
				map.put("result", result);
				map.put("message", message);
				return AppUtil.returnObject(new PageData(), map);
			}
			int past = 0;
			if ("近七天".equals(pd.get("pastName").toString())) {
				past = 7;
			} else if ("近三十天".equals(pd.get("pastName").toString())) {
				past = 30;
			}

			ArrayList<String> pastDate = DateUtil.getPastDate(past, DateUtil.getDay());
			if (pastDate != null && pastDate.size() > 0) {
				map.put("starTime", pastDate.get(past - 2));
				map.put("endTime", DateUtil.getDay());
				result = "0000";
				message = "成功";
			} else {
				map.put("starTime", "");
				map.put("endTime", DateUtil.getDay());
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
	 * 21. 描述: 判断 营业时间 值班 接诊限制 是否开启
	 * 
	 * @auther 霍学杰
	 * @date 2017年11月13日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/getPDstate", method = RequestMethod.POST)
	@ResponseBody
	public Object getPDstate() {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接错误，请检查您的网络";
		try {
			if (IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "您还有项没有完成";
			} else {
				PageData findzsClinic = clinicService.findzsClinic(pd);
				int yyOpen = Integer.parseInt(findzsClinic.get("openState").toString());
				int jhyyOpen = Integer.parseInt(findzsClinic.get("jhOpenState").toString());
				if (yyOpen == 1) {
					map.put("yyOpen", yyOpen);
					map.put("message", "已经开启营业时间");
				} else {
					map.put("yyOpen", yyOpen);
					map.put("message", "未开启营业时间");
				}
				if (jhyyOpen == 1) {
					map.put("jhyyOpen", jhyyOpen);
					map.put("message", "已经开启节假日营业时间");
				} else {
					map.put("jhyyOpen", jhyyOpen);
					map.put("message", "未开启节假日营业时间");
				}
				map.put("openClinic", findzsClinic);
				result = "0000";
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
}
