package com.fh.controller.app.question;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.controller.base.BaseController;
import com.fh.service.app.ConversationLastService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.NewsService;
import com.fh.service.app.PatiSearDiseService;
import com.fh.service.app.PatiSearOfficeService;
import com.fh.service.app.PatientService;
import com.fh.service.app.QuestionService;
import com.fh.util.AppUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PagingUtil;

/**
 * 
 * @author 董雪蕊
 * @date 2017年12月26日
 */
@Controller
@RequestMapping(value = "/api/question/patient")
public class AppPatientQuestionController extends BaseController {

	@Resource
	QuestionService questionService;
	@Resource
	ConversationLastService conversationLastService;
	@Resource
	DoctorService doctorService;
	@Resource
	NewsService newsService;
	@Resource
	PatiSearOfficeService patiSearOfficeService;
	@Resource
	PatiSearDiseService patiSearDiseService;
	@Resource
	PatientService patientService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 患者查询医生评价列表
	 * @date 2018年2月13日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/findDocPingList")
	@ResponseBody
	public Object findDocPingList() {
		logBefore(logger, "患者查询医生评价列表");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("pageSize"))
					|| IsNull.paramsIsNull(pd.get("pageIndex"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd = PagingUtil.addPaging(pd);
				// 查询医生的评价列表
				List<PageData> list = questionService.listByDocId(pd);
				if (list != null && list.size() > 0) {
					for (int i = 0; i < list.size(); i++) {
						PageData pageData = list.get(i);
						// 存储小程序时间戳
						pageData.put("XCXpingTime", pageData.get("pingTime"));
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
						pageData.put("pingTime", sdf.format(pageData.get("pingTime")));
					}
					map.put("list", list);
					map.put("size", list.size());
				} else {
					map.put("list", new ArrayList<>());
					map.put("size", 0);
				}
				result = "0000";
				message = "查询成功";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("message", message);
			map.put("result", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 得到患者的随机码 如果患者没有，就插入
	 * 
	 * @throws Exception
	 */
	public static String getPatiNum(String patientId, PatientService patientService2) throws Exception {
		String retString = "";
		PageData pd = new PageData();
		pd.put("patientId", patientId);
		PageData patiData = patientService2.findById(pd);
		if (IsNull.paramsIsNull(patiData) == false) {
			if (IsNull.paramsIsNull(patiData.get("randomNum")) == false
					&& Integer.parseInt(patiData.get("randomNum").toString()) != 0) {
				retString = patiData.get("randomNum").toString();
			} else {
				int randomNum = (int) ((Math.random() * 9) * 1000);
				pd.put("randomNum", randomNum);
				patientService2.editPatientInfo(pd);
				retString = randomNum + "";
			}
		}
		return retString;
	}

}
