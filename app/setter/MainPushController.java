package com.fh.controller.app.setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ClinicService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.CloudSignService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.NewPushService;
import com.fh.service.app.NewsService;
import com.fh.service.app.PatientService;
import com.fh.service.app.QuestionService;
import com.fh.service.app.SBYYPOSTPHONEService;
import com.fh.service.app.WxTokenService;
import com.fh.service.statistics.DoctPatService;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.jPush.examples.jPush;
import com.fh.util.weixin.openId;

@Component
public class MainPushController extends BaseController implements ApplicationListener<ContextRefreshedEvent> {
	/**
	 * 推送的主类 1.先查询库里医生没有推送的消息 2.再查询库里患者没有推送的消息 3.检查有没有提醒的问题提醒 4.检查有没有聊天提醒
	 */
	private long timeInterval = 1000 * 50;// 50s
	int i = 0;
	// 记录下需要推送的日期，如果今天的发了，日期改为明天
	String date = DateUtil.getDay();// 日期格式 2017-09-23
	String month = DateUtil.getMonth();// 日期格式 05
	String year = DateUtil.getYear();// 日期格式 2017
	@Resource
	private NewsService newsService;
	@Resource
	private DoctorService doctorService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private NewPushService newPushService;
	@Resource
	private PatientService patientService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private WxTokenService wxTokenService;
	@Resource
	private QuestionService questionService;
	@Resource
	private DoctPatService doctPatService;
	@Resource
	private SBYYPOSTPHONEService sbyypostphoneService;
	@Resource
	private CloudSignService cloudSignService;

	public void onApplicationEvent(ContextRefreshedEvent event) {
		i = i + 1;
		if (event.getApplicationContext().getParent() == null) {// root application context 没有parent，他就是老大.
			if (i == 1) {
				Runnable runnable = new Runnable() {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					public void run() {
						while (true) {
							try {
								// 1.先查询库里医生没有推送的消息
								List<PageData> newsList = newsService.findNoPush(null);
								if (newsList != null && newsList.size() > 0) {
									for (int i = 0; i < newsList.size(); i++) {
										PageData newsData = newsList.get(i);
										try {
											if (!"0".equals(newsData.get("phoneSole").toString())
													|| !"0".equals(newsData.get("huaWeiToken").toString())
													|| !"0".equals(newsData.get("miRegId").toString())
													|| !"0".equals(newsData.get("mzPushId").toString())) {
												int type = Integer.parseInt(newsData.get("type").toString());
												switch (type) {
												case 33:// 系统处理预约超时的。
													// [33] 预约挂号失败通知 由于xxx患者的预约挂号在一小时内未能及时处理，系统已帮您自动拒绝本次预约挂号。（系统处理医生未接的诊
													// 给医生）
													Collection<String> alias = new ArrayList<>();
													Collection<String> registrationId = new ArrayList<>();
													alias.add(newsData.getString("toUserId"));
													registrationId.add(newsData.get("phoneSole").toString());

													String phoneSole = "";
													String huaWeiToken = "";
													String miRegId = "";
													String mzPushId = "";
													phoneSole = newsData.getString("phoneSole");
													huaWeiToken = newsData.getString("huaWeiToken");
													miRegId = newsData.getString("miRegId");
													mzPushId = newsData.getString("mzPushId");

													jPush.sendAll(alias, registrationId, "挂号消息",
															newsData.get("type").toString(),
															newsData.getString("messageContent"), "", "0",
															newsData.getString("toUserId"), "1", DateUtil.getDay(),
															DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
															mzPushId, phoneSole);
													// 修改挂号信息
													PageData pd = new PageData();
													pd.put("state", 3);
													pd.put("yuState", 5);
													pd.put("checkinId", newsData.get("skitId"));
													clinic_checkinService.cancelChecIn(pd);
													break;
												default:
													Collection<String> alias2 = new ArrayList<>();
													Collection<String> registrationId2 = new ArrayList<>();
													alias2.add(newsData.getString("toUserId"));
													registrationId2.add(newsData.get("phoneSole").toString());

													phoneSole = newsData.getString("phoneSole");
													huaWeiToken = newsData.getString("huaWeiToken");
													miRegId = newsData.getString("miRegId");
													mzPushId = newsData.getString("mzPushId");

													jPush.sendAll(alias2, registrationId2, "系统消息",
															newsData.get("type").toString(),
															newsData.getString("messageContent"), "", "0",
															newsData.getString("toUserId"), "1", DateUtil.getDay(),
															DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
															mzPushId, phoneSole);
													break;
												}
											}
											newsData.put("flag", 1);
											newsService.updateFlag(newsData);
											Thread.sleep(101);
										} catch (Exception e) {
											newsData.put("flag", 1);
											newsService.updateFlag(newsData);
											Thread.sleep(101);
										}

									}
								}
								// 2.再查询库里患者没有推送的消息
								List<PageData> newsList2 = newsService.findNoPushPat(null);
								if (newsList2 != null) {
									for (int i = 0; i < newsList2.size(); i++) {
										PageData newsData = newsList2.get(i);
										try {
											if (IsNull.paramsIsNull(newsData) == false) {
												// 系统处理预约超时的。
												// [32] 预约挂号失败通知 由于xxx诊所对您的预约挂号在一小时内未能及时作出回应，系统取消了本次预约挂号（系统处理医生未接的诊 给患者）
												// 去发送模板消息
												newsData.put("checkinId", newsData.get("skitId"));
												PageData cliInData = clinic_checkinService.findByidxq(newsData);

												openId.sendPatientMu(newsData.getString("toUserId"),
														cliInData.getString("clinicId"),
														cliInData.getString("doctorId"), 1, "", patientService,
														newPushService, wxTokenService, clinicService, doctorService,
														"", "", cliInData);
												// app
												String phoneSole = "";
												String huaWeiToken = "";
												String miRegId = "";
												String mzPushId = "";
												phoneSole = newsData.getString("phoneSole");
												huaWeiToken = newsData.getString("huaWeiToken");
												miRegId = newsData.getString("miRegId");
												mzPushId = newsData.getString("mzPushId");

												Collection<String> alias2 = new ArrayList();
												Collection<String> registrationId2 = new ArrayList();
												if (!"0".equals(newsData.get("phoneSole").toString())
														|| !"0".equals(newsData.get("huaWeiToken").toString())
														|| !"0".equals(newsData.get("miRegId").toString())
														|| !"0".equals(newsData.get("mzPushId").toString())) {
													alias2.add(newsData.get("toUserId").toString());
													registrationId2.add(newsData.getString("phoneSole"));
													jPush.sendAllPat(alias2, registrationId2,
															newsData.getString("title"),
															newsData.get("type").toString(),
															newsData.getString("messageContent"), "", "0",
															newsData.get("toUserId").toString(), "2", DateUtil.getDay(),
															DateUtil.getTime(), "", newsData.getString("skitId"), "",
															"", huaWeiToken, miRegId, mzPushId, phoneSole);
												}
											}
											newsData.put("flag", 1);
											newsService.updateFlag(newsData);
											Thread.sleep(101);
										} catch (Exception e) {
											newsData.put("flag", 1);
											newsService.updateFlag(newsData);
											Thread.sleep(101);
										}

									}
								}
								// 3.检查有没有提醒的问题提醒
								List<PageData> quesList = questionService.listByAlert(null);
								if (quesList != null && quesList.size() > 0) {
									for (int i = 0; i < quesList.size(); i++) {
										PageData quesData = quesList.get(i);
										try {
											PageData patiData = patientService.findById(quesData);
											if (IsNull.paramsIsNull(patiData) == false) {
												String title = "问题暂未回复通知";
												String content = "医生现在比较忙，不能及时对您的问题进行回复，您重新选择医生进行提问。";
												String type = "50";
												Collection<String> alias2 = new ArrayList();
												Collection<String> registrationId2 = new ArrayList();
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

													alias2.add(patiData.get("patientId").toString());
													registrationId2.add(patiData.getString("phoneSole"));
													jPush.sendAllPat(alias2, registrationId2, title, type, content, "",
															"0", patiData.get("patientId").toString(), "2",
															DateUtil.getDay(), DateUtil.getTime(), "",
															quesData.getString("questionId"), "", "", huaWeiToken,
															miRegId, mzPushId, phoneSole);
													PageData newPd = new PageData();
													newPd.put("title", title);
													newPd.put("type", type);
													newPd.put("flag", 1);
													newPd.put("messageContent", content);
													newPd.put("fromUserId", "");
													newPd.put("fromRoleFlag", 0);
													newPd.put("toUserId", patiData.get("patientId").toString());
													newPd.put("toRoleFlag", 2);
													newPd.put("creatDate", DateUtil.getDay());
													newPd.put("creatTime", DateUtil.getTime());
													newPd.put("state", 0);
													newPd.put("skitId", quesData.getString("questionId"));
													newPd.put("pushTime", DateUtil.getTime());
													newsService.save(newPd);
													// 将状态改为30分钟后,如果状态不是今天，结束提醒
													String fauTime = DateUtil.getFauTime(30, DateUtil.getTime())
															+ ":00";
													String fauDate = fauTime.substring(0, 10);
													String creaDate = quesData.get("createTime").toString().substring(0,
															10);

													int alertCount = Integer
															.parseInt(quesData.get("alertCount").toString());
													if (alertCount < 2) {
														if (fauDate.equals(creaDate)) {
															quesData.put("alertTime", fauTime);
															quesData.put("alertCount", alertCount + 1);
														} else {
															quesData.put("alertTime", fauTime);
															quesData.put("isAlert", 0);
														}
													} else {
														quesData.put("alertTime", fauTime);
														quesData.put("isAlert", 0);
													}

													questionService.editPointDoc(quesData);
												}
											}
										} catch (Exception e) {
											quesData.put("alertTime", DateUtil.getTime());
											quesData.put("isAlert", 0);
											questionService.editPointDoc(quesData);
										}

									}
								}

								Thread.sleep(timeInterval);
							} catch (Exception e) {
								try {
									Thread.sleep(timeInterval);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
							}

						}
					}
				};
				Thread thread = new Thread(runnable);
				thread.start();
			}
		}
	}

}
