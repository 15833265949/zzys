package com.fh.controller.app.weixin;

import java.util.List;

import javax.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.fh.controller.base.BaseController;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.NewPushService;
import com.fh.service.app.NewsService;
import com.fh.service.app.PatientService;
import com.fh.util.PageData;

@Component
public class WXPushController extends BaseController implements ApplicationListener<ContextRefreshedEvent> {
	private long timeInterval = 1000 * 5000000;// 50s
	int i = 0;
	@Resource
	private NewsService newsService;
	@Resource
	private DayCountService dayCountService;
	@Resource
	private MonthCountService monthCountService;
	@Resource
	private DoctorService doctorService;
	@Resource
	private NewPushService newPushService;
	@Resource
	private PatientService patientService;

	public void onApplicationEvent(ContextRefreshedEvent event) {
		i = i + 1;
		if (event.getApplicationContext().getParent() == null) {// root application context 没有parent，他就是老大.
			if (i == 1) {
				Runnable runnable = new Runnable() {
					public void run() {
						while (true) {
							try {
								PageData pd = new PageData();
								pd.put("phone", "17778292417");
								PageData findByPhone = patientService.findByPhone(pd);
								if (findByPhone != null) {
									pd.put("userId", findByPhone.get("patientId"));
									List<PageData> findByuserId = newPushService.findByuserId(pd);
									if (findByuserId != null && findByuserId.size() > 0) {
//	                    	 		for (int i = 0; i < findByuserId.size(); i++) {
//	                    	 			PageData dd=new PageData();
//	                    	 			dd=findByuserId.get(i);
//	                    	 			//得到推送消息
//		                    	 		String jsonMsg= openId.makeRouteMessage(dd.get("wxopenId").toString(),
//		                    	 				"-96Uzp0CKdgdNZMvVl3uq468PhxHTOKpA88G38Zmxfo", "pages/register/register?clinicId=", 
//		                    	 				dd.get("formId").toString(), "red", "");
//		                    	 		     //推送
//		                    	 		   openId.sendTemplateMessage(jsonMsg);
//		                    	 		   dd.put("state", 1);
//		                    	 		  newPushService.editNewpush(dd);
//									}

									}
								}
								Thread.sleep(timeInterval);
							} catch (Exception e) {
								e.printStackTrace();
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
