package com.fh.controller.app.dingTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.fh.config.TencentCloudConfig;
import com.fh.controller.app.tencentCloud.AppTencentCloudController;
import com.fh.service.app.ClinicService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.CloudSignService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.NewPushService;
import com.fh.service.app.NewsService;
import com.fh.service.app.PatientService;
import com.fh.service.app.WxTokenService;
import com.fh.service.statistics.ActiveService;
import com.fh.service.statistics.CumulAtiveService;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.jPush.examples.jPush;
import com.fh.util.weixin.openId;

@Component
public class stateMentTask {
	@Resource
	private NewsService newsService;
	@Resource
	private MonthCountService monthCountService;
	@Resource
	private DoctorService doctorService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private PatientService patientService;
	@Resource
	private NewPushService newPushService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private WxTokenService wxTokenService;
	@Resource
	private CloudSignService cloudSignService;
	@Resource
	private CumulAtiveService cumulAtiveService;
	@Resource
	private ActiveService activeService;

	/**
	 * 1. 描述：统计累计用户处理
	 * 
	 * @auther 霍学杰
	 * @date 2018年03月19日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@Scheduled(cron = "0 0 1 * * *")
	public void sumUser() throws Exception {
		PageData pd = new PageData();
		System.out.println("统计累计用户处理===20180501");
		pd.put("createTime", DateUtil.getPastDate2(1, DateUtil.getTime()) + " 23:59:59");
		List<PageData> findUserSum = activeService.findUserSum(pd);
		pd.put("dayDate", DateUtil.getPastDate2(1, DateUtil.getTime()));
		if (findUserSum != null && findUserSum.size() > 0) {
			for (int i = 0; i < findUserSum.size(); i++) {
				PageData dd = new PageData();
				dd = findUserSum.get(i);
				dd.put("createTime", pd.get("createTime"));
				dd.put("dayDate", pd.get("dayDate"));
				cumulAtiveService.save(dd);
			}
			Thread.sleep(500);
			System.gc();
		}
	}

	// 2.判断 现在时间到了 月报每月1号9点推送上一个月数据
	// 月工作报表：给出管理员 [4]月报表：上月总接诊XXX人，初诊患者xxx人，复诊患者xxx人，收益xxx元。
	@Scheduled(cron = "0 0 9 1 * *")
	public void monthStateMent() throws Exception {
		PageData monthPd = new PageData();
		monthPd.put("year", DateUtil.getYear());
		monthPd.put("month", DateUtil.getMonth());
		List<PageData> dataListM = monthCountService.monthNeedPush(monthPd);
		if (dataListM != null) {
			for (int i = 0; i < dataListM.size(); i++) {
				PageData data = dataListM.get(i);
				int sumMonthCount = 0;// 总接诊量
				int chuMonthCount = 0;// 初诊量
				int fuMonthCount = 0;// 复诊量
				String inMoney = "0";
				if (IsNull.paramsIsNull(data) == false) {
					if (IsNull.paramsIsNull(data.get("sumMonthCount")) == false) {
						sumMonthCount = Integer.parseInt(data.get("sumMonthCount").toString());
					}
					if (IsNull.paramsIsNull(data.get("chuMonthCount")) == false) {
						chuMonthCount = Integer.parseInt(data.get("chuMonthCount").toString());
					}
					if (IsNull.paramsIsNull(data.get("fuMonthCount")) == false) {
						fuMonthCount = Integer.parseInt(data.get("fuMonthCount").toString());
					}
					if (IsNull.paramsIsNull(data.get("inMoney")) == false) {
						inMoney = data.get("inMoney").toString();
					}
				}
				PageData userData = doctorService.findGuan(data);
				if (IsNull.paramsIsNull(userData) == false) {
					// 数据
					String content = "月报表：上月总接诊" + sumMonthCount + "人，初诊患者" + chuMonthCount + "人，复诊患者" + fuMonthCount
							+ "人，收益" + inMoney + "元。";
					String title = "工作报表";
					String type = "4";// 月工作报表
					int flag = 1;
					String fromUserId = "";
					int fromRoleFlag = 0;
					int toRoleFlag = 1;
					String toUserId = userData.get("doctorId").toString();
					String creatDate = DateUtil.getDay();
					String creatTime = DateUtil.getTime();
					// 推送
					Collection<String> alias = new ArrayList<>();
					Collection<String> registrationId = new ArrayList<>();
					alias.add(userData.get("doctorId").toString());
					registrationId.add(userData.get("phoneSole").toString());

					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					phoneSole = userData.getString("phoneSole");
					huaWeiToken = userData.getString("huaWeiToken");
					miRegId = userData.getString("miRegId");
					mzPushId = userData.getString("mzPushId");

					jPush.sendAll(alias, registrationId, title, type, content, "", "0", toUserId, "1",
							DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId,
							phoneSole);
					// 添加消息
					PageData newsPd = new PageData();
					newsPd.put("sysdrugName", "");
					newsPd.put("title", title);
					newsPd.put("type", type);
					newsPd.put("flag", flag);
					newsPd.put("messageContent", content);
					newsPd.put("fromUserId", fromUserId);
					newsPd.put("fromRoleFlag", fromRoleFlag);
					newsPd.put("toUserId", userData.get("doctorId").toString());
					newsPd.put("toRoleFlag", toRoleFlag);
					newsPd.put("creatDate", creatDate);
					newsPd.put("creatTime", creatTime);
					newsService.save(newsPd);
				}
				Thread.sleep(101);
			}
		}
	}

	// 3.判断 现在时间到了 年报阳历1-12月为一年，元旦9点推送年报
	// 年工作报表：给出管理员 [5]年报表：去年总接诊XXX人，初诊患者xxx人，复诊患者xxx人，收益xxx元。
	@Scheduled(cron = "0 0 9 1 1 *")
	public void yearStateMent() throws Exception {
		PageData monthPd = new PageData();
		monthPd.put("year", DateUtil.getYear());
		List<PageData> dataListY = monthCountService.yearNeedPush(monthPd);
		if (dataListY != null) {
			for (int i = 0; i < dataListY.size(); i++) {
				PageData data = dataListY.get(i);
				int sumYearCount = 0;// 总接诊量
				int chuYearCount = 0;// 初诊量
				int fuYearCount = 0;// 复诊量
				String inMoneyYear = "0";
				if (IsNull.paramsIsNull(data) == false) {
					if (IsNull.paramsIsNull(data.get("sumYearCount")) == false) {
						sumYearCount = Integer.parseInt(data.get("sumYearCount").toString());
					}
					if (IsNull.paramsIsNull(data.get("chuYearCount")) == false) {
						chuYearCount = Integer.parseInt(data.get("chuYearCount").toString());
					}
					if (IsNull.paramsIsNull(data.get("fuYearCount")) == false) {
						fuYearCount = Integer.parseInt(data.get("fuYearCount").toString());
					}
					if (IsNull.paramsIsNull(data.get("inMoneyYear")) == false) {
						inMoneyYear = data.get("inMoneyYear").toString();
					}
				}
				PageData userData = doctorService.findGuan(data);
				if (IsNull.paramsIsNull(userData) == false) {
					// 数据
					String content = "年报表：去年总接诊" + sumYearCount + "人，初诊患者" + chuYearCount + "人，复诊患者" + fuYearCount
							+ "人，收益" + inMoneyYear + "元。";
					String title = "工作报表";
					String type = "5";// 年工作报表
					int flag = 1;
					String fromUserId = "";
					int fromRoleFlag = 0;
					int toRoleFlag = 1;
					String toUserId = userData.get("doctorId").toString();
					String creatDate = DateUtil.getDay();
					String creatTime = DateUtil.getTime();
					// 推送
					Collection<String> alias = new ArrayList<>();
					Collection<String> registrationId = new ArrayList<>();
					alias.add(userData.get("doctorId").toString());
					registrationId.add(userData.get("phoneSole").toString());

					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					phoneSole = data.getString("phoneSole");
					huaWeiToken = data.getString("huaWeiToken");
					miRegId = data.getString("miRegId");
					mzPushId = data.getString("mzPushId");

					jPush.sendAll(alias, registrationId, title, type, content, "", "0", toUserId, "1",
							DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId,
							phoneSole);
					// 添加消息
					PageData newsPd = new PageData();
					newsPd.put("sysdrugName", "");
					newsPd.put("title", title);
					newsPd.put("type", type);
					newsPd.put("flag", flag);
					newsPd.put("messageContent", content);
					newsPd.put("fromUserId", fromUserId);
					newsPd.put("fromRoleFlag", fromRoleFlag);
					newsPd.put("toUserId", userData.get("doctorId").toString());
					newsPd.put("toRoleFlag", toRoleFlag);
					newsPd.put("creatDate", creatDate);
					newsPd.put("creatTime", creatTime);
					newsService.save(newsPd);
				}
			}
		}

	}

	// 1预约中 --> 5预约医生未处理，系统取消
	// ***************************↓↓↓为每天晚上23:59取消挂号和预约挂号推送↓↓↓********************************************8
	@Scheduled(cron = "0 50 23 * * *")
	public void cancelCheckIn() throws Exception {

		List<PageData> checkInList = clinic_checkinService.checkInList(null);
		if (checkInList != null) {
			for (int i = 0; i < checkInList.size(); i++) {
				PageData data = checkInList.get(i);
				if (data.get("isYu").toString().equals("true")) {// 是预约的
					PageData pd = new PageData();
					pd.put("yuState", 6);
					pd.put("state", 3);
					pd.put("checkinId", data.get("checkinId"));
					clinic_checkinService.cancelChecIn(pd);// 取消了今天没接诊的
					// ============推送给患者
					PageData patiData = patientService.findById(data);
					// 添加消息
					PageData newsPd = new PageData();
					newsPd.put("sysdrugName", "");
					newsPd.put("title", "预约挂号失败通知");
					newsPd.put("type", 37);
					newsPd.put("flag", 1);
					newsPd.put("messageContent", "您预约今天" + data.get("checkinTime").toString().substring(11, 16) + "去"
							+ data.getString("clinicName") + "就诊，系统超时自动取消。");
					newsPd.put("fromUserId", "");
					newsPd.put("fromRoleFlag", 0);
					newsPd.put("toUserId", data.get("patientId"));
					newsPd.put("toRoleFlag", 2);
					newsPd.put("creatDate", DateUtil.getDay());
					newsPd.put("creatTime", DateUtil.getTime());
					newsService.save(newsPd);
					// 推送
					if (IsNull.paramsIsNull(patiData) == false) {
						if (!"0".equals(patiData.get("phoneSole").toString())
								|| !"0".equals(patiData.get("huaWeiToken").toString())
								|| !"0".equals(patiData.get("miRegId").toString())
								|| !"0".equals(patiData.get("mzPushId").toString())) {
							String phoneSole = "";
							String huaWeiToken = "";
							String miRegId = "";
							String mzPushId = "";
							phoneSole = patiData.getString("phoneSole");
							huaWeiToken = patiData.getString("huaWeiToken");
							miRegId = patiData.getString("miRegId");
							mzPushId = patiData.getString("mzPushId");

							Collection<String> alias = new ArrayList<>();
							Collection<String> registrationId = new ArrayList<>();
							alias.add(patiData.get("patientId").toString());
							registrationId.add(patiData.get("phoneSole").toString());
							jPush.sendAllPat(alias, registrationId, "预约挂号失败通知", "37",
									newsPd.getString("messageContent"), "", "0", data.get("patientId").toString(), "2",
									DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
									mzPushId, phoneSole);
						}
					}
					// 发模板消息--失败--
					if (IsNull.paramsIsNull(patiData.get("wxopenId")) == false) {
						data.put("refuseReason", newsPd.getString("messageContent"));
						openId.sendPatientMu(patiData.getString("patientId"), data.getString("clinicId"),
								data.getString("doctorId"), 1, "", patientService, newPushService, wxTokenService,
								clinicService, doctorService, "", "", data);
					}
					// ============推送给医生
					PageData docData = doctorService.findById(data);
					// 添加消息
					PageData newsPd2 = new PageData();
					newsPd2.put("sysdrugName", "");
					newsPd2.put("title", "预约挂号失败通知");
					newsPd2.put("type", 38);
					newsPd2.put("flag", 1);
					newsPd2.put("messageContent", data.get("patientName").toString() + "预约今天"
							+ data.get("checkinTime").toString().substring(11, 16) + "前来就诊，系统超时自动取消。");
					newsPd2.put("fromUserId", "");
					newsPd2.put("fromRoleFlag", 0);
					newsPd2.put("toUserId", data.get("doctorId").toString());
					newsPd2.put("toRoleFlag", 1);
					newsPd2.put("creatDate", DateUtil.getDay());
					newsPd2.put("creatTime", DateUtil.getTime());
					newsService.save(newsPd2);
					// 推送
					Collection<String> alias = new ArrayList<>();
					Collection<String> registrationId = new ArrayList<>();
					alias.add(docData.get("doctorId").toString());
					registrationId.add(docData.get("phoneSole").toString());

					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					phoneSole = data.getString("phoneSole");
					huaWeiToken = data.getString("huaWeiToken");
					miRegId = data.getString("miRegId");
					mzPushId = data.getString("mzPushId");

					jPush.sendAll(alias, registrationId, "预约挂号失败通知", "38", newsPd2.getString("messageContent"), "", "0",
							docData.get("doctorId").toString(), "1", DateUtil.getDay(), DateUtil.getTime(), "", "", "",
							"", huaWeiToken, miRegId, mzPushId, phoneSole);

				} else {
					// 不是预约的，不推送
					PageData pd = new PageData();
					pd.put("yuState", 0);
					pd.put("state", 3);
					pd.put("checkinId", data.get("checkinId"));
					clinic_checkinService.cancelChecIn(pd);// 取消了今天没接诊的
				}

			}
		}

	}

	// ***************************↓↓↓为每天01:59更新腾讯云管理员的sign↓↓↓********************************************8
	@Scheduled(cron = "0 59 01 * * *")
	public void editCloudSign() throws Exception {
		String sign = AppTencentCloudController.getUserSign(TencentCloudConfig.ADMIN_NAME);
		PageData pd = new PageData();
		pd.put("CloudSign", sign);
		cloudSignService.edit(pd);
	}

}
