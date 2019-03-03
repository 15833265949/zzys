package com.fh.controller.app.setter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.fh.controller.base.BaseController;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.PatientService;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.hp.hpl.sparta.ParseException;

/**
 * Title:实时查询    超过两天没有评价的系统自动做出评价
 * 
 * @author 霍学杰
 * @date 2017年7月12日
 * @version 1.0
 */
@Component("PJAutoControler")
public class PJAutoControler extends BaseController implements
		ApplicationListener<ContextRefreshedEvent> {

	private PageData pd = new PageData();
	int i = 0;
	@Resource
	private DoctorService doctorService;
	@Resource
	private PatientService patientService;
	@Resource
	private Clinic_checkinService clinic_checkinService;

	public void onApplicationEvent(ContextRefreshedEvent event) {
		i = i + 1;
		if (i == 100) {
			Runnable runnable = new Runnable() {
				public void run() {
					while (true) {
						try {
							System.out.println("自动扫描调度类");
							String startime = DateUtil.getDay()+" 23:00:00";
							String endtime = DateUtil.getDay()+" 23:59:00";
							int flag=DateUtil.compare_date(startime, endtime);
							if(flag==1){
								System.out.println("开始扫描========");
								List<PageData> list = clinic_checkinService.findDDlist(pd);
								if(list!=null&&list.size()>0){
									for (int i = 0; i < list.size(); i++) {
										PageData pat=list.get(i);
										if(IsNull.paramsIsNull(pat.get("state"))==false){
											pd.put("checkinId", pat.get("checkinId"));
											pd.put("state", 3);
											clinic_checkinService.updatestate(pd);
											System.out.println("处理完成");	
										}else{
											System.out.println("状态为空");
										}										
									}								
								}else{
									System.out.println("当前没有需要处理的");
									Thread.sleep(1000*60*5);
								}	
							}else{
								System.out.println("没有到指定的扫描时间");
								Thread.sleep(1000*60*5);
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

	public static void main(String[] args) throws ParseException {
		String day = DateUtil.getDay();
		System.out.println(new PJAutoControler()
				.getHMinute("2017-04-22 18:23:00"));

		int endMinute = new PJAutoControler().getHMinute(day + " " + "13:55"
				+ ":00");
		System.out.println("end:" + endMinute);

	}
}
