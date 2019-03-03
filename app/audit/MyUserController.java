package com.fh.controller.app.audit;

import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fh.controller.base.BaseController;
import com.fh.entity.Page;
import com.fh.entity.system.User;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorBigSortService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.MyUserService;
import com.fh.service.app.NewsService;
import com.fh.service.back.DoctorBMService;
import com.fh.util.Const;
import com.fh.util.DateUtil;
import com.fh.util.ObjectExcelView;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.jPush.examples.jPush;

/**
 * 描述：审核管理诊所
 * 
 * @author 霍学杰 2017.9.2
 */

@Controller
@RequestMapping(value = "/myUser")
public class MyUserController extends BaseController {

	@Resource(name = "MyUserService")
	private MyUserService MyUserService;

	@Resource
	private DoctorService doctorService;

	@Resource
	private DoctorBMService doctorBMService;

	@Resource
	private ClinicUserService clinicUserService;

	@Resource
	private ClinicService clinicService;

	@Resource
	private MonthCountService monthCountService;

	@Resource
	private DayCountService dayCountService;

	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private NewsService newsService;

	@Resource
	private DoctorBigSortService doctorBigSortService;

	/**
	 * 1.描述：展示审核诊所列表 霍学杰
	 * 
	 * @param page
	 * @return
	 * @throws Exception 2017.9.2
	 */
	@RequestMapping(value = "/validate")
	public ModelAndView sum(Page page) throws Exception {
		logBefore(logger, "诊所认证列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		if (IsNull.paramsIsNull(pd.get("state"))) {
			pd.put("state", 5);
		}
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = MyUserService.clinicList(page); // 列出Pictures列表
		mv.setViewName("system/enroll/doctor");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}

	// ==================================================================================
	/**
	 * 1.1描述：展示 申请中诊所列表
	 * 
	 * @author 霍学杰
	 * @param page
	 * @return
	 * @throws Exception
	 * @date 2017.10.24
	 */
	@RequestMapping(value = "/syslistPage")
	public ModelAndView sum2(Page page) throws Exception {
		logBefore(logger, "诊所认证列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		Subject currentUser = SecurityUtils.getSubject();
		Session session = currentUser.getSession();

		User user = (User) session.getAttribute(Const.SESSION_USER);
		pd.put("role", user.getROLE_ID());
		pd.put("state", 0);
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = MyUserService.syslistPage(page); // 列出Pictures列表
		mv.setViewName("app/review/SHclinic");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}

	/**
	 * 1.2描述：展示 审核通过诊所列表
	 * 
	 * @author 霍学杰
	 * @param page
	 * @return
	 * @throws Exception
	 * @date 2017.10.24
	 */
	@RequestMapping(value = "/syslistPage2")
	public ModelAndView sum3(Page page) throws Exception {
		logBefore(logger, "诊所认证列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		pd.put("state", 1);
		/*
		 * pd.put("starTime",DateUtil.getDay()+" 00:10:00");
		 * pd.put("endTime",DateUtil.getDay()+" 23:55:59");
		 */
		if (IsNull.paramsIsNull(pd.get("starTime1")) == false) {
			pd.put("starTime", pd.get("starTime1") + " 00:10:00");
		} else {
			pd.put("starTime1", DateUtil.getDay());
		}
		if (IsNull.paramsIsNull(pd.get("endTime1")) == false) {
			pd.put("endTime", pd.get("endTime1") + " 23:55:59");
		} else {
			pd.put("endTime1", DateUtil.getDay());
		}
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = MyUserService.syslistPage(page); // 列出Pictures列表
		mv.setViewName("app/review/CKclinic");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}

	/**
	 * 1.2描述：展示 审核 未通过诊所列表
	 * 
	 * @author 霍学杰
	 * @param page
	 * @return
	 * @throws Exception
	 * @date 2017.11.27
	 */
	@RequestMapping(value = "/syslistPage3")
	public ModelAndView sum4(Page page) throws Exception {
		logBefore(logger, "审核未通过列表");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		pd.put("state", 4);
		if (IsNull.paramsIsNull(pd.get("starTime1")) == false) {
			pd.put("starTime", pd.get("starTime1") + " 00:10:00");
		} else {
			pd.put("starTime", DateUtil.getDay() + " 00:10:00");
		}
		if (IsNull.paramsIsNull(pd.get("endTime1")) == false) {
			pd.put("endTime", pd.get("endTime1") + " 23:55:59");
		}
		page.setPd(pd);
		// 查询列表
		List<PageData> varList = MyUserService.syslistPage(page); // 列出Pictures列表
		mv.setViewName("app/review/WCKclinic");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}

	/**
	 * 导出信息到EXCEL
	 * 
	 * @author 霍学杰
	 * @return
	 */
	@RequestMapping(value = "/excel")
	public ModelAndView exportExcel() {
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		try {
			Map<String, Object> dataMap = new HashMap<String, Object>();
			List<String> titles = new ArrayList<String>();

			titles.add("诊所名称"); // 1
			titles.add("省"); // 2
			titles.add("市"); // 3
			titles.add("区/县"); // 4
			titles.add("地址"); // 5
			titles.add("电话"); // 6
			titles.add("审核时间"); // 7
			titles.add("审核状态"); // 8
			titles.add("主治方向"); // 8

			dataMap.put("titles", titles);
			pd.put("state", 1);
			List<PageData> userList = MyUserService.getClinicList(pd);
			List<PageData> varList = new ArrayList<PageData>();
			for (int i = 0; i < userList.size(); i++) {
				PageData vpd = new PageData();
				vpd.put("var1", userList.get(i).getString("clinicName")); // 1
				try {
					vpd.put("var2", userList.get(i).getString("clinicProvince")); // 2
				} catch (Exception e) {
					vpd.put("var2", "暂无"); // 2
				}
				vpd.put("var3", userList.get(i).getString("cityname")); // 3
				vpd.put("var4", userList.get(i).getString("adname")); // 4
				vpd.put("var5", userList.get(i).getString("clinicAddress")); // 5
				vpd.put("var6", userList.get(i).getString("clinicphone")); // 6
				vpd.put("var7", userList.get(i).getString("auditTime")); // 7
				int state = Integer.parseInt(userList.get(i).get("state").toString());
				// vpd.put("var8", userList.get(i).getString("state")); //8
				if (state == 1) {
					vpd.put("var8", "审核通过");
				} else {
					vpd.put("var8", "审核失败");
				}
				try {
					vpd.put("var9", userList.get(i).getString("majorDirection"));
				} catch (Exception e) {
					vpd.put("var9", "暂无");// 8
				}
				varList.add(vpd);
			}
			dataMap.put("varList", varList);
			ObjectExcelView erv = new ObjectExcelView(); // 执行excel操作
			mv = new ModelAndView(erv, dataMap);

		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
		return mv;
	}

	// =================================================================================

	/**
	 * 2.描述：审核通过 霍学杰
	 * 
	 * @param doctorId clinicId 2017.9.2
	 */
	@RequestMapping(value = "/adopt")
	public void adopt(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			PageData clinic = clinicService.findzsClinic(pd);// 查询诊所
			int state = Integer.parseInt(clinic.get("state").toString());
			if (state == 0) {
				pd.put("auditResult", "审核通过");
				pd.put("auditTime", DateUtil.getTime());
				MyUserService.adopt(pd);// 通过 修改状态 state 诊所
				pd.put("clinicName", clinic.get("clinicName"));
				MyUserService.adopt2(pd);// 修改医生 状态

				PageData data = doctorService.findDoctorInfo(pd);
				if (data != null) {
					PageData doct = clinicUserService.findbyDocid(pd);
					if (doct != null) {
						int flag = Integer.parseInt(doct.get("state").toString());
						if (flag == 1 || flag == 0) {
							PageData gg = new PageData();
							gg.put("state", 2);
							gg.put("doctorId", doct.get("doctorId"));
							clinicUserService.updateDocx(gg);// 添加诊所成员
						}

					} else {
						data.put("state", 2);
						data.put("addTime", DateUtil.getTime());
						clinicUserService.addClinic(data);// 添加诊所成员
					}
				}
				// ===========================
				// 添加月历史统计记录 诊所 v2
				PageData day1 = new PageData();
				day1.put("cdayId", this.get32UUID());
				day1.put("year", DateUtil.getYear());
				day1.put("month", DateUtil.getMonth());
				day1.put("day", DateUtil.getDay1());
				day1.put("clinicId", pd.get("clinicId"));
				day1.put("doctorId", pd.get("doctorId"));

				// 1.查看有没有历史记录 有跳过 没有添加
				PageData findClinicsy2 = monthCountService.findClinicshujutj(day1);// 诊所
				PageData findminthsj = monthCountService.findDoctorshujutj(day1);// 个人
				PageData findDaysj = dayCountService.findDaytjc(day1);// 诊所天数据
				PageData findDaysj2 = dayCountService.findshujutj(day1);// 个人天数据

				if (findClinicsy2 == null) {
					day1.put("cmonthId", this.get32UUID());
					monthCountService.save2(day1);
				}
				if (findminthsj == null) {
					day1.put("monthId", this.get32UUID());
					monthCountService.save1(day1);
				}
				if (findDaysj == null) {
					day1.put("cdayId", this.get32UUID());
					dayCountService.save2(day1);
				}
				if (findDaysj2 == null) {
					day1.put("dayId", this.get32UUID());
					dayCountService.save(day1);
				}

				out.write("success");
				out.close();

				// 修改权限 加推送
				pd.put("clinicRzFlag", 2);
				pd.put("authPlat", 0);
				doctorService.editDocAuthPlat(pd);

				List<PageData> findFriend = doctorService.findFriend(pd);
				String messageConet = "诊所已认证审核通过，等待管理员进行审核分配角色";

				PageData dd2 = new PageData();
				if (findFriend != null && findFriend.size() > 0) {
					for (int j = 0; j < findFriend.size(); j++) {
						dd2 = findFriend.get(j);//
						Collection<String> alias = new ArrayList<>();
						Collection<String> registrationId = new ArrayList<>();
						registrationId.add(dd2.get("phoneSole").toString());
						String toUserId = dd2.get("doctorId").toString();
						alias.add(toUserId);
						String title = "审核提醒";

						String phoneSole = "";
						String huaWeiToken = "";
						String miRegId = "";
						String mzPushId = "";
						phoneSole = dd2.getString("phoneSole");
						huaWeiToken = dd2.getString("huaWeiToken");
						miRegId = dd2.getString("miRegId");
						mzPushId = dd2.getString("mzPushId");

						if (toUserId.equals(pd.get("doctorId").toString())) {
							jPush.sendAll(alias, registrationId, title, "1",
									"您认证的" + clinic.get("clinicName").toString() + "已经通过审核", "", "0", toUserId, "1",
									DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
									mzPushId, phoneSole);
							// 添加消息
							PageData newsPd = new PageData();
							newsPd.put("sysdrugName", "");
							newsPd.put("title", title);
							newsPd.put("type", "1");
							newsPd.put("flag", "1");
							newsPd.put("messageContent", "您认证的" + clinic.get("clinicName").toString() + "已经通过审核");
							newsPd.put("fromUserId", "");
							newsPd.put("fromRoleFlag", "0");
							newsPd.put("toUserId", pd.get("doctorId").toString());
							newsPd.put("toRoleFlag", "1");
							newsPd.put("creatDate", DateUtil.getDay());
							newsPd.put("creatTime", DateUtil.getTime());
							newsService.save(newsPd);

							continue;
						} else {
							jPush.sendAll(alias, registrationId, title, "1", messageConet, "", "0", toUserId, "1",
									DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
									mzPushId, phoneSole);
							// 添加消息
							PageData newsPd = new PageData();
							newsPd.put("sysdrugName", "");
							newsPd.put("title", title);
							newsPd.put("type", "1");
							newsPd.put("flag", "1");
							newsPd.put("messageContent", messageConet);
							newsPd.put("fromUserId", "");
							newsPd.put("fromRoleFlag", "0");
							newsPd.put("toUserId", toUserId);
							newsPd.put("toRoleFlag", "1");
							newsPd.put("creatDate", DateUtil.getDay());
							newsPd.put("creatTime", DateUtil.getTime());
							newsService.save(newsPd);

							// 释放资源
							PageData findexitcli = clinic_checkinService.findexitcli(pd);
							if (findexitcli != null) {
								pd.put("checkinId", findexitcli.get("checkinId"));
								clinic_checkinService.exitUpdate(pd);
							}

						}

					}
				}
			}

		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

	/**
	 * 3.描述：审核不通过 霍学杰
	 * 
	 * @param doctorId clinicId 2017.9.2
	 */
	@RequestMapping(value = "/noAdopt")
	public void noAdopt(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			try {
				if (IsNull.paramsIsNull(pd.get("auditResult")) == false) {
					System.out.println("审核原因");
				} else {
					pd.put("auditResult", "审核失败，诊所证书不合格");
				}
			} catch (Exception e) {
				pd.put("auditResult", "审核失败，诊所证书不合格");
			}
			pd.put("auditResult", URLDecoder.decode(pd.getString("auditResult"), "UTF-8"));
			pd.put("auditTime", DateUtil.getTime());
			MyUserService.noAdopt(pd);// bu通过 修改状态 state
			pd.put("clinicRzFlag", 3);
			doctorService.editDocAuthPlat(pd);// 修改医生 状态
			// clinicService.delByclinicId(pd);//删除诊所记录
			PageData findById = doctorService.findById(pd);
			Collection<String> alias = new ArrayList<>();
			Collection<String> registrationId = new ArrayList<>();
			registrationId.add(findById.get("phoneSole").toString());
			String toUserId = findById.get("doctorId").toString();
			alias.add(toUserId);
			String title = "审核提醒";

			String phoneSole = "";
			String huaWeiToken = "";
			String miRegId = "";
			String mzPushId = "";
			phoneSole = findById.getString("phoneSole");
			huaWeiToken = findById.getString("huaWeiToken");
			miRegId = findById.getString("miRegId");
			mzPushId = findById.getString("mzPushId");

			jPush.sendAll(alias, registrationId, title, "2",
					"您认证的" + findById.get("clinicName").toString() + "未通过审核，原因：" + pd.get("auditResult"), "", "0",
					toUserId, "1", DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
					mzPushId, phoneSole);
			// 添加消息
			PageData newsPd = new PageData();
			newsPd.put("sysdrugName", "");
			newsPd.put("title", title);
			newsPd.put("type", "2");
			newsPd.put("flag", "1");
			newsPd.put("messageContent", "您认证的" + findById.get("clinicName").toString() + "未通过审核，请查看详细原因");
			newsPd.put("fromUserId", "");
			newsPd.put("fromRoleFlag", "0");
			newsPd.put("toUserId", toUserId);
			newsPd.put("toRoleFlag", "1");
			newsPd.put("creatDate", DateUtil.getDay());
			newsPd.put("creatTime", DateUtil.getTime());
			newsService.save(newsPd);
			out.write("success");
			out.close();

		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
	}

	/**
	 * 4.描述： 实名审核列表 霍学杰(20180603 医生资质审核)
	 * 
	 * @param page
	 * @return
	 * @throws Exception 2017.9.3
	 */
	@RequestMapping(value = "/autolist")
	public ModelAndView autolist(Page page) throws Exception {
		logBefore(logger, "医生资质审核");
		ModelAndView mv = this.getModelAndView();
		PageData pd = new PageData();
		pd = this.getPageData();
		if (IsNull.paramsIsNull(pd.get("doctorLIflag"))) {
			pd.put("doctorLIflag", 1);
		}
		page.setPd(pd);
		List<PageData> varList = doctorBMService.datalistPage(page); // 列出Pictures列表
		if (varList != null && varList.size() > 0) {
			String url = null;
			for (int i = 0; i < varList.size(); i++) {
				PageData attpUrl = varList.get(i);
				attpUrl.put("time", attpUrl.get("createTime").toString());
				if (IsNull.paramsIsNull(attpUrl.get("doctorLicense"))) {
				} else {
					url = attpUrl.get("doctorLicense").toString();
					if (url.contains(",")) {
						String[] img = url.split(",");
						attpUrl.put("img0", img[0]);
						attpUrl.put("img1", img[1]);
						varList.set(i, attpUrl);
					} else {
						attpUrl.put("img0", url);
						varList.set(i, attpUrl);
					}
				}
				if (IsNull.paramsIsNull(attpUrl.get("doctorLicense2"))) {
				} else {
					url = attpUrl.get("doctorLicense2").toString();
					if (url.contains(",")) {
						String[] img = url.split(",");
						attpUrl.put("img2", img[0]);
						attpUrl.put("img3", img[1]);
						varList.set(i, attpUrl);
					} else {
						attpUrl.put("img2", url);
						varList.set(i, attpUrl);
					}
				}
			}
		}

//		mv.setViewName("system/enroll/shiming");
		mv.setViewName("app/back/shiming");
		mv.addObject("varList", varList);
		mv.addObject("page", page);
		mv.addObject("pd", pd);
		return mv;
	}

	/**
	 * 5.描述：审核 实名认证(20180603 修改为医生资质审核)
	 * 
	 * @author 霍学杰
	 * @param doctorId flag 2通过 3不通过
	 * @date 2017.9.3
	 */
	@RequestMapping(value = "/adopt3")
	public void adopt3(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			PageData findById = doctorService.findById(pd);
			if ("3".equals(pd.get("flag").toString())) {
				try {
					if (IsNull.paramsIsNull(pd.get("auditResult")) == false) {
						System.out.println("审核原因");
						pd.put("Reason", URLDecoder.decode(pd.getString("auditResult"), "UTF-8"));
					}
				} catch (Exception e) {
					pd.put("Reason", "审核失败，证书不合格");
				}
				pushJG(findById, newsService, pd, 81);
			} else {
				pd.put("Reason", "审核成功");
				pushJG(findById, newsService, pd, 80);
			}
			pd.put("doctorLIflag", pd.get("flag"));

			PageData findByID2 = doctorBMService.findByID(pd);
			if (findByID2 != null) {
				doctorBMService.editDocBM(pd);
				findByID2.put("doctorLIflag", pd.get("flag"));
				findByID2.put("Reason", pd.get("Reason"));
				doctorService.editPatflag(findByID2);
				if (IsNull.paramsIsNull(findByID2.get("bigOffice")) == false) {
					officeSave2(findByID2.get("bigOffice").toString(), pd.get("doctorId").toString());
				}
				out.write("success");
				out.close();

			} else {
				out.write("erro");
				out.close();
			}
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

	/**
	 * 解析医生科室
	 * 
	 * @param topOfficeId
	 * @param doctorId
	 * @throws Exception
	 */
	public void officeSave2(String topOfficeId, String doctorId) throws Exception {
		PageData pd = new PageData();
		pd.put("doctorId", doctorId);
		String[] split = topOfficeId.split(",");
		if (split.length > 0) {
			doctorBigSortService.del(pd);// 删除新版的医生科室表
			for (int i = 0; i < split.length; i++) {
				pd.put("topOfficeId", split[i]);
				pd.put("isQuan", 0);
				doctorBigSortService.save(pd);
			}
		}
	}

	/**
	 * 审核推送
	 * 
	 * @param info
	 * @param newsService
	 * @param pd
	 * @throws Exception pushState 1诊所认证成功 2 诊所认证失败 80 医生资质 通过 81 医生资质 通过 失败
	 */

	public static void pushJG(PageData info, NewsService newsService, PageData pd, int pushState) throws Exception {
		Collection<String> alias = new ArrayList<String>();
		Collection<String> registrationId = new ArrayList<String>();
		registrationId.add(info.get("phoneSole").toString());
		String toUserId = info.get("doctorId").toString();
		alias.add(toUserId);
		String title = "知心平台审核提醒";
		String messageContent = "";

		if (pushState == 2) {
			messageContent = "您认证的" + info.get("clinicName").toString() + "未通过知心平台审核，原因：" + pd.get("auditResult");
		}
		if (pushState == 1) {
			messageContent = "您认证的" + info.get("clinicName").toString() + "通过知心平台审核";
		}

		if (pushState == 80) {
			messageContent = "您认证的医生资质通过知心平台审核";
		}
		if (pushState == 81) {
			messageContent = "您认证的医生资质未通过知心平台审核";
		}

		String phoneSole = "";
		String huaWeiToken = "";
		String miRegId = "";
		String mzPushId = "";
		phoneSole = info.getString("phoneSole");
		huaWeiToken = info.getString("huaWeiToken");
		miRegId = info.getString("miRegId");
		mzPushId = info.getString("mzPushId");

		jPush.sendAll(alias, registrationId, title, pushState + "", messageContent, "", "0", toUserId, "1",
				DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId, mzPushId, phoneSole);
		// 添加消息
		PageData newsPd = new PageData();
		newsPd.put("sysdrugName", "");
		newsPd.put("title", title);
		newsPd.put("type", pushState);
		newsPd.put("flag", "1");
		newsPd.put("messageContent", messageContent);
		newsPd.put("fromUserId", "");
		newsPd.put("fromRoleFlag", "0");
		newsPd.put("toUserId", toUserId);
		newsPd.put("toRoleFlag", "1");
		newsPd.put("creatDate", DateUtil.getDay());
		newsPd.put("creatTime", DateUtil.getTime());
		newsService.save(newsPd);
	}

}
