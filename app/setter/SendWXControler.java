package com.fh.controller.app.setter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.fh.controller.base.BaseController;
import com.fh.service.app.DoctorService;
import com.fh.service.app.NewPushService;
import com.fh.service.app.PatientService;
import com.fh.service.app.WxTokenService;
import com.fh.service.statistics.DoctPatService;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.weixin.openId;

/**
 * Title:专家回复5分钟 患者没有回复发送模板消息
 * 
 * @author 霍学杰
 * @date 2018年10月02日
 * @version 1.0
 */
@Component("SendWXControler")
public class SendWXControler extends BaseController implements ApplicationListener<ContextRefreshedEvent> {

	private PageData pd = new PageData();
	int i = 0;
	@Resource
	private NewPushService newPushService;

	@Resource
	private WxTokenService wxTokenService;

	@Resource
	private DoctPatService doctPatService;

	@Resource
	private PatientService patientService;

	@Resource
	private DoctorService doctorService;

	public void onApplicationEvent(ContextRefreshedEvent event) {
		i = i + 1;
		if (i == 1) {
			Runnable runnable = new Runnable() {
				public void run() {
					while (true) {
						try {
							//System.out.println("专家回复5分钟 患者没有回复发送模板消息");
							long starTime = System.currentTimeMillis();// 获取的毫秒时间戳

							String start = stampToDate((starTime - 305000) + "");// 开始时间
							String end = stampToDate((starTime - 295000) + "");// 结束时间

							/*
							 * String start = stampToDate((starTime-65000)+"");//开始时间 String end =
							 * stampToDate((starTime-55000)+"");//结束时间
							 */
							pd.put("startime", start);
							pd.put("endtime", end);
							pd.put("dayDate", DateUtil.getDay());

							// 查询需要推送的数据
							List<PageData> findSendWX = doctPatService.findSendWX(pd);
							if (findSendWX != null && findSendWX.size() > 0) {
								for (int i = 0; i < findSendWX.size(); i++) {
									PageData pushData = findSendWX.get(i);
									pd.put("patientId", pushData.get("userId"));
									// 查询患者信息
									PageData findById = patientService.findById(pd);
									// 查询医生信息
									PageData doctor = doctorService.findById(pushData);

									if (findById != null && doctor != null) {
										// 获取过去六天的日期
										pushData.put("wxStartTime",
												DateUtil.getPastDate2(5, DateUtil.getTime()) + " 00:00:00");
										List<PageData> formList = newPushService.findByuserId(pushData);
										if (formList == null || formList.size() < 0) {
											continue;
										}else {
											PageData formData = formList.get(0);
											String jsonMsg = openId.expertMessage(formData.get("wxopenId").toString(),
													"pages/chatroom/chatroom?id=" + doctor.get("doctorId").toString(),
													formData.get("formId").toString(), "red", "",
													doctor.get("trueName").toString(), "知心医生专家回复",
													pushData.get("realTime").toString(),
													doctor.get("shanDisease").toString());
											Boolean isPush = openId.sendTemplateMessage(jsonMsg, wxTokenService);
											if (isPush) {// formId 没过期，能用
												updateState(pushData, doctPatService, formData, newPushService);
											} else {
												formData.put("state", 2);
												newPushService.editNewpush(formData);
											}
										}
									}
								}
								Thread.sleep(1000 * 5);
							} else {
								Thread.sleep(1000 * 5);
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
			Thread thread = new Thread(runnable);
			thread.start();
		}

	}

	public static void updateState(PageData pushData, DoctPatService doctPatService, PageData formData,
			NewPushService newPushService) {
		PageData dd = new PageData();
		dd.put("docPatId", pushData.get("docPatId"));
		dd.put("isPush", "1");
		try {
			doctPatService.updateDoctPat(dd);
			formData.put("state", 1);
			newPushService.editNewpush(formData);
			System.out.println("专家回复 5分钟   推送成功");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取当前时间前几分钟
	 * 
	 * @param minute
	 * @return time
	 */
	public static String getCurrentTime(int minute) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar beforeTime = Calendar.getInstance();
		beforeTime.add(Calendar.MINUTE, -minute);// minute分钟之前的时间
		Date beforeD = beforeTime.getTime();
		String time = sdf.format(beforeD);
		return time;
	}

	/**
	 * 将时间戳转换为时间
	 */
	public static String stampToDate(String s) {
		String res;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long lt = new Long(s);
		Date date = new Date(lt);
		res = simpleDateFormat.format(date);
		return res;
	}

}
