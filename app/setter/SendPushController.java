package com.fh.controller.app.setter;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.fh.controller.base.BaseController;
import com.fh.controller.newapi.txCloud.TXCloundcontroller;
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
import com.fh.util.UuidUtil;
import com.fh.util.tools.IsNull;

@Component
public class SendPushController extends BaseController implements ApplicationListener<ContextRefreshedEvent> {
	
	private long timeInterval = 1000 * 10;// 10s
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
					public void run() {
						while (true) {
							try {

								// 检查有没有聊天提醒
								List<PageData> sendList = doctPatService.findBySend(null);
								if (sendList != null && sendList.size() > 0) {
									PageData newSendData = new PageData();
									for (int i = 0; i < sendList.size(); i++) {
										PageData sendData = sendList.get(i);
										newSendData.put("doctorId", sendData.get("doctorId"));
										newSendData.put("patientId", sendData.get("userId"));
										//通知医生
										PageData doctorData = doctorService.findById(newSendData);
										if (IsNull.paramsIsNull(doctorData) == false) {
											String notes = "【知心医生医生版】您好，有患者正在知心医生平台向您咨询疾病问题，请尽快登录与其联系。";// 消息体内容
											PageData createPd = new PageData();
											createPd.put("psan", "知心诊所");
											createPd.put("postToPhoneNo", doctorData.get("phone"));
											createPd.put("notes", notes);
											createPd.put("channel", 1);// 渠道 1.亿美
											createPd.put("createTime", new Date());

											createPd.put("endTime", DateUtil.getTime3(30));// 有效时间
											createPd.put("actId", 3);
											createPd.put("sign", 0);
											createPd.put("flag", 0);// 状态 0.待发送 1.已发送2已校验
											createPd.put("SBYYPOSTPHONE_ID", UuidUtil.get32UUID());
											sbyypostphoneService.save(createPd);// 保存到数据库
											System.out.println("===============已发送短信================");
										}
										//通知患者
										String mid = newSendData.getString("doctorId");// 消息发送方账号
										String uid = newSendData.getString("patientId");// 消息接收方账号
										PageData siData = cloudSignService.findById(null);//获取腾讯云的sign
										String msg = "【系统消息】您好，医生正在接诊，请稍等，我们将尽快告知医生与您联系。";// 消息体内容
										TXCloundcontroller.OpenimSendMsg(uid, mid, msg, siData);
										System.out.println("===============已发送消息================");
										// 将状态改为已发送，结束提醒
										newSendData.put("docPatId", sendData.get("docPatId"));
										newSendData.put("isSend", "1");
										doctPatService.updateDoctPat(newSendData);
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
