package com.fh.controller.app.alipaypay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.config.CommonConfig;
import com.fh.config.CommonMessage;
import com.fh.controller.app.question.AppPatientQuestionController;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ChargeService;
import com.fh.service.app.ClinicCheckinDetailService;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.ConversationLastService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorMoneyDetailService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.DrugOutRecordService;
import com.fh.service.app.DrugclinicService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.NewsService;
import com.fh.service.app.OrderPatiService;
import com.fh.service.app.PatientService;
import com.fh.service.app.WxPayService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.DoubleUtil;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.jPush.examples.jPush;

/**
 * 描述：患者端支付
 * 
 * @time
 */
@Controller
@RequestMapping("/api/patiPay")
public class AppPatiPayController extends BaseController {

	@Resource
	OrderPatiService orderPatiService;
	@Resource
	WxPayService wxPayService;
	@Resource
	ConversationLastService conversationLastService;
	@Resource
	DoctorService doctorService;
	@Resource
	PatientService patientService;
	@Resource
	DoctorMoneyDetailService doctorMoneyDetailService;
	@Resource
	private NewsService newsService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private ClinicCheckinDetailService clinicCheckinDetailService;
	@Resource
	private DrugclinicService drugclinicService;
	@Resource
	private DrugOutRecordService drugOutRecordService;
	@Resource
	private DayCountService dayCountService;
	@Resource
	private MonthCountService monthCountService;
	@Resource
	private ChargeService chargeService;
	@Resource
	private ClinicUserService clinicUserService;
	@Resource
	private ClinicService clinicService;

	/**
	 * app医生端 使用 描述：准备去支付
	 * 
	 * @author 董雪蕊
	 * @date 2018年2月27日
	 * @version 2.0
	 * @return
	 */
	@RequestMapping(value = "/toPay", method = RequestMethod.POST)
	@ResponseBody
	public Object toPay() {
		logBefore(logger, "准备去支付");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000"; // 正常返回
		String retMessage = "服务器暂无响应!";
		try {
			if (IsNull.paramsIsNull(pd.get("patientId")) || IsNull.paramsIsNull(pd.get("shangMoney"))
					|| IsNull.paramsIsNull(pd.get("conversationLastId")) || IsNull.paramsIsNull(pd.get("payStyle"))
					|| IsNull.paramsIsNull(pd.get("payType"))) {
				result = "9993";
				retMessage = CommonMessage.CODE_9993;
			} else {

				if ("xcx".equals(pd.get("payType").toString()) && IsNull.paramsIsNull(pd.get("code"))) {
					result = "9993";
					retMessage = CommonMessage.CODE_9993;
					map.put("result", result);
					map.put("retMessage", retMessage);
					return AppUtil.returnObject(new PageData(), map);
				}
				double shangMoney = Double.parseDouble(pd.get("shangMoney").toString());
				PageData orderPatiData = new PageData();
				if ("1".equals(pd.get("payStyle").toString())) {
					// 查看处方
					pd.put("shangMoney", shangMoney);
					String checkinId = pd.get("conversationLastId").toString();
					// 生成未支付订单表
					orderPatiData.put("orderPatiId", this.get32UUID());
					orderPatiData.put("patientId", pd.get("patientId"));
					orderPatiData.put("conversationLastId", checkinId);// 处方id
					orderPatiData.put("payMoney", pd.get("shangMoney"));
					orderPatiData.put("payType", pd.get("payType"));
					orderPatiData.put("shangMoney", pd.get("shangMoney"));
					orderPatiData.put("payStyle", pd.get("payStyle"));
					orderPatiService.save(orderPatiData);
					orderPatiData.put("title", CommonConfig.PATIENT_PAY_TITLE1);

				} else if ("2".equals(pd.get("payStyle").toString())) {
					// 买药
					if (IsNull.paramsIsNull(pd.get("orderPatiId"))) {
						result = "9993";
						retMessage = CommonMessage.CODE_9993;
						map.put("result", result);
						map.put("retMessage", retMessage);
						return AppUtil.returnObject(new PageData(), map);
					}
					PageData pp = new PageData();
					// 更新订单表
					pp.put("orderPatiId", pd.get("orderPatiId"));
					pp.put("orderNumber", pd.get("orderPatiId"));
					orderPatiData = orderPatiService.findById(pp);
					pp.put("orderPatiId", orderPatiData.get("orderPatiId"));
					pp.put("payType", pd.get("payType"));
					pp.put("payState", 0);
					pp.put("sucesTime", new Date());
					pp.put("payStyle", pd.get("payStyle"));
					orderPatiService.edit(pp);
					pp.remove("orderNumber");
					orderPatiData = orderPatiService.findById(pp);
					orderPatiData.put("title", CommonConfig.PATIENT_PAY_TITLE2);
				} else if ("3".equals(pd.get("payStyle").toString())) {
					// 转账
					// 收款人ID
					String doctorId = pd.get("conversationLastId").toString();
					if (shangMoney < 0) {
						result = "3005";
						retMessage = CommonMessage.CODE_3005;
						map.put("result", result);
						map.put("retMessage", retMessage);
						return AppUtil.returnObject(new PageData(), map);
					} else {
						// 生成未支付订单表
						pd.put("shangMoney", shangMoney);
						orderPatiData.put("orderPatiId", this.get32UUID());
						orderPatiData.put("patientId", pd.get("patientId"));
						orderPatiData.put("conversationLastId", doctorId);// 处方id
						orderPatiData.put("payMoney", pd.get("shangMoney"));
						orderPatiData.put("payType", pd.get("payType"));
						orderPatiData.put("shangMoney", pd.get("shangMoney"));
						orderPatiData.put("payStyle", pd.get("payStyle"));
						orderPatiService.save(orderPatiData);
						orderPatiData.put("title", CommonConfig.PATIENT_PAY_TITLE3);
					}
				} else {
					// 知心医生打赏支付
					if (shangMoney < 1) {
						result = "3004";
						retMessage = CommonMessage.CODE_3004;
						map.put("result", result);
						map.put("retMessage", retMessage);
						return AppUtil.returnObject(new PageData(), map);
					} else {
						pd.put("shangMoney", shangMoney);
						// 生成未支付订单表
						orderPatiData.put("orderPatiId", this.get32UUID());
						orderPatiData.put("patientId", pd.get("patientId"));
						orderPatiData.put("conversationLastId", pd.get("conversationLastId"));
						orderPatiData.put("payMoney", pd.get("shangMoney"));
						orderPatiData.put("payType", pd.get("payType"));
						orderPatiData.put("pingContent", pd.get("pingContent"));
						orderPatiData.put("pingStar", pd.get("pingStar"));
						orderPatiData.put("shangMoney", pd.get("shangMoney"));
						orderPatiService.save(orderPatiData);
						orderPatiData.put("title", CommonConfig.PATIENT_PAY_TITLE);
						orderPatiData.put("payStyle", pd.get("payStyle"));
					}
				}
				// 根据不同的支付类型，调用不同的方法
				if (pd.get("payType").toString().equals("zfb")) {
					String alipayReturnContent = AppPatiZFBAlipayController.zfbPay(orderPatiData);
					map.put("alipayReturnContent", alipayReturnContent);
				} else if (pd.get("payType").toString().equals("wx")) {
					PageData wxData = AppPatiWXAlipayController.wxPay(orderPatiData);
					map.put("wxData", wxData);
				} else if (pd.get("payType").toString().equals("xcx")) {
					orderPatiData.put("code", pd.get("code"));
					PageData wxData = AppPatiWXAlipayController.wxPay2(orderPatiData, patientService);
					map.put("wxData", wxData);
				}
				map.put("orderPatiId", orderPatiData.get("orderPatiId"));
				result = "0000";
				retMessage = "成功";
			}
		} catch (Exception e) {
			result = "9999";
			retMessage = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("retMessage", retMessage);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 处理支付成功后 赏金
	 * 
	 * @throws Exception
	 */
	public void shangPaySuccess(PageData pd, OrderPatiService orderPatiService,
			ConversationLastService conversationLastService, DoctorService doctorService, PatientService patientService,
			DoctorMoneyDetailService doctorMoneyDetailService) throws Exception {

		pd.put("orderPatiId", pd.get("out_trade_no"));
		// 第一步：将此次评价的状态修改
		PageData orderPData = orderPatiService.findById(pd);
		if (IsNull.paramsIsNull(orderPData) == false && IsNull.paramsIsNull(orderPData.get("payState")) == false
				&& 0 == Integer.parseInt(orderPData.get("payState").toString())) {
			orderPData.put("doctorId", orderPData.get("conversationLastId"));
			// 查询医生诊所id
			PageData docData = doctorService.findById(orderPData);
			if (IsNull.paramsIsNull(docData) == false) {
				PageData patiData = patientService.findById(orderPData);
				if (IsNull.paramsIsNull(patiData) == false) {
					// 创建回答表
					String randomNum = AppPatientQuestionController.getPatiNum(orderPData.get("patientId").toString(),
							patientService);
					orderPData.put("patientRandomNum", randomNum);
					orderPData.put("isPing", 1);
					orderPData.put("isShang", 1);
					orderPData.put("clinicId", docData.get("clinicId"));
					orderPData.put("questionId", this.get32UUID());
					conversationLastService.savePingjia(orderPData);
					// 更换订单conversationLastId
					orderPData.put("conversationLastId", orderPData.get("conversationLastId"));
					orderPatiService.editCid(orderPData);
					
					// 第三步：将医生的评价数量和评价星级修改;将医生的余额修改
					PageData convData = conversationLastService.findById(orderPData);
					if (IsNull.paramsIsNull(convData) == false) {

						int pingStar = Integer.parseInt(orderPData.get("pingStar").toString());
						int pingNum = Integer.parseInt(docData.get("pingNum").toString()) + 1;
						int totalPingNum = Integer.parseInt(docData.get("totalPingNum").toString()) + pingStar;
						int totalPingStar = totalPingNum / pingNum;
						docData.put("pingNum", pingNum);
						docData.put("totalPingNum", totalPingNum);
						docData.put("totalPingStar", totalPingStar);
						// 金钱
						double totalMoney = Double.parseDouble(docData.get("totalMoney").toString());
						double remainderMoney = Double.parseDouble(docData.get("remainderMoney").toString());
						double shangMoney = Double.parseDouble(convData.get("shangMoney").toString());
						docData.put("totalMoney", DoubleUtil.add(totalMoney, shangMoney));
						docData.put("remainderMoney", DoubleUtil.add(remainderMoney, shangMoney));
						doctorService.editDoctorInfo(docData);
					}

					// 第四步：将此次订单信息存入 医生交易明细表

					orderPData.put("detailType", 0);
					orderPData.put("money", orderPData.get("payMoney"));
					String trueName = "";
					if (IsNull.paramsIsNull(patiData.get("trueName")) == false) {
						trueName = patiData.get("trueName").toString();
					}
					orderPData.put("explainTitle", trueName + "打赏");
					doctorMoneyDetailService.save(orderPData);

					// 推送消息
					String messageConet = "您的患者" + patiData.get("trueName").toString() + "向您打赏"
							+ orderPData.get("payMoney").toString() + "元!";

					Collection<String> alias = new ArrayList<>();
					Collection<String> registrationId = new ArrayList<>();
					registrationId.add(docData.get("phoneSole").toString());
					String toUserId = docData.get("doctorId").toString();
					alias.add(toUserId);
					String title = "打赏通知";

					String phoneSole = "";
					String huaWeiToken = "";
					String miRegId = "";
					String mzPushId = "";
					phoneSole = docData.getString("phoneSole");
					huaWeiToken = docData.getString("huaWeiToken");
					miRegId = docData.getString("miRegId");
					mzPushId = docData.getString("mzPushId");

					jPush.sendAll(alias, registrationId, title, "52", messageConet,
							patiData.get("patientId").toString(), "0", toUserId, "1", DateUtil.getDay(),
							DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId, phoneSole);

				}
			}
		}
		// 第二步：将订单的支付状态修改
		pd.put("orderPatiId", pd.get("out_trade_no"));
		pd.put("payStyle", "0");
		pd.put("sucesTime", DateUtil.getTime());
		pd.put("payState", "1");
		orderPatiService.edit(pd);
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 查询微信订单支付状态
	 * @date 2017年12月16日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/getWxState")
	@ResponseBody
	public Object getWxState() {
		logBefore(logger, "查询微信订单支付状态");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId")) 
//					|| IsNull.paramsIsNull(pd.get("conversationLastId"))
					|| IsNull.paramsIsNull(pd.get("orderPatiId"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				pd.put("out_trade_no", pd.get("orderPatiId"));
				PageData payData = wxPayService.findById(pd);
				if (IsNull.paramsIsNull(payData)) {
					// 微信交易状态SUCCESS—支付成功 REFUND—转入退款 NOTPAY—未支付 CLOSED—已关闭
					// REVOKED—已撤销（刷卡支付）USERPAYING--用户支付中
					// PAYERROR--支付失败(其他原因，如银行返回失败) NORETURN-未返回-自己定义状态
					// 没有收到微信的回调，去查询
					AppPatiWXAlipayController appPatiWXAlipayController = new AppPatiWXAlipayController();
					String trade_state = appPatiWXAlipayController.getState(pd.get("orderPatiId").toString(),
							wxPayService, orderPatiService, conversationLastService, doctorService, patientService,
							doctorMoneyDetailService);

					map.put("trade_state", trade_state);
				} else {
					map.put("trade_state", payData.get("trade_state"));
				}
				result = "0000";
				retMessage = "查询成功";
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
	 * @description 查询微信订单支付状态 小程序
	 * @date 2017年12月16日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/getWxState2")
	@ResponseBody
	public Object getWxState2() {
		logBefore(logger, "查询微信订单支付状态");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId")) || IsNull.paramsIsNull(pd.get("conversationLastId"))
					|| IsNull.paramsIsNull(pd.get("orderPatiId"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				pd.put("out_trade_no", pd.get("orderPatiId"));
				PageData payData = wxPayService.findById(pd);
				if (IsNull.paramsIsNull(payData)) {
					// 微信交易状态SUCCESS—支付成功 REFUND—转入退款 NOTPAY—未支付 CLOSED—已关闭
					// REVOKED—已撤销（刷卡支付）USERPAYING--用户支付中
					// PAYERROR--支付失败(其他原因，如银行返回失败) NORETURN-未返回-自己定义状态
					// 没有收到微信的回调，去查询
					AppPatiWXAlipayController appPatiWXAlipayController = new AppPatiWXAlipayController();
					String trade_state = appPatiWXAlipayController.getState2(pd.get("orderPatiId").toString(),
							wxPayService, orderPatiService, conversationLastService, doctorService, patientService,
							doctorMoneyDetailService);

					map.put("trade_state", trade_state);
				} else {
					map.put("trade_state", payData.get("trade_state"));
				}
				result = "0000";
				retMessage = "查询成功";
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
	 * 描述:处理支付成功后 查看处方
	 * 
	 * @author 王立飞
	 * @date 2018年7月7日 上午9:06:21
	 */
	public void chuPaySuccess(PageData pd, ClinicUserService clinicUserService,
			Clinic_checkinService clinic_checkinService, ChargeService chargeService, OrderPatiService orderPatiService,
			DoctorService doctorService, PatientService patientService,
			DoctorMoneyDetailService doctorMoneyDetailService, ClinicService clinicService,
			MonthCountService monthCountService, DayCountService dayCountService) throws Exception {
		pd.put("orderPatiId", pd.get("out_trade_no"));
		// 查询患者订单表
		PageData orderPData = orderPatiService.findById(pd);
		if (IsNull.paramsIsNull(orderPData) == false && IsNull.paramsIsNull(orderPData.get("payState")) == false
				&& 0 == Integer.parseInt(orderPData.get("payState").toString())) {
			pd.put("checkinId", orderPData.get("conversationLastId"));
			// 查询处方信息
			PageData ChuData = clinic_checkinService.findByidxq(pd);
			if (IsNull.paramsIsNull(ChuData) == false) {
				// 得到诊所管理员
				pd.put("clinicId", ChuData.get("clinicId"));
				PageData GLY = clinicUserService.findByGLY(pd);
				PageData docData = doctorService.findById(GLY);
				orderPData.put("doctorId", docData.get("doctorId"));
				if (IsNull.paramsIsNull(docData) == false) {
					// 金钱
					double ZStotalMoney = Double.parseDouble(docData.get("ZStotalMoney").toString());
					double ZSremainderMoney = Double.parseDouble(docData.get("ZSremainderMoney").toString());
					double shangMoney = Double.parseDouble(orderPData.get("shangMoney").toString());
					docData.put("ZStotalMoney", DoubleUtil.add(ZStotalMoney, shangMoney));
					docData.put("ZSremainderMoney", DoubleUtil.add(ZSremainderMoney, shangMoney));
					doctorService.editDoctorInfo(docData);

					// 将此次订单信息存入 医生交易明细表
					PageData patiData = patientService.findById(orderPData);
					if (IsNull.paramsIsNull(patiData) == false) {
						orderPData.put("detailType", 0);
						orderPData.put("money", orderPData.get("payMoney"));
						orderPData.put("moneyType", "1");
						String trueName = "";
						if (IsNull.paramsIsNull(patiData.get("trueName")) == false) {
							trueName = patiData.get("trueName").toString();
						}
						orderPData.put("explainTitle", trueName + "查看处方");
						doctorMoneyDetailService.save(orderPData);
					}
					// 推送消息
					// 查询开方医生信息
					PageData doctorInfo = doctorService.findDoctorInfo(ChuData);
					if (doctorInfo != null) {
						int roleFlag = Integer.parseInt(doctorInfo.get("roleFlag").toString());
						int authPlat = Integer.parseInt(doctorInfo.get("authPlat").toString());
						if (roleFlag != 1 && authPlat == 1) {
							String messageConet = patiData.get("trueName").toString() + "已查看您发送的处方笺!";
							Collection<String> alias = new ArrayList<>();
							Collection<String> registrationId = new ArrayList<>();
							registrationId.add(doctorInfo.get("phoneSole").toString());
							String toUserId = doctorInfo.get("doctorId").toString();
							alias.add(toUserId);
							String title = "查看处方通知";

							String phoneSole = "";
							String huaWeiToken = "";
							String miRegId = "";
							String mzPushId = "";
							phoneSole = doctorInfo.getString("phoneSole");
							huaWeiToken = doctorInfo.getString("huaWeiToken");
							miRegId = doctorInfo.getString("miRegId");
							mzPushId = doctorInfo.getString("mzPushId");

							jPush.sendAll(alias, registrationId, title, "55", messageConet,
									patiData.get("patientId").toString(), "0", toUserId, "1", DateUtil.getDay(),
									DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId, phoneSole);
						}
					}
					// 查询诊所所有成员
					PageData ss = new PageData();
					ss.put("clinicId", ChuData.get("clinicId"));
					ss.put("ss", "ss");
					List<PageData> docList = clinicUserService.findcylist(ss);
					for (int i = 0; i < docList.size(); i++) {
						PageData doc = docList.get(i);
						int roleFlag = Integer.parseInt(doc.get("roleFlag").toString());
						if (roleFlag == 1) {
							// 管理员
							String messageConet = "患者" + patiData.get("trueName").toString() + "已支付"
									+ orderPData.get("payMoney").toString() + "元,查看处方!";
							Collection<String> alias = new ArrayList<>();
							Collection<String> registrationId = new ArrayList<>();
							registrationId.add(doc.get("phoneSole").toString());
							String toUserId = doc.get("doctorId").toString();
							alias.add(toUserId);
							String title = "处方支付通知";

							String phoneSole = "";
							String huaWeiToken = "";
							String miRegId = "";
							String mzPushId = "";
							phoneSole = doc.getString("phoneSole");
							huaWeiToken = doc.getString("huaWeiToken");
							miRegId = doc.getString("miRegId");
							mzPushId = doc.getString("mzPushId");

							jPush.sendAll(alias, registrationId, title, "54", messageConet,
									patiData.get("patientId").toString(), "0", toUserId, "1", DateUtil.getDay(),
									DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId, phoneSole);
						} else {
							// 医生 平台权限 0.没有权限 1.医生 2.医生、收费员 3.收费员 4.收费员、药剂员、药库管理
							// 5.药剂员 、药库管理6.医生、收费、药剂
							int authPlat = Integer.parseInt(doc.get("authPlat").toString());
							if (authPlat == 2 || authPlat == 3 || authPlat == 4 || authPlat == 6) {
								String messageConet = "患者" + patiData.get("trueName").toString() + "已支付"
										+ orderPData.get("payMoney").toString() + "元,查看处方!";
								Collection<String> alias = new ArrayList<>();
								Collection<String> registrationId = new ArrayList<>();
								registrationId.add(doc.get("phoneSole").toString());
								String toUserId = doc.get("doctorId").toString();
								alias.add(toUserId);
								String title = "处方支付通知";

								String phoneSole = "";
								String huaWeiToken = "";
								String miRegId = "";
								String mzPushId = "";
								phoneSole = doc.getString("phoneSole");
								huaWeiToken = doc.getString("huaWeiToken");
								miRegId = doc.getString("miRegId");
								mzPushId = doc.getString("mzPushId");

								jPush.sendAll(alias, registrationId, title, "54", messageConet,
										patiData.get("patientId").toString(), "0", toUserId, "1", DateUtil.getDay(),
										DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId, phoneSole);
							}
						}
					}

					// 将处方状态修改
					PageData dd = new PageData();
					dd.put("checkinId", orderPData.get("conversationLastId"));
					dd.put("chuState", 1);
					clinic_checkinService.updatestate(dd);

					// 收费表
					PageData data = chargeService.findByID(dd);// 查询收费表
					PageData charg = new PageData();
					if (data != null) {
						// 存在,更新到收费表
						charg.put("chuMoney", shangMoney);
						charg.put("chuState", 1);// 已支付
						charg.put("checkinId", orderPData.get("conversationLastId"));
						chargeService.updateCharge2(charg);
					} else {
						// 不存在,添加到收费表
						charg.put("chuMoney", shangMoney);
						charg.put("chuState", 1);// 已支付
						charg.put("chargeId", this.get32UUID());
						charg.put("checkinId", orderPData.get("conversationLastId"));
						charg.put("clinicId", ChuData.get("clinicId"));
						charg.put("charDoctorId", docData.get("doctorId"));// 管理员
						charg.put("charDoctorName", docData.get("trueName"));// 管理员
						charg.put("clinicName", ChuData.get("clinicName"));
						charg.put("resultdoctor", ChuData.get("doctorName"));// 接诊人姓名
						charg.put("patientId", ChuData.get("patientId"));
						charg.put("headUrl", ChuData.get("headUrl"));
						charg.put("patientName", ChuData.get("patientName"));
						charg.put("chargeState", 0);
						charg.put("medicameState", 0);
						charg.put("patientVisit", ChuData.get("patientVisit"));
						charg.put("pAge", ChuData.get("patientAge"));
						charg.put("pSex", ChuData.get("patientSex"));
						charg.put("symptom", ChuData.get("patientCondition"));
						charg.put("xDrugList", " ");
						charg.put("zDrugList", " ");
						charg.put("createTime", DateUtil.getTime());
						chargeService.save(charg);
					}
					// 更新医生接诊总数 和诊所接诊总数
					allCount(ChuData, clinic_checkinService, doctorService, clinicService, monthCountService,
							dayCountService);
					// 统计资金收益
					double money = shangMoney;
					double moneyCh = 0;
					double moneyWe = 0;
					double chuMoney = shangMoney;
					String clinicId = ChuData.get("clinicId").toString();
					moneyIn(clinicId, moneyCh, moneyWe, chuMoney, money, monthCountService, dayCountService);// 走接口
				}
			}
		}
		// 将订单的支付状态修改
		pd.put("orderPatiId", pd.get("out_trade_no"));
		pd.put("payStyle", 1);
		pd.put("sucesTime", DateUtil.getTime());
		pd.put("payState", 1);
		orderPatiService.edit(pd);
	}

	/**
	 * 描述:处理支付成功后 买药
	 * 
	 * @author 王立飞
	 * @date 2018年7月7日 上午9:06:21
	 */
	public void yaoPaySuccess(PageData pd, ClinicUserService clinicUserService,
			Clinic_checkinService clinic_checkinService, ChargeService chargeService, OrderPatiService orderPatiService,
			DoctorService doctorService, PatientService patientService,
			DoctorMoneyDetailService doctorMoneyDetailService, MonthCountService monthCountService,
			DayCountService dayCountService) throws Exception {

		pd.put("orderPatiId", pd.get("out_trade_no"));
		// 查询患者订单表
		PageData orderPData = orderPatiService.findById(pd);
		if (IsNull.paramsIsNull(orderPData) == false && IsNull.paramsIsNull(orderPData.get("payState")) == false
				&& 0 == Integer.parseInt(orderPData.get("payState").toString())) {
			pd.put("checkinId", orderPData.get("conversationLastId"));
			// 查询处方信息
			PageData ChuData = clinic_checkinService.findByidxq(pd);
			if (IsNull.paramsIsNull(ChuData) == false) {
				pd.put("clinicId", ChuData.get("clinicId"));
				// 得到诊所管理员
				PageData GLY = clinicUserService.findByGLY(pd);
				PageData docData = doctorService.findById(GLY);
				orderPData.put("doctorId", docData.get("doctorId"));
				if (IsNull.paramsIsNull(docData) == false) {
					// 金钱
					double ZStotalMoney = Double.parseDouble(docData.get("ZStotalMoney").toString());
					double ZSfreezeMoney = Double.parseDouble(docData.get("ZSfreezeMoney").toString());
					double shangMoney = Double.parseDouble(orderPData.get("shangMoney").toString());
					docData.put("ZStotalMoney", DoubleUtil.add(ZStotalMoney, shangMoney));
					docData.put("ZSfreezeMoney", DoubleUtil.add(ZSfreezeMoney, shangMoney));// 冻结金额
					doctorService.editDoctorInfo(docData);

					// 将此次订单信息存入 医生交易明细表
					PageData patiData = patientService.findById(orderPData);
					if (IsNull.paramsIsNull(patiData) == false) {
						orderPData.put("detailType", 0);
						orderPData.put("money", orderPData.get("payMoney"));
						orderPData.put("moneyType", 1);
						String trueName = "";
						if (IsNull.paramsIsNull(patiData.get("trueName")) == false) {
							trueName = patiData.get("trueName").toString();
						}
						orderPData.put("explainTitle", trueName + "在线购药");
						doctorMoneyDetailService.save(orderPData);
					}
					double money = shangMoney;
					double moneyCh = 0;
					double moneyWe = 0;
					double chuMoney = 0;
					if (IsNull.paramsIsNull(ChuData.get("moneyCh")) == false) {
						moneyCh = Double.parseDouble(ChuData.get("moneyCh").toString());
					}
					if (IsNull.paramsIsNull(ChuData.get("moneyWe")) == false) {
						moneyWe = Double.parseDouble(ChuData.get("moneyWe").toString());
					}
					String clinicId = docData.get("clinicId").toString();
					// 统计资金收益
					moneyIn(clinicId, moneyCh, moneyWe, chuMoney, money, monthCountService, dayCountService);// 走接口

					// 推送消息
					// 查询开方医生信息
					PageData doctorInfo = doctorService.findDoctorInfo(ChuData);
					String docName = "";
					if (doctorInfo != null) {
						docName = doctorInfo.getString("trueName");
						int roleFlag = Integer.parseInt(doctorInfo.get("roleFlag").toString());
						int authPlat = Integer.parseInt(doctorInfo.get("authPlat").toString());
						if (roleFlag != 1 && authPlat == 1) {
							String messageConet = patiData.get("trueName").toString() + "已购买您处方上的药品，请注意及时处理。";
							Collection<String> alias = new ArrayList<>();
							Collection<String> registrationId = new ArrayList<>();
							registrationId.add(doctorInfo.get("phoneSole").toString());
							String toUserId = doctorInfo.get("doctorId").toString();
							alias.add(toUserId);
							String title = "在线购药通知";

							String phoneSole = "";
							String huaWeiToken = "";
							String miRegId = "";
							String mzPushId = "";
							phoneSole = doctorInfo.getString("phoneSole");
							huaWeiToken = doctorInfo.getString("huaWeiToken");
							miRegId = doctorInfo.getString("miRegId");
							mzPushId = doctorInfo.getString("mzPushId");

							jPush.sendAll(alias, registrationId, title, "56", messageConet,
									patiData.get("patientId").toString(), "0", toUserId, "1", DateUtil.getDay(),
									DateUtil.getTime(), "", pd.getString("checkinId"), "", "", huaWeiToken, miRegId,
									mzPushId, phoneSole);
						}
					}
					// 查询诊所所有成员
					PageData ss = new PageData();
					ss.put("clinicId", ChuData.get("clinicId"));
					ss.put("ss", "ss");
					List<PageData> docList = clinicUserService.findcylist(ss);
					for (int i = 0; i < docList.size(); i++) {
						PageData doc = docList.get(i);
						int roleFlag = Integer.parseInt(doc.get("roleFlag").toString());
						if (roleFlag == 1) {
							// 管理员
							String messageConet = "患者" + patiData.get("trueName").toString() + "在" + docName
									+ "医生处,在线购买了药品,共计" + orderPData.get("payMoney").toString() + "元.";
							Collection<String> alias = new ArrayList<>();
							Collection<String> registrationId = new ArrayList<>();
							registrationId.add(doc.get("phoneSole").toString());
							String toUserId = doc.get("doctorId").toString();
							alias.add(toUserId);
							String title = "在线购药通知";

							String phoneSole = "";
							String huaWeiToken = "";
							String miRegId = "";
							String mzPushId = "";
							phoneSole = doc.getString("phoneSole");
							huaWeiToken = doc.getString("huaWeiToken");
							miRegId = doc.getString("miRegId");
							mzPushId = doc.getString("mzPushId");

							jPush.sendAll(alias, registrationId, title, "56", messageConet,
									patiData.get("patientId").toString(), "0", toUserId, "1", DateUtil.getDay(),
									DateUtil.getTime(), "", pd.getString("checkinId"), "", "", huaWeiToken, miRegId,
									mzPushId, phoneSole);
						} else {
							// 医生 平台权限 0.没有权限 1.医生 2.医生、收费员 3.收费员 4.收费员、药剂员、药库管理
							// 5.药剂员 、药库管理6.医生、收费、药剂
							int authPlat = Integer.parseInt(doc.get("authPlat").toString());
							if (authPlat == 2 || authPlat == 3 || authPlat == 4 || authPlat == 5 || authPlat == 6) {
								String messageConet = "患者" + patiData.get("trueName").toString() + "在" + docName
										+ "医生处,在线购买了药品,共计" + orderPData.get("payMoney").toString() + "元.";
								Collection<String> alias = new ArrayList<>();
								Collection<String> registrationId = new ArrayList<>();
								registrationId.add(doc.get("phoneSole").toString());
								String toUserId = doc.get("doctorId").toString();
								alias.add(toUserId);
								String title = "在线购药通知";

								String phoneSole = "";
								String huaWeiToken = "";
								String miRegId = "";
								String mzPushId = "";
								phoneSole = doc.getString("phoneSole");
								huaWeiToken = doc.getString("huaWeiToken");
								miRegId = doc.getString("miRegId");
								mzPushId = doc.getString("mzPushId");

								jPush.sendAll(alias, registrationId, title, "56", messageConet,
										patiData.get("patientId").toString(), "0", toUserId, "1", DateUtil.getDay(),
										DateUtil.getTime(), "", pd.getString("checkinId"), "", "", huaWeiToken, miRegId,
										mzPushId, phoneSole);
							}
						}
					}
					// 将订单的支付状态修改
					pd.put("orderPatiId", pd.get("out_trade_no"));
					pd.put("payStyle", 2);// 买药
					pd.put("sucesTime", DateUtil.getTime());
					pd.put("payState", 1);// 支付
					pd.put("yes", "1");// 过期时间
//					pd.put("exceedTime", "");// 过期时间
					orderPatiService.edit(pd);

					// 将处方状态修改
					PageData dd = new PageData();
					dd.put("checkinId", orderPData.get("conversationLastId"));
					dd.put("chargeState", 1);
					dd.put("chargeTime", DateUtil.getTime());
					clinic_checkinService.updatestate(dd);

					PageData chargeDate = chargeService.findByID(dd);

					// 更新收费表
					PageData pp = new PageData();
					pp.put("checkinId", orderPData.get("conversationLastId"));
					pp.put("chargeState", 1);
					pp.put("chargeTime", DateUtil.getTime());

					pd.put("chargeId", chargeDate.get("chargeId"));
					pd.put("medFlag", "1");
					pd.put("medTime", DateUtil.getTime());
					pd.put("medDoctorName", doctorInfo.get("trueName"));
					pd.put("medDoctorId", doctorInfo.get("doctorId"));
					chargeService.updateCharge(pd);
				}
			}
		}
	}

	/**
	 * 描述:处理支付成功后 在线转账
	 * 
	 * @author 王立飞
	 * @date 2018年7月31日 上午9:07:55
	 */
	public void zhuanPaySuccess(PageData pd, OrderPatiService orderPatiService,
			ConversationLastService conversationLastService, DoctorService doctorService, PatientService patientService,
			DoctorMoneyDetailService doctorMoneyDetailService) throws Exception {

		pd.put("orderPatiId", pd.get("out_trade_no"));
		// 查询订单
		PageData orderPData = orderPatiService.findById(pd);
		if (IsNull.paramsIsNull(orderPData) == false && IsNull.paramsIsNull(orderPData.get("payState")) == false
				&& 0 == Integer.parseInt(orderPData.get("payState").toString())) {
			// 将医生的余额修改
			orderPData.put("doctorId", orderPData.get("conversationLastId"));
			PageData docData = doctorService.findById(orderPData);
			if (IsNull.paramsIsNull(docData) == false) {
				// 金钱
				double totalMoney = Double.parseDouble(docData.get("totalMoney").toString());
				double remainderMoney = Double.parseDouble(docData.get("remainderMoney").toString());
				double shangMoney = Double.parseDouble(orderPData.get("shangMoney").toString());
				docData.put("totalMoney", DoubleUtil.add(totalMoney, shangMoney));
				docData.put("remainderMoney", DoubleUtil.add(remainderMoney, shangMoney));
				doctorService.editDoctorInfo(docData);
			}
			// 将此次订单信息存入 医生交易明细表
			PageData patiData = patientService.findById(orderPData);
			if (IsNull.paramsIsNull(patiData) == false) {
				orderPData.put("detailType", 0);
				orderPData.put("money", orderPData.get("payMoney"));
				String trueName = "";
				if (IsNull.paramsIsNull(patiData.get("trueName")) == false) {
					trueName = patiData.get("trueName").toString();
				}
				orderPData.put("explainTitle", trueName + "在线转账");
				orderPData.put("moneyType", "0");
				doctorMoneyDetailService.save(orderPData);
			}
			pd.put("doctorId", orderPData.get("doctorId"));
			PageData doctorInfo = doctorService.findDoctorInfo(pd);
			// 推送消息
			String messageConet = "您的患者" + patiData.get("trueName").toString() + "向您转账"
					+ orderPData.get("payMoney").toString() + "元!";

			Collection<String> alias = new ArrayList<>();
			Collection<String> registrationId = new ArrayList<>();
			registrationId.add(doctorInfo.get("phoneSole").toString());
			String toUserId = doctorInfo.get("doctorId").toString();
			alias.add(toUserId);
			String title = "转账通知";

			String phoneSole = "";
			String huaWeiToken = "";
			String miRegId = "";
			String mzPushId = "";
			phoneSole = doctorInfo.getString("phoneSole");
			huaWeiToken = doctorInfo.getString("huaWeiToken");
			miRegId = doctorInfo.getString("miRegId");
			mzPushId = doctorInfo.getString("mzPushId");

			jPush.sendAll(alias, registrationId, title, "58", messageConet, patiData.get("patientId").toString(), "0",
					toUserId, "1", DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
					mzPushId, phoneSole);
		}
		// 第二步：将订单的支付状态修改
		pd.put("orderPatiId", pd.get("out_trade_no"));
		pd.put("payStyle", "3");
		pd.put("sucesTime", DateUtil.getTime());
		pd.put("payState", "1");
		orderPatiService.edit(pd);
	}

	/**
	 * 统计资金收益
	 * 
	 * @throws Exception
	 */
	public void moneyIn(String clinicId, double inMoneyCh, double inMoneyWe, double inMoneyOther, double inMoney,
			MonthCountService monthCountService, DayCountService dayCountService) throws Exception {
		// 加数据
		PageData pd = new PageData();
		pd.put("clinicId", clinicId);
		String day = DateUtil.getDay();// 2017-01-01
		pd.put("year", day.substring(0, 4));
		pd.put("month", day.substring(5, 7));
		pd.put("inMoneyCh", inMoneyCh);
		pd.put("inMoneyWe", inMoneyWe);
		pd.put("inMoneyOther", inMoneyOther);
		pd.put("inMoney", inMoney);
		pd.put("inMoneyYear", inMoney);
		pd.put("inMoneyChYear", inMoneyCh);
		pd.put("inMoneyWeYear", inMoneyWe);
		pd.put("inMoneyOtherYear", inMoneyOther);
		pd.put("day", day.substring(5));
		// 处理逻辑
		PageData dayData = dayCountService.findDaysj2(pd);
		// 天数据
		if (IsNull.paramsIsNull(dayData) == false) {
			pd.put("cdayId", dayData.get("cdayId"));
			pd.put("inMoneyCh", DoubleUtil.add(inMoneyCh, Double.parseDouble(dayData.get("inMoneyCh").toString())));
			pd.put("inMoneyWe", DoubleUtil.add(inMoneyWe, Double.parseDouble(dayData.get("inMoneyWe").toString())));
			pd.put("inMoneyOther",
					DoubleUtil.add(inMoneyOther, Double.parseDouble(dayData.get("inMoneyOther").toString())));
			pd.put("inMoney", DoubleUtil.add(inMoney, Double.parseDouble(dayData.get("inMoney").toString())));
			dayCountService.updateCount2(pd);
		}
		// 月数据
		PageData monData = monthCountService.findClinicsy2(pd);
		if (IsNull.paramsIsNull(monData) == false) {// 必不为空
			pd.put("cmonthId", monData.get("cmonthId"));
			pd.put("inMoneyCh", DoubleUtil.add(inMoneyCh, Double.parseDouble(monData.get("inMoneyCh").toString())));
			pd.put("inMoneyWe", DoubleUtil.add(inMoneyWe, Double.parseDouble(monData.get("inMoneyWe").toString())));
			pd.put("inMoneyOther",
					DoubleUtil.add(inMoneyOther, Double.parseDouble(monData.get("inMoneyOther").toString())));
			pd.put("inMoney", DoubleUtil.add(inMoney, Double.parseDouble(monData.get("inMoney").toString())));
			pd.put("inMoneyYear", DoubleUtil.add(inMoney, Double.parseDouble(monData.get("inMoneyYear").toString())));
			pd.put("inMoneyOtherYear",
					DoubleUtil.add(inMoneyOther, Double.parseDouble(monData.get("inMoneyOtherYear").toString())));
			pd.put("inMoneyChYear",
					DoubleUtil.add(inMoneyCh, Double.parseDouble(monData.get("inMoneyChYear").toString())));
			pd.put("inMoneyWeYear",
					DoubleUtil.add(inMoneyWe, Double.parseDouble(monData.get("inMoneyWeYear").toString())));
			monthCountService.updateCount2(pd);
		}
	}

	/**
	 * 将所有的医生 就诊数量+1，重复的医生不加 将诊所的接诊量+1 将所有医生加上就诊记录 添加后台统计
	 * 
	 * @throws Exception
	 */
	public void allCount(PageData checkInPd, Clinic_checkinService clinic_checkinService, DoctorService doctorService,
			ClinicService clinicService, MonthCountService monthCountService, DayCountService dayCountService)
			throws Exception {
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
				addRecord(checkInPd, doctorId, monthCountService, dayCountService);
			}
		}
	}

	public void addRecord(PageData checkInPd, String doctorId, MonthCountService monthCountService,
			DayCountService dayCountService) throws Exception {
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
			dd = editMonth(flag, findClinicsy2);
			dd.put("doctorId", doctorId);
			dd.put("cmonthId", findClinicsy2.get("cmonthId"));
			monthCountService.updateCount2(dd);
			if (findminthsj != null) {// 个人月
				// 不为空更新个人月
				dd = editMonth(flag, findminthsj);
				dd.put("doctorId", doctorId);
				dd.put("monthId", findminthsj.get("monthId"));
				monthCountService.updateCount1(dd);
				if (findDaysj2 != null) {
					// 不为空更新诊所天
					dd = editDay(flag, findDaysj2);
					dd.put("doctorId", doctorId);
					dd.put("cdayId", findDaysj2.get("cdayId"));
					dayCountService.updateCount2(dd);
					if (findDaysj != null) {// 不为空
						// 不为空更新个人天
						dd = editDay(flag, findDaysj);
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
					dd = editDay(flag, findDaysj2);
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
	 * 描述： 诊所接诊数据统计公共方法
	 * 
	 * @param 霍学杰
	 * @param     2017.9.20
	 * @return int
	 */
	public static PageData editMonth(int flag, PageData data) {
		PageData pd = new PageData();
		try {
			pd.put("year", DateUtil.getYear());
			pd.put("clinicId", data.get("clinicId"));
			pd.put("month", DateUtil.getMonth());
			pd.put("day", DateUtil.getDay1());
			if (flag == 1) {// 复诊
				pd.put("fuMonthCount", Integer.parseInt(data.get("fuMonthCount").toString()) + 1);
				pd.put("fuYearCount", Integer.parseInt(data.get("fuYearCount").toString()) + 1);
				pd.put("sumMonthCount", Integer.parseInt(data.get("sumMonthCount").toString()) + 1);
				pd.put("sumYearCount", Integer.parseInt(data.get("sumYearCount").toString()) + 1);
			} else if (flag == 0) {// 初诊
				pd.put("chuMonthCount", Integer.parseInt(data.get("chuMonthCount").toString()) + 1);
				pd.put("chuYearCount", Integer.parseInt(data.get("chuYearCount").toString()) + 1);
				pd.put("sumMonthCount", Integer.parseInt(data.get("sumMonthCount").toString()) + 1);
				pd.put("sumYearCount", Integer.parseInt(data.get("sumYearCount").toString()) + 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pd;
	}

	public static PageData editDay(int flag, PageData data) {
		PageData pd = new PageData();
		try {
			pd.put("year", DateUtil.getYear());
			pd.put("clinicId", data.get("clinicId"));
			pd.put("month", DateUtil.getMonth());
			pd.put("day", DateUtil.getDay1());
			pd.put("hour", "hour" + DateUtil.getNowHour());// 不是数据库字段，只是为了添加时使用
			if (flag == 1) {// 复诊
				pd.put("fuCount", Integer.parseInt(data.get("fuCount").toString()) + 1);
				pd.put("sumCount", Integer.parseInt(data.get("sumCount").toString()) + 1);
			} else if (flag == 0) {// 初诊
				pd.put("chuCount", Integer.parseInt(data.get("chuCount").toString()) + 1);
				pd.put("sumCount", Integer.parseInt(data.get("sumCount").toString()) + 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pd;
	}
}
