package com.fh.controller.app.setter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.fh.controller.base.BaseController;
import com.fh.service.app.NewPushService;
import com.fh.service.app.WxTokenService;
import com.fh.service.back.PushService;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.weixin.openId;

/**
 * Title:医生回复 推送模板消息
 * 
 * @author 霍学杰
 * @date 2018年05月117日
 * @version 1.0
 */
@Component("PushWXControler")
public class PushWXControler extends BaseController implements
		ApplicationListener<ContextRefreshedEvent> {

	private PageData pd = new PageData();
	int i = 0;
	@Resource
	private  NewPushService newPushService;
	
	@Resource
	private  PushService pushService;
	
	@Resource
	private WxTokenService wxTokenService;

	public void onApplicationEvent(ContextRefreshedEvent event) {
		i = i + 1;
		if (i == 10) {
			Runnable runnable = new Runnable() {
				public void run() {
					while (true) {
						try {
							Random random = new Random();
							int sl1=random.nextInt(25000);
							Thread.sleep(sl1*2);
							System.out.println("自动扫描调度类"+sl1*2);
							String startime =DateUtil.getDay()+" "+ DateUtil.getTime2()+":00";
							String endtime = DateUtil.getDay()+" "+ DateUtil.getTime2()+":59";
							pd.put("startime", startime);
							pd.put("endtime", endtime);
							List<PageData> tSlist = pushService.getTSlist(pd);
							if(tSlist!=null&&tSlist.size()>0){
								for (int i = 0; i < tSlist.size(); i++) {
									PageData pushData = tSlist.get(i);
									pd.put("userId", pushData.get("patientId"));
									List<PageData> formList = newPushService.findByuserId(pd);
									if(formList!=null&&formList.size()>0){
										
										for (int j = 0; j < formList.size(); j++) {
											PageData formData = formList.get(j);
											
											String jsonMsg = openId.makeRouteMessage4(formData.get("wxopenId").toString(),
													"pages/chatroom/chatroom?id=" + pushData.get("doctorId").toString(), formData.get("formId").toString(), "red", "",
													pushData.get("docAnswerCon").toString(), pushData.get("trueName").toString(), pushData.get("officeName").toString(), 
													pushData.get("content").toString(), DateUtil.getTime());
											
											
											Boolean isPush = openId.sendTemplateMessage(jsonMsg, wxTokenService);
											if (isPush) {// formId 没过期，能用
												formData.put("state", 1);
												newPushService.editNewpush(formData);
												pushService.editPush(pushData);
												break;
											} else {
												formData.put("state", 2);
												newPushService.editNewpush(formData);
											}																						
											
										}
										
									}									
									
								}								
								
							}else{
								Thread.sleep(random.nextInt(30000));
							}
									
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
			Thread thread = new Thread(runnable);
			thread.start();
			System.out.println(thread.getId());
		}

	}

	public int getHMinute(String nextTime) {
		int minute = 0;
		// 得到当前时间
		String nowTime = DateUtil.getTime();
		Date nextDate = null;
		Date nowDate = null;
		DateFormat df_parseDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			nowDate = (Date) df_parseDate.parse(nowTime);
		} catch (java.text.ParseException e) {

			e.printStackTrace();
		}
		try {
			nextDate = (Date) df_parseDate.parse(nextTime);
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}

		minute = (int) ((nextDate.getTime() - nowDate.getTime()));

		minute = minute / 60 / 1000;

		return minute;
	}

	public static String getDateBefore(Date d, int day) {
		Calendar now = Calendar.getInstance();
		now.setTime(d);
		now.set(Calendar.DATE, now.get(Calendar.DATE) - day);
		String day1 = now.getTime().toString();
		return day1;
	}

}
