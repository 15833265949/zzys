package com.fh.controller.app.clinicuser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fh.config.CommonMessage;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.CloudSignService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.NewsService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.jPush.examples.jPush;

/**
 * 作者：董江宁 类：诊所处理类
 */

@Controller
@RequestMapping(value = "/api/clinicuser")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AppClinicUserController extends BaseController {

	@Resource
	private ClinicService clinicService;
	@Resource
	private DoctorService doctorService;
	@Resource
	private ClinicUserService clinicUserService;
	@Resource
	private MonthCountService monthCountService;
	@Resource
	private DayCountService dayCountService;
	@Resource
	private NewsService newsService;
	@Resource
	private CloudSignService cloudSignService;

	/**
	 * 2.1描述：审核 成员 分配角色 v3
	 * 
	 * 留一个版本后删除 9.3
	 * 
	 * @auther 霍学杰
	 * @date 2017年11月13日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/editcyjs3", method = RequestMethod.POST)
	@ResponseBody
	public Object editcyjs3() {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("addId")) || IsNull.paramsIsNull(pd.get("doctorId"))
					|| IsNull.paramsIsNull(pd.get("state")) || IsNull.paramsIsNull(pd.get("addClinicFlag"))
					|| IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {

				int state = Integer.parseInt(pd.get("state").toString());
				PageData doctorInfo = doctorService.findDoctorInfo(pd);
				if (state == 1) {
					pd.put("roleFlag", 0);
					clinicUserService.editcliniccy(pd);// 通过
					// 添加成员 历史统计记录
					// 1月数据 2年数据
					// 添加月历史统计记录 诊所 v2
					PageData day1 = new PageData();
					day1.put("dayId", this.get32UUID());
					day1.put("month", DateUtil.getMonth());
					day1.put("day", DateUtil.getDay1());
					day1.put("clinicId", pd.get("clinicId"));

					// 添加个人月历史数据 v2
					day1.put("doctorId", pd.get("doctorId"));
					// 添加年历史统计记录 诊所 v2
					day1.put("year", DateUtil.getYear());

					// 1.查看有没有历史记录 有跳过 没有添加
					PageData findminthsj = monthCountService.findDoctorshujutj(day1);// 个人
					PageData findDaysj2 = dayCountService.findshujutj(day1);// 诊所天数据

					if (findminthsj == null) {
						day1.put("monthId", this.get32UUID());
						monthCountService.save1(day1);
					}
					if (findDaysj2 == null) {
						day1.put("dayId", this.get32UUID());
						dayCountService.save(day1);
					}

					result = "0000";
					message = "审核成员通过";
					// 推送审核通过消息
					String authPlat = "医生";
					if ("6".equals(pd.get("authPlat").toString())) {
						authPlat = "医生、收银员、药剂师";
					} else if ("5".equals(pd.get("authPlat").toString())) {
						authPlat = "药剂师";
					} else if ("4".equals(pd.get("authPlat").toString())) {
						authPlat = "收银员、药剂师";
					} else if ("3".equals(pd.get("authPlat").toString())) {
						authPlat = "收银员";
					} else if ("2".equals(pd.get("authPlat").toString())) {
						authPlat = "医生、收银员";
					}

					String messageConet = "您已成功加入" + doctorInfo.get("clinicName").toString() + "管理员分配身份为：" + authPlat;

					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					phoneSole = doctorInfo.getString("phoneSole");
					huaWeiToken = doctorInfo.getString("huaWeiToken");
					miRegId = doctorInfo.getString("miRegId");
					mzPushId = doctorInfo.getString("mzPushId");

					Collection<String> alias = new ArrayList<>();
					Collection<String> registrationId = new ArrayList<>();
					registrationId.add(doctorInfo.get("phoneSole").toString());
					String toUserId = doctorInfo.get("doctorId").toString();
					alias.add(toUserId);
					String title = "审核消息提醒";
					jPush.sendAll(alias, registrationId, title, "10", messageConet, "", "0", toUserId, "1",
							DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId,
							phoneSole);
					// 将成员信息改变
					doctorService.editDoctorClinicInfo(pd);
				} else {
					pd.put("doctorLIflag", 3);
					clinicUserService.editcliniccy(pd);
					// 不通过
					result = "0000";
					message = "审核成员不通过";
					// 推送 审核不通过消息
					String messageConet = doctorInfo.get("clinicName").toString() + "拒绝您的加入申请，请重新返回选择诊所";

					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					phoneSole = doctorInfo.getString("phoneSole");
					huaWeiToken = doctorInfo.getString("huaWeiToken");
					miRegId = doctorInfo.getString("miRegId");
					mzPushId = doctorInfo.getString("mzPushId");

					Collection<String> alias = new ArrayList<>();
					Collection<String> registrationId = new ArrayList<>();
					registrationId.add(doctorInfo.get("phoneSole").toString());
					String toUserId = doctorInfo.get("doctorId").toString();
					alias.add(toUserId);
					String title = "审核消息提醒";
					jPush.sendAll(alias, registrationId, title, "12", messageConet, "", "0", toUserId, "1",
							DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId,
							phoneSole);
					// 将成员信息改变
					pd.put("authPlat", 0);
					pd.put("encounters", '0');
					pd.put("clinicRzFlag", 0);
					pd.put("addClinicFlag", 0);
					doctorService.editAuthPlat(pd);
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
	 * @author 董雪蕊
	 * @description 根据加入的惟一标识移除诊所成员
	 * @date 2017.8.27
	 * @version 1.0
	 * @param pd exitState 0移除诊所 1 退出诊所
	 * @return result+retMessage
	 */
	@RequestMapping(value = "/deleteClinicuser", method = RequestMethod.POST)
	@ResponseBody
	@Transactional
	public Object deleteClinicuser() {
		// 返回Json
		logBefore(logger, "删除诊所成员");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "申请成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("addId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("exitState"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				PageData addData = clinicUserService.findById(pd);
				if (IsNull.paramsIsNull(addData) == false) {

					// 删除成员消息
					clinicUserService.deleteByAddId(addData);

					// 修改医生信息 标记状态 权限
					pd.put("addClinicFlag", 0);
					pd.put("roleFlag", 0);
					pd.put("authPlat", 0);
					pd.put("exit", "1");
					pd.put("clinicRzFlag", 0);
					pd.put("doctorId", addData.get("doctorId"));
					pd.put("encounters", '0');
					clinicUserService.editclicyjs(pd);// 修改个人平台信息

					// 推送和消息
					String title = "";
					String type = "";
					String messageContent = "";
					String fromUserId = "";
					String fromRoleFlag = "";
					String toUserId = "";
					String toRoleFlag = "";
					String creatDate = DateUtil.getDay();
					String creatTime = DateUtil.getTime();
					String sysdrugName = "";
					String skitId = "";
					String headUrlNew = "";
					String huoDongUrl = "";
					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					int exitState = Integer.parseInt(pd.get("exitState").toString());
					if (exitState == 0) {// 被移除诊所
						PageData myData = doctorService.findById(pd);
						if (IsNull.paramsIsNull(myData) == false) {
							title = "移除机构提醒";
							type = "23";
							messageContent = "管理员将您移除了机构，请重新选择机构加入。";
							fromUserId = "";
							fromRoleFlag = "0";
							toUserId = myData.getString("doctorId");
							toRoleFlag = "1";
							phoneSole = myData.getString("phoneSole");
							huaWeiToken = myData.getString("huaWeiToken");
							miRegId = myData.getString("miRegId");
							mzPushId = myData.getString("mzPushId");

							Collection<String> alias = new ArrayList();
							Collection<String> registrationId = new ArrayList();
							alias.add(toUserId);
							registrationId.add(phoneSole);
							PageData news = new PageData();
							news.put("title", title);
							news.put("type", type);
							news.put("flag", 1);
							news.put("messageContent", messageContent);
							news.put("fromUserId", fromUserId);
							news.put("fromRoleFlag", fromRoleFlag);
							news.put("toRoleFlag", toRoleFlag);
							news.put("creatDate", creatDate);
							news.put("creatTime", creatTime);
							news.put("sysdrugName", sysdrugName);
							news.put("skitId", skitId);
							news.put("headUrlNew", headUrlNew);
							news.put("huoDongUrl", huoDongUrl);
							news.put("toUserId", toUserId);
							newsService.save(news);
							jPush.sendAll(alias, registrationId, title, type, messageContent, fromUserId, fromRoleFlag,
									toUserId, toRoleFlag, creatDate, creatTime, sysdrugName, skitId, headUrlNew,
									huoDongUrl, huaWeiToken, miRegId, mzPushId, phoneSole);

						}
					} else {// 退出诊所
						PageData guanData = doctorService.findGuan(pd);
						title = "退出机构提醒";
						type = "24";
						messageContent = "医生退出了机构";
						fromUserId = "";
						fromRoleFlag = "0";
						toUserId = guanData.getString("doctorId");
						toRoleFlag = "1";
						phoneSole = guanData.getString("phoneSole");
						huaWeiToken = guanData.getString("huaWeiToken");
						miRegId = guanData.getString("miRegId");
						mzPushId = guanData.getString("mzPushId");

						Collection<String> alias = new ArrayList();
						Collection<String> registrationId = new ArrayList();
						alias.add(toUserId);
						registrationId.add(phoneSole);
						PageData news = new PageData();
						news.put("title", title);
						news.put("type", type);
						news.put("flag", 1);
						news.put("messageContent", messageContent);
						news.put("fromUserId", fromUserId);
						news.put("fromRoleFlag", fromRoleFlag);
						news.put("toRoleFlag", toRoleFlag);
						news.put("creatDate", creatDate);
						news.put("creatTime", creatTime);
						news.put("sysdrugName", sysdrugName);
						news.put("skitId", skitId);
						news.put("headUrlNew", headUrlNew);
						news.put("huoDongUrl", huoDongUrl);
						news.put("toUserId", toUserId);
						newsService.save(news);
						jPush.sendAll(alias, registrationId, title, type, messageContent, fromUserId, fromRoleFlag,
								toUserId, toRoleFlag, creatDate, creatTime, sysdrugName, skitId, headUrlNew, huoDongUrl,
								huaWeiToken, miRegId, mzPushId, phoneSole);
					}

				}

			}
			retMessage = "拒绝成功";
			result = "0000";
			map.put("retMessage", retMessage);
			map.put("result", result);
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			retMessage = "系统异常";
			map.put("retMessage", retMessage);
			map.put("result", result);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 5.描述：诊所端 诊所管理员 重新分配角色权限
	 * 
	 * @auther 霍学杰
	 * @date 2017年10月13日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/updateauthPlat", method = RequestMethod.POST)
	@ResponseBody
	public Object updateNYflag() {
		logBefore(logger, "重新分配角色权限");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {

			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("authPlat"))
					|| IsNull.paramsIsNull(pd.get("loginId")) || IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData guanPd = new PageData();
				guanPd.put("doctorId", pd.get("loginId"));
				guanPd.put("clinicId", pd.get("clinicId"));
				PageData guanData = clinicService.findzsClinic(guanPd);
				if (IsNull.paramsIsNull(guanData) == false) {
					clinicUserService.editclicyjs(pd);// 角色
					if (pd.get("doctorId").toString().equals(guanData.get("doctorId").toString())) {// 当管理员自己改自己权限是不改
					} else {
						PageData data = doctorService.findDoctorInfo(pd);
						if (IsNull.paramsIsNull(data) == false) {
							Collection<String> alias = new ArrayList();
							Collection<String> registrationId = new ArrayList();

							String title = "权限修改";
							String type = "22";
							String messageContent = "管理员修改了您的权限，请注意查看。";
							String fromUserId = "";
							String fromRoleFlag = "1";
							String toUserId = data.getString("doctorId");
							String toRoleFlag = "1";
							String creatDate = DateUtil.getDay();
							String creatTime = DateUtil.getTime();
							String sysdrugName = "";
							String skitId = pd.get("authPlat").toString();
							String headUrlNew = "";
							String huoDongUrl = "";
							String doctorId = data.getString("doctorId");
							PageData news = new PageData();
							news.put("title", title);
							news.put("type", type);
							news.put("flag", 1);
							news.put("messageContent", messageContent);
							news.put("fromUserId", fromUserId);
							news.put("fromRoleFlag", fromRoleFlag);
							news.put("toRoleFlag", toRoleFlag);
							news.put("creatDate", creatDate);
							news.put("creatTime", creatTime);
							news.put("sysdrugName", sysdrugName);
							news.put("skitId", skitId);
							news.put("headUrlNew", headUrlNew);
							news.put("huoDongUrl", huoDongUrl);
							news.put("toUserId", doctorId);
							newsService.save(news);
							alias.add(doctorId);

							String phoneSole = "";
							String huaWeiToken = "";
							String miRegId = "";
							String mzPushId = "";
							phoneSole = data.getString("phoneSole");
							huaWeiToken = data.getString("huaWeiToken");
							miRegId = data.getString("miRegId");
							mzPushId = data.getString("mzPushId");

							registrationId.add(data.getString("phoneSole"));
							jPush.sendAll(alias, registrationId, title, type, messageContent, fromUserId, fromRoleFlag,
									toUserId, toRoleFlag, creatDate, creatTime, sysdrugName, skitId, headUrlNew,
									huoDongUrl, huaWeiToken, miRegId, mzPushId, phoneSole);
						}
					}
					result = "0000";
					message = "成功";
				} else {
					result = "5002";
					message = CommonMessage.CODE_5002;
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
	 * @description 得到 医生是否还是该诊所成员，如果是，返回信息
	 * @date 2017年11月22日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/isChengYuan")
	@ResponseBody
	public Object isChengYuan() {
		logBefore(logger, "得到 医生是否还是该诊所成员");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				retMessage = "参数异常";
			} else {
				PageData data = clinicUserService.findByTwo(pd);
				if (IsNull.paramsIsNull(data)) {
					result = "8008";
					retMessage = CommonMessage.CODE_8008;// 您已拒绝该成员或该成员已退出！
					map.put("db", new PageData());
				} else {
					result = "0000";
					retMessage = "成功";
					map.put("db", data);
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

}
