package com.fh.controller.app.doctor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fh.config.CommonConfig;
import com.fh.config.CommonMessage;
import com.fh.controller.app.tencentCloud.AppTencentCloudController;
import com.fh.controller.app.upload.UploadController;
import com.fh.controller.base.BaseController;
import com.fh.controller.statistics.invitecode.InviteCodeControler;
import com.fh.service.app.ChargeService;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.CloudSignService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorBigSortService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.FriendsService;
import com.fh.service.app.LoginLogService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.NewsService;
import com.fh.service.app.PatientService;
import com.fh.service.app.QuestionService;
import com.fh.service.app.SchedulService;
import com.fh.service.statistics.ActiveHoursService;
import com.fh.service.statistics.InviteCodeService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.ditu.DituUtil;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PhoneCheck;
import com.fh.util.tools.caiCode.QRCodeUtil;
import com.fh.util.tools.jPush.examples.jPush;
import com.google.gson.JsonObject;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 描述：医生业务处理类
 * 
 * @author 霍学杰
 * @date 2017.8.25 版本：1.0
 */

@Controller
@RequestMapping(value = "/api/doctor")
public class DoctorControler extends BaseController {

	@Resource
	private DoctorService doctorService;
	@Resource
	private ActiveHoursService activeHoursService;
	@Resource(name = "loginLogService")
	private LoginLogService loginLogService;
	@Resource
	private CloudSignService cloudSignService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private PatientService patientService;
	@Resource
	private ChargeService chargeService;
	@Resource
	private ClinicUserService clinicUserService;
	@Resource
	private MonthCountService monthCountService;
	@Resource
	private DayCountService dayCountService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private NewsService newsService;
	@Resource
	private SchedulService schedulService;
	@Resource
	private DoctorBigSortService doctorBigSortService;
	@Resource
	private QuestionService questionService;
	@Resource
	private InviteCodeService inviteCodeService;
	@Resource
	private FriendsService friendsService;

	/**
	 * @Explain 医生二维码生成
	 * @author 霍学杰
	 * @param content
	 * @param fileName
	 * @param doctor
	 * @return
	 * @throws Exception
	 */

	public String doctorQR(String content, String fileName, PageData doctor) throws Exception {

		String doctorQR = "";

		String logoPath = "";
		String destPath = "";

		switch (CommonConfig.PIC_FLAG) {
		case 0:
			logoPath = "E:\\ApacheServe\\tomcat_pic\\webapps\\pic\\512.png";// logo地址
			// 头像处理
			int starNum = 0;
			int endNum = 0;
			if (IsNull.paramsIsNull(doctor.get("headUrl")) == false) {
				String ss = doctor.get("headUrl").toString();
				starNum = ss.length();
				ss = ss.replaceAll("http://jk.120ksb.com:8003/pic/picResource/zxys_sys/", "");
				endNum = ss.length();
				if (starNum > endNum) {
					String[] split = ss.split("/");
					if (split.length > 1) {
						logoPath = "E:\\ApacheServe\\tomcat_pic\\webapps\\pic\\picResource\\zxys_sys\\" + split[0]
								+ "\\" + split[1];
					}
				} else {
					ss = doctor.get("headUrl").toString();
					starNum = ss.length();
					ss = ss.replaceAll("https://ws.120ksb.com/picture/api/", "");
					endNum = ss.length();
					if (starNum > endNum) {
						String[] split = ss.split("/");
						if (split.length > 1) {
							logoPath = "E:\\apache-tomcat-7.0.72\\webapps\\picture\\api\\" + split[0] + "\\" + split[1];
						}
					} else {
						ss = doctor.get("headUrl").toString();
						starNum = ss.length();
						ss = ss.replaceAll("http://jk.120ksb.com:8003/touxiang/", "");
						endNum = ss.length();
						if (starNum > endNum) {
							if (ss.length() > 1) {
								logoPath = "E:\\ApacheServe\\tomcat_pic\\webapps\\touxiang\\" + ss;
							}
						}
					}

				}
			}
			destPath = "E:\\apache-tomcat-7.0.72\\webapps\\picture\\zxys\\";// 存放目录
			QRCodeUtil.encode(content, logoPath, destPath, fileName, true);
			doctorQR = "https://ws.120ksb.com/picture/zxys/" + fileName + ".jpg";
			break;
		case 2:
			destPath = "D:\\tomcat\\apache-tomcat-7.0.68\\webapps\\pic\\zxys\\";// 存放目录
			QRCodeUtil.encode(content, logoPath, destPath, fileName, true);

			doctorQR = "http://192.168.42.215:8086/pic/zxys/" + fileName + ".jpg";
			break;
		case 3:
			destPath = "E:\\server\\tomcat-8.5.8022\\webapps\\pic\\zxys\\";// 存放目录
			QRCodeUtil.encode(content, logoPath, destPath, fileName, true);
			doctorQR = "http://222.222.24.149:8022/pic/zxys/" + fileName + ".jpg";
			break;
		case 1:
			destPath = "E:\\server\\apache-tomcat-8.5.20\\webapps\\pic\\zxys\\";// 存放目录
			QRCodeUtil.encode(content, logoPath, destPath, fileName, true);

			doctorQR = "http://222.222.24.144:8083/pic/zxys/" + fileName + ".jpg";
			break;
		default:
			break;
		}

		return doctorQR;
	}

	/**
	 * 查询医生个人信息二维码 20180403
	 * 
	 * @author 霍学杰
	 * @date 2018年04月03日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/getDocQR")
	@ResponseBody
	public Object getDocQR() {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData doctor = doctorService.findDoctorInfo(pd);
				if (doctor != null) {

					// 4.30**********************
					PageData pingData = questionService.dbByDocId(pd);
					if (IsNull.paramsIsNull(pingData) == false) {
						doctor.putAll(pingData);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
						doctor.put("pingTime", sdf.format(doctor.get("pingTime")));
					} else {
						doctor.put("pingStar", -1);
						doctor.put("pingTime", -1);
						doctor.put("pingContent", "");
						doctor.put("conversationLastId", -1);
						doctor.put("patiTrueName", "");
					}
					// *************************
					JsonObject lan1 = new JsonObject();
					if (IsNull.paramsIsNull(doctor.get("trueName")) == false) {
						lan1.addProperty("trueName", doctor.get("trueName").toString());
					} else {
						lan1.addProperty("trueName", "知心医生" + doctor.get("randomNumDoc").toString());
					}
					if (IsNull.paramsIsNull(doctor.get("headUrl")) == false) {
						lan1.addProperty("headUrl", doctor.get("headUrl").toString());
					} else {
						lan1.addProperty("headUrl", CommonConfig.DOC_HEAD_URL);
					}

					if (IsNull.paramsIsNull(doctor.get("clinicId")) == false) {
						lan1.addProperty("clinicId", doctor.get("clinicId").toString());
					} else {
						lan1.addProperty("clinicId", "");
					}

					if (IsNull.paramsIsNull(doctor.get("totalPingStar")) == false) {
						lan1.addProperty("totalPingStar", doctor.get("totalPingStar").toString());
					} else {
						lan1.addProperty("totalPingStar", "");
					}

					if (IsNull.paramsIsNull(doctor.get("sex")) == false) {
						lan1.addProperty("sex", doctor.get("sex").toString());
					} else {
						lan1.addProperty("sex", "1");
					}

					if (IsNull.paramsIsNull(doctor.get("age")) == false) {
						lan1.addProperty("age", doctor.get("age").toString());
					} else {
						lan1.addProperty("age", "0");
					}

					String content = "https://ws.120ksb.com/zxys_sys/api/doctor?id=" + pd.get("doctorId").toString()
							+ "&json=" + lan1;
					String fileName = pd.get("doctorId").toString();
					String doctorQR = doctorQR(content, fileName, doctor);
					map.put("doctorQR", doctorQR);
					map.put("doctor", doctor);
					result = "0000";
					message = "成功";
				} else {
					result = "1002";
					message = "医生信息不存在";
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
	 * 获取登录用户的IP
	 * 
	 * @throws Exception
	 */
	public String getRemortIP(String phone) throws Exception {
		HttpServletRequest request = this.getRequest();
		String ip = "";
		if (request.getHeader("x-forwarded-for") == null) {
			ip = request.getRemoteAddr();
		} else {
			ip = request.getHeader("x-forwarded-for");
		}
		System.out.println(ip);
		return ip;
	}

	/**
	 * 2. 描述：退出登录
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月19日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/loginOut", method = RequestMethod.POST)
	@ResponseBody
	public Object loginOut() {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("loginId", pd.get("doctorId"));
				PageData db = loginLogService.findLoginLog(pd);
				if (db != null) {
					pd.put("logId", db.get("logId"));
					pd.put("logoutTime", DateUtil.getTime());
					loginLogService.editLogoutTime(pd);
					// 更改离线状态
					pd.put("onlineState", "0");
					// 清除推送id
					pd.put("huaWeiToken", "0");
					pd.put("miRegId", "0");
					pd.put("mzPushId", "0");
					pd.put("phoneSole", "0");
					doctorService.editDoctorInfo(pd);
					result = "0000";
					message = "退出成功";
				} else {// 用户登录信息不存在
					result = "1002";
					message = "退出失败";
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
	 * 3.描述： 0 忘记密码 1 医生注册
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月19日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/composite", method = RequestMethod.POST)
	@ResponseBody
	public Object composite() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("phone")) || IsNull.paramsIsNull(pd.get("password"))
					|| IsNull.paramsIsNull(pd.get("type"))) {
				result = "9993";
				message = "参数异常";
			} else {
				int type = Integer.parseInt(pd.get("type").toString());
				if (type == 1) {// 注册
					String passwd = new SimpleHash("SHA-1", pd.get("password").toString().trim()).toString(); // 密码加密
					pd.put("password", passwd);
					pd.put("createTime", DateUtil.getTime());
					pd.put("doctorId", this.get32UUID());
					doctorService.saveUser(pd);
					result = "0000";
					message = "注册成功";
					// 返回实体信息
					PageData doctor = doctorService.findDoctorInfo(pd);
					map.put("doctor", doctor);
				} else if (type == 0) {
					// 修改密码
					String Pass = pd.get("password").toString().trim();
					String passwd = new SimpleHash("SHA-1", Pass).toString(); // 密码加密
					pd.put("password", passwd);
					doctorService.editPass(pd);
					result = "0000";
					message = "修改成功";
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
			result = "9999";
			message = "服务器异常";
		} finally {
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 判断诊所是否是vip
	 * 
	 * @throws Exception
	 */
	public int isVip(PageData pd) throws Exception {
		int isVip = 0;
		PageData clinicData = clinicService.findzsClinic(pd);
		if (IsNull.paramsIsNull(clinicData) == false && IsNull.paramsIsNull(clinicData.get("vipEndTime")) == false) {
			String now = DateUtil.getTime();
			if (DateUtil.compareDate(clinicData.get("vipEndTime").toString(), now)) {
				isVip = 1;
			}
		}
		return isVip;
	}

	/**
	 * 4. 描述：查询个人信息 20180524修改
	 * 
	 * @author 霍学杰
	 * @date 2017年8月19日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/findDoctorInfo")
	@ResponseBody
	public Object findDoctorInfo() {
		logBefore(logger, "查询个人信息");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 判断 当前登录人id
			if (IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				map.put("signCloud", AppTencentCloudController.getUserSign(pd.get("doctorId").toString()));
				PageData doctor = doctorService.findDoctorInfo(pd);
				String state = "0";
				try {
					state = doctor.get("password").toString();
					state = "1";
					doctor.remove("password");
				} catch (Exception e) {
					state = "0";
					System.out.println("需要设置密码");
				}
				map.put("state", state);
				if (doctor != null) {
					map.put("doctor", doctor);

					result = "0000";
					message = "成功";
					String content = "https://ws.120ksb.com/zxys_sys/api/doctor?id=" + pd.get("doctorId").toString();
					String fileName = pd.get("doctorId").toString();

					String doctorQR = doctorQR(content, fileName, doctor);
					map.put("doctorQR", doctorQR);

					// 判断是否是vip
					if (IsNull.paramsIsNull(doctor.get("clinicId")) == false) {
						pd.remove("doctorId");
						pd.put("clinicId", doctor.get("clinicId"));
						pd.put("isVip", isVip(pd));
						pd.remove("clinicId");
						map.put("db", pd);
					}

				} else {// 用户不存在
					doctor = doctorService.findById(pd);
					if (doctor != null) {
						map.put("doctor", doctor);

						// 判断是否是vip
						if (IsNull.paramsIsNull(doctor.get("clinicId")) == false) {
							pd.remove("doctorId");
							pd.put("clinicId", doctor.get("clinicId"));
							pd.put("isVip", isVip(pd));
							pd.remove("clinicId");
							map.put("db", pd);
						}

						result = "0000";
						message = "成功";
					} else {
						map.put("doctor", new PageData());
						result = "1002";
						message = "没有信息";
					}
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
	 * 5. 描述：完善医生个人信息
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月20日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(value = "/editDoctorInfo", method = RequestMethod.POST)
	@ResponseBody
	public Object editDoctorInfo(HttpServletRequest request,
			@RequestParam(value = "headUrl", required = false) MultipartFile[] files,
			@RequestParam(value = "trueName", required = false) String trueName,
			@RequestParam(value = "sex", required = false) String sex,
			@RequestParam(value = "birthTime", required = false) String birthTime,
			@RequestParam(value = "doctorId", required = false) String doctorId,
			@RequestParam(value = "wxNumber", required = false) String wxNumber,
			@RequestParam(value = "isPrint", required = false) String isPrint,
			@RequestParam(value = "visible", required = false) String visible,
			@RequestParam(value = "doctIntro", required = false) String doctIntro,
			@RequestParam(value = "bigOffice", required = false) String bigOffice,
			@RequestParam(value = "bigOfficeName", required = false) String bigOfficeName,
			@RequestParam(value = "officeList", required = false) String officeList,
			@RequestParam(value = "qqNumber", required = false) String qqNumber) throws Exception {
		logBefore(logger, "图片");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(doctorId) == false) {
				pd.put("doctorId", doctorId);
				PageData data = doctorService.findById(pd);
				if (data != null) {
					// 上传图片
					String filesPath = "";
					if (files != null && files.length > 0) {
						filesPath = UploadController.picUp(files);
						pd.put("headUrl", filesPath);
						map.put("headUrl", filesPath);
						// 董雪蕊，添加图片上传测试，如果图片上传失败，返回
						boolean can = UploadController.isTong(filesPath);
						if (!can) {
							map.put("result", "7003");// 图片上传失败，请检查网络连接！
							map.put("message", CommonMessage.CODE_7003);
							return AppUtil.returnObject(new PageData(), map);
						}
					} else {
						filesPath = data.get("headUrl").toString();
					}

					Integer putSex = 2;
					if (IsNull.paramsIsNull(trueName) == false) {
						pd.put("trueName", trueName);
						// 判断名称是否一样
						if (pd.get("trueName").toString().contains("知心医生")) {
							pd.put("trueName", data.get("trueName"));
						}
					}
					if (IsNull.paramsIsNull(sex) == false) {
						pd.put("sex", sex);
						putSex = Integer.parseInt(sex);
					} else {
						putSex = Integer.parseInt(data.get("sex").toString());
					}
					if (IsNull.paramsIsNull(birthTime) == false) {
						pd.put("birthTime", birthTime);
					} else {
						if (IsNull.paramsIsNull(data.get("birthTime")) == false) {
							birthTime = data.get("birthTime").toString();
						}
					}
					if (IsNull.paramsIsNull(doctIntro) == false) {
						pd.put("doctIntro", doctIntro);
					} else {
						if (IsNull.paramsIsNull(data.get("doctIntro")) == false) {
							doctIntro = data.get("doctIntro").toString();
						}
					}
					pd.put("qqNumber", qqNumber);
					pd.put("wxNumber", wxNumber);
					pd.put("isPrint", isPrint);
					pd.put("visible", visible);
					pd.put("bigOffice", bigOffice);

					if (IsNull.paramsIsNull(bigOfficeName) == false) {
						String[] str = bigOfficeName.split(",");
						Set set = new TreeSet();
						for (int i = 0; i < str.length; i++) {
							set.add(str[i]);
						}
						str = (String[]) set.toArray(new String[0]);
						StringBuffer s = new StringBuffer();
						for (int i = 0; i < str.length; i++) {
							if (i > 0) {
								str[i] = "，" + str[i];
							}
							s.append(str[i]);
						}
						pd.put("bigOfficeName", s.toString());
					}
					doctorService.editDoctorInfo(pd);
					PageData findbyDocid = clinicUserService.findbyDocid(pd);// 同步修改诊所成员信息
					if (findbyDocid != null) {
						clinicUserService.updateDocx(pd);// 修改
					}
					AppTencentCloudController.setUserInfo(doctorId, pd.getString("trueName"), putSex, birthTime,
							doctIntro, filesPath, cloudSignService);
					// 添加擅长科室解析 2018-1-30
					officeSave2(officeList, doctorId);
					result = "0000";
					message = "修改成功";
				}
			} else {
				result = "9993";
				message = "参数异常";
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
	 * 解析医生删除的科室，并保存 只保存一个大科室 没有了小科室 2018-2-23 董雪蕊 问答科室 第三版
	 * 
	 * @param officeList
	 * @throws Exception
	 */
	public void officeSave2(String officeList, String doctorId) throws Exception {
		PageData pd = new PageData();
		pd.put("doctorId", doctorId);
		if (IsNull.paramsIsNull(officeList) == false) {
			doctorBigSortService.del(pd);// 删除新版的医生科室表
			JSONArray jsonArray = JSONArray.fromObject(officeList);
			if (jsonArray.size() > 0) {
				for (int i = 0; i < jsonArray.size(); i++) {
					JSONObject data = JSONObject.fromObject(jsonArray.get(i));
					String topOfficeId = data.getString("bigOffice");
					pd.put("topOfficeId", topOfficeId);
					pd.put("isQuan", 0);
					doctorBigSortService.save(pd);
				}
			}
		}

	}

	/**
	 * 6.描述：医生端 修改密码
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月20日
	 * @version 1.0
	 * @return
	 */
	@RequestMapping(value = "/editPass", method = RequestMethod.POST)
	@ResponseBody
	public Object editPass() {
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("password")) || IsNull.paramsIsNull(pd.get("doctorId"))
					|| IsNull.paramsIsNull(pd.get("oldPass"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData db = doctorService.findById(pd);
				String OldPass = pd.get("oldPass").toString().trim();
				String oldPass = new SimpleHash("SHA-1", OldPass).toString();
				if (db.get("password").equals(oldPass)) {
					String PASSWORD = pd.get("password").toString().trim();
					String passwd = new SimpleHash("SHA-1", PASSWORD).toString(); // 密码加密
					pd.put("password", passwd);
					doctorService.editPass(pd);
					result = "0000";// 修改成功
					message = "修改成功";
				} else {
					result = "1002";// 旧密码不正确
					message = "旧密码不正确";
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
	 * 7.描述：医生端 修改手机号
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月20日
	 * @version 1.0
	 * @return
	 */
	@RequestMapping(value = "/editPhone", method = RequestMethod.POST)
	@ResponseBody
	public Object editPhone() {
		logBefore(logger, "医生端 修改手机号");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("phone")) || IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData db = doctorService.findById(pd);
				if (db != null) {
					// 修改手机号
					doctorService.editDoctorInfo(pd);
					PageData findbyDocid = clinicUserService.findbyDocid(db);// 同步修改诊所成员信息
					if (findbyDocid != null) {
						clinicUserService.updateDocx(pd);// 修改
					}
					PageData paiData = schedulService.findById(pd);// 同步修改门诊排班信息
					if (IsNull.paramsIsNull(paiData) == false) {
						schedulService.editSchedul(pd);
					}
					result = "0000";
					message = "修改成功";
				} else {
					result = "1002";
					message = "修改失败";
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
	 * 8.描述：查询手机号是否存在
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月20日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/findByPhone", method = RequestMethod.POST)
	@ResponseBody
	public Object findByPhone() {
		logBefore(logger, "查询手机号是否存在");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("phone"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData db = doctorService.findByPhone(pd);
				if (db != null) {
					result = "1002";
					message = "该手机号已注册";
				} else {// 用户不存在
					result = "0000";
					message = "手机号不存在";
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
	 * 13.描述：医生端 接诊
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月26日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/updatestate", method = RequestMethod.POST)
	@ResponseBody
	public Object updatestate() {
		logBefore(logger, "医生端 接诊");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 判断 当前登录人id
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("doctorName"))
					|| IsNull.paramsIsNull(pd.get("checkinId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData findGH = clinic_checkinService.findGH(pd);
				if (findGH != null) {
					if (IsNull.paramsIsNull(findGH.get("doctorId"))) {
						pd.put("state", "1");
						clinic_checkinService.updatestate(pd);
						result = "0000";
						message = "接诊成功";
					} else if (findGH.get("doctorId").toString().equals(pd.get("doctorId").toString())) {
						pd.put("state", "1");
						clinic_checkinService.updatestate(pd);
						result = "0000";
						message = "接诊成功";
					} else {
						result = "1002";
						message = "接诊失败";
					}
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
	 * 14.描述：查看患者 历史病例
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月26日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/findhzJZlist", method = RequestMethod.POST)
	@ResponseBody
	public Object findhzJZlist() {
		logBefore(logger, "查看患者 历史病例");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("pageSize", 10);
				List<PageData> resultList = clinic_checkinService.findhzJZlist(pd);
				// 分页处理 （当前页-1）*页面大小，页面大小
				Integer min = 0;
				Integer max = 0;
				Integer pageIndex = 0;
				max = Integer.parseInt(pd.get("pageSize").toString().trim());
				int num = resultList.size();
				int pageTotal = 0;// 总页数
				if ((num % max) > 0) {
					pageTotal = num / max + 1;
				} else {
					pageTotal = num / max;
				}
				if (IsNull.paramsIsNull(pd.get("pageIndex"))) {
					pageIndex = 0;
				} else {
					pageIndex = Integer.parseInt(pd.get("pageIndex").toString().trim());
					min = (pageIndex * max) - max;
				}
				pd.put("min", min);
				pd.put("max", max);
				pd.put("page", 1);

				List<PageData> list = clinic_checkinService.findhzJZlist(pd);
				if (list == null || list.size() <= 0) {
					pd.put("checkinId", pd.get("patientId"));
					list = new ArrayList<>();
					PageData data = clinic_checkinService.findByidxq(pd);// 不符合逻辑，记得改
					if (IsNull.paramsIsNull(data) == false) {
						list.add(data);
						pageTotal = 0;
					}
				}
				if (list != null && list.size() > 0) {
					PageData pp = new PageData();
					for (int i = 0; i < list.size(); i++) {
						PageData dataList = list.get(i);
						pp.put("patientId", dataList.get("patientId"));
						pp.put("doctorId", dataList.get("doctorId"));
						PageData one = friendsService.findDossier(pp);
						if (one != null) {
							dataList.put("memoName", one.get("memoName"));
							dataList.put("headUrl", one.get("headUrl"));
						}
					}
					map.put("list", list);
					map.put("pageTotal", pageTotal);// 总页数
					map.put("size", list.size());
					result = "0000";
					message = "成功";
				} else {
					result = "0000";
					map.put("list", new ArrayList<PageData>());
					map.put("pageTotal", pageTotal);
					map.put("size", 0);
					message = "成功";
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
	 * 17.描述：医生端 取消患者挂号
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月31日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/editstate", method = RequestMethod.POST)
	@ResponseBody
	public Object editstate() {
		logBefore(logger, "取消患者挂号");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 判断 当前登录人id
			if (IsNull.paramsIsNull(pd.get("state")) || IsNull.paramsIsNull(pd.get("checkinId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData findGH = clinic_checkinService.findGH(pd);
				if (findGH != null) {
					int state = Integer.parseInt(findGH.get("state").toString());
					int yuState = Integer.parseInt(findGH.get("yuState").toString());
					if (state == 0) {
						if (yuState == 1) {
							pd.put("yuState", "2");
						} else if (yuState != 1 && yuState != 0) {
							pd.put("yuState", yuState + "");
						}
						clinic_checkinService.updatestate(pd);
						result = "0000";
						message = "取消挂号成功";
						// 推送 退费
						String sex = "";
						if ("0".equals(findGH.get("patientSex").toString())) {
							sex = "女";
						} else {
							sex = "男";
						}
						String cfz = "";
						if ("0".equals(findGH.get("patientVisit").toString())) {
							cfz = "初诊";
						} else {
							cfz = "复诊";
						}
						String messageConet = "患者：" + findGH.getString("patientName") + " " + sex + " "
								+ findGH.get("patientAge") + " " + cfz + " 已经取消挂号。";
						pd.put("clinicId", findGH.get("clinicId"));
						pd.put("ss", "1");
						List<PageData> findcylist = clinicUserService.findcylist(pd);
						PageData dd = new PageData();
						if (findcylist != null && findcylist.size() > 0) {
							for (int j = 0; j < findcylist.size(); j++) {
								dd = findcylist.get(j);//

								String phoneSole = "";
								String huaWeiToken = "";
								String miRegId = "";
								String mzPushId = "";
								phoneSole = dd.getString("phoneSole");
								huaWeiToken = dd.getString("huaWeiToken");
								miRegId = dd.getString("miRegId");
								mzPushId = dd.getString("mzPushId");

								Collection<String> alias = new ArrayList<>();
								Collection<String> registrationId = new ArrayList<>();
								registrationId.add(dd.get("phoneSole").toString());
								String toUserId = dd.get("doctorId").toString();
								alias.add(toUserId);
								String title = "取消挂号";
								jPush.sendAll(alias, registrationId, title, "16", messageConet, "", "0", toUserId, "1",
										DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
										mzPushId, phoneSole);
							}
						}

					} else {
						result = "1002";
						message = "医生已接诊取消失败";
					}
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
	 * 17.1描述：医生端 取消患者挂号 v2
	 * 
	 * @auther 霍学杰
	 * @date 2017年10月4日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/editstate2", method = RequestMethod.POST)
	@ResponseBody
	public Object editstate2() {
		logBefore(logger, "医生端 取消患者挂号");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 判断 当前登录人id
			if (IsNull.paramsIsNull(pd.get("state")) || IsNull.paramsIsNull(pd.get("checkinId"))
					|| IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData findGH = clinic_checkinService.findGH(pd);
				if (findGH != null) {
					int state = Integer.parseInt(findGH.get("state").toString());
					// 预约状态 0不是预约的用户 1预约中 2患者取消 预约，在医生同意前3医生拒绝预约 4同意预约了，即预约逻辑结束 5预约医生未处理，系统取消
					// 6预约成功后患者没来，系统取消 7接诊后患者没来医生取消
					int yuState = Integer.parseInt(findGH.get("yuState").toString());
					if (state == 0 || state == 1) {
						if (yuState == 1) {
							pd.put("yuState", "2");
						} else if (yuState != 1 && yuState != 0) {
							pd.put("yuState", yuState + "");
						}

						clinic_checkinService.updatestate(pd);
						result = "0000";
						message = "取消接诊成功";
						// 待开发推送患者端

					} else {
						result = "1002";
						message = "取消失败";
					}
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
	 * 19.描述：异步查询 初诊复诊
	 * 
	 * @author 霍学杰
	 * @date 2017年9月1日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/findczfz", method = RequestMethod.POST)
	@ResponseBody
	public Object findczfz() {
		logBefore(logger, "医生端 异步查询 初诊复诊");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 判断 当前登录人id
			if (IsNull.paramsIsNull(pd.get("phone")) || IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData findbyphonecheck = clinic_checkinService.findbyphonecheck(pd);
				if (findbyphonecheck != null) {
					map.put("patientVisit", 1);
					map.put("patientName", findbyphonecheck.get("patientName"));
					map.put("patientSex", findbyphonecheck.get("patientSex"));
					map.put("patientAge", findbyphonecheck.get("patientAge"));
					map.put("patientMedicalHistory", findbyphonecheck.get("patientMedicalHistory"));

					if (IsNull.paramsIsNull(findbyphonecheck.get("patientId")) == false) {
						pd.put("patientId", findbyphonecheck.get("patientId"));
						map.put("patientId", findbyphonecheck.get("patientId"));
					} else {
						pd.put("patientId", "");
						map.put("patientId", "");
					}
					message = "复诊";
				} else {
					PageData db = patientService.findByPhone(pd);
					if (db != null) {
						pd.put("patientId", db.get("patientId"));
						pd.put("sy", 1);
						List<PageData> list1 = clinic_checkinService.findhzJZlist(pd);
						if (list1 != null && list1.size() > 0) {
							map.put("patientVisit", 1);
							map.put("patientId", db.get("patientId"));
							map.put("patientName", db.get("trueName"));
							map.put("patientSex", db.get("sex"));
							map.put("patientAge", db.get("age"));
							map.put("patientMedicalHistory", db.get("patientMedicalHistory"));
							message = "复诊";
						} else {
							map.put("patientVisit", 0);
							map.put("patientId", db.get("patientId"));
							message = "初诊";
						}
					} else {// 用户不存在
						map.put("patientVisit", 0);
						message = "初诊";
					}
				}
				result = "0000";
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
	 * @description 更新推送注册ID
	 * @date 2017年11月3日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/editRegistId")
	@ResponseBody
	public Object name() {
		logBefore(logger, "更新推送注册ID");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String retMessage = "连接异常";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId"))) {
				result = "9993";
				retMessage = "参数异常";
			} else {
				if (IsNull.paramsIsNull(pd.get("phoneSole")) == false) {
					// 极光ID
					pd.put("huaWeiToken", "0");
					pd.put("miRegId", "0");
					pd.put("mzPushId", "0");
				} else if (IsNull.paramsIsNull(pd.get("huaWeiToken")) == false) {
					// 华为ID
					pd.put("phoneSole", "0");
					pd.put("miRegId", "0");
					pd.put("mzPushId", "0");
				} else if (IsNull.paramsIsNull(pd.get("miRegId")) == false) {
					// 小米ID
					pd.put("huaWeiToken", "0");
					pd.put("phoneSole", "0");
					pd.put("mzPushId", "0");
				} else if (IsNull.paramsIsNull(pd.get("mzPushId")) == false) {
					// 魅族ID
					pd.put("huaWeiToken", "0");
					pd.put("miRegId", "0");
					pd.put("phoneSole", "0");
				}
				doctorService.editDoctorInfo(pd);
				result = "0000";
				retMessage = "成功";
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
	 * @description 根据ID判断是医生还是患者
	 * @date 2017年12月30日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/isDocOrPat", method = RequestMethod.POST)
	@ResponseBody
	public Object isDocOrPat() {
		logBefore(logger, "根据ID判断是医生还是患者");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("userId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				String isRole = "notOther";
				pd.put("doctorId", pd.get("userId"));
				PageData data = doctorService.findById(pd);
				if (IsNull.paramsIsNull(data) == false) {
					isRole = "doctor";
					map.put("db", data);
				} else {
					data = patientService.findById(pd);
					isRole = "patient";
					if (IsNull.paramsIsNull(data) == false) {
						map.put("db", data);
					}
				}

				map.put("isRole", isRole);
				if (IsNull.paramsIsNull(data)) {
					map.put("db", new PageData());
				}
				result = "0000";
				message = "成功";
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
	 * 描述：登录接口 app在使用 20180828
	 * 
	 * @author 霍学杰
	 * @date 2018年1月25日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/loginweb", method = RequestMethod.POST)
	@ResponseBody
	public Object loginweb() {
		logBefore(logger, "登录接口-知心医生医生版");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		map.put("result", "1000");
		map.put("message", "网络异常");
		try {
			if (IsNull.paramsIsNull(pd.get("phone"))) {
				map.put("result", "9993");
				map.put("message", "参数异常");
				return AppUtil.returnObject(new PageData(), map);
			} else {
				// 1 先校验手机号是否正确
				boolean mobile = PhoneCheck.isMobile(pd.get("phone").toString());
				if (mobile) {// 手机号正确
					PageData byPhone = doctorService.findByPhone(pd);
					if (byPhone != null) {
						List<String> list = new ArrayList<String>();
						list.add("平台客服");
						if (!"13831128915".equals(pd.get("phone").toString())) {
							// 平台客服 在线充值 我的钱包
							list.add("在线充值");
							list.add("我的钱包");
						}
						if ("13831128915".equals(pd.get("phone").toString()) && CommonConfig.PIC_FLAG != 0) {
							// 平台客服 在线充值 我的钱包
							list.add("在线充值");
							list.add("我的钱包");
						}
						map.put("list", list);

						String phone2 = "";
						try {
							phone2 = byPhone.get("password").toString();
						} catch (Exception e) {
							phone2 = "";
						}
						if (IsNull.paramsIsNull(phone2) && IsNull.paramsIsNull(pd.get("password"))) {
							map.put("result", "1021");
							map.put("message", "手机号验证通过");// 手机号不存在,发验证码注册
							return AppUtil.returnObject(new PageData(), map);
						} else if (IsNull.paramsIsNull(phone2) && IsNull.paramsIsNull(pd.get("password")) == false) {
							String PASSWORD = pd.get("password").toString().trim();
							String passwd = new SimpleHash("SHA-1", PASSWORD).toString(); // 密码加密
							pd.put("password", passwd);
							doctorService.editPass(pd);// 设置密码成功
							String ip = getRemortIP(pd.get("phone").toString());
							pd.put("IP", ip);
							pd.put("doctorId", byPhone.get("doctorId"));
							doctorService.editDoctorInfo(pd);
							byPhone.put("IP", ip);
							// 登录成功后，处理医生的登录状态
							doctorService.editIsDeng(byPhone);
							byPhone.put("onlineState", 1);
							String doctorQR = logindoctorQR(byPhone);
							byPhone.put("doctorQR", doctorQR);
							map.put("doctor", byPhone);
							map.put("result", "0000");
							map.put("message", "设置密码成功");
							// 最后这里我要返回腾讯云的sign，登录就返回
							String signCloud = "";
							if (IsNull.paramsIsNull(pd.get("IMlive")) == false
									&& "2".equals(pd.get("IMlive").toString())) {
								signCloud = AppTencentCloudController.getUserSign(byPhone.get("doctorId").toString());
							} else if (IsNull.paramsIsNull(pd.get("IMlive")) == false
									&& "3".equals(pd.get("IMlive").toString())) {
								signCloud = AppTencentCloudController.getUserSign_h(byPhone.get("doctorId").toString());
							}
							map.put("signCloud", signCloud);

							return AppUtil.returnObject(new PageData(), map);
						} else if (IsNull.paramsIsNull(pd.get("password")) == false) {
							String PASSWORD = pd.get("password").toString().trim();
							String passwd = new SimpleHash("SHA-1", PASSWORD).toString(); // 密码加密
							pd.put("password", passwd);
							PageData userLogin = doctorService.userLogin(pd);
							if (userLogin != null) {
								map.put("result", "0000");
								map.put("message", "登录成功");
								String ip = getRemortIP(pd.get("phone").toString());
								pd.put("IP", ip);
								pd.put("doctorId", userLogin.get("doctorId"));
								doctorService.editDoctorInfo(pd);
								userLogin.put("IP", ip);
								// 登录成功后，处理医生的登录状态
								doctorService.editIsDeng(userLogin);
								userLogin.put("onlineState", 1);

								String doctorQR = logindoctorQR(userLogin);
								userLogin.put("doctorQR", doctorQR);
								map.put("doctor", userLogin);

								// 最后这里我要返回腾讯云的sign，登录就返回

								String signCloud = "";

								if (IsNull.paramsIsNull(pd.get("IMlive")) == false
										&& "2".equals(pd.get("IMlive").toString())) {
									signCloud = AppTencentCloudController
											.getUserSign(userLogin.get("doctorId").toString());
								} else if (IsNull.paramsIsNull(pd.get("IMlive")) == false
										&& "3".equals(pd.get("IMlive").toString())) {
									signCloud = AppTencentCloudController
											.getUserSign_h(userLogin.get("doctorId").toString());
								}
								map.put("signCloud", signCloud);

								return AppUtil.returnObject(new PageData(), map);
							} else {
								map.put("result", "1002");
								map.put("message", "密码或账号错误");
								return AppUtil.returnObject(new PageData(), map);
							}

						} else {
							map.put("result", "1020");
							map.put("message", "手机号验证通过");
							return AppUtil.returnObject(new PageData(), map);
						}

					} else {
						if (IsNull.paramsIsNull(pd.get("password")) == false) {

							// 检测用户是否使用邀请码
							if (IsNull.paramsIsNull(pd.get("inviteCode")) == false) {
								String inviteCode = InviteCodeControler.InviteCode(1, pd, inviteCodeService);
								if (IsNull.paramsIsNull(inviteCode) == false) {
									pd.put("inviteCode", inviteCode);
								} else {
									map.put("result", "6003");
									map.put("message", "邀请码错误");
									return AppUtil.returnObject(new PageData(), map);
								}
							} else {
								String creatInviteCode = InviteCodeControler.CreatInviteCode(1, inviteCodeService);
								pd.put("inviteCode", creatInviteCode);
							}

							// 为空 注册
							String PASSWORD = pd.get("password").toString().trim();
							String passwd = new SimpleHash("SHA-1", PASSWORD).toString(); // 密码加密
							pd.put("password", passwd);
							pd.put("doctorId", this.get32UUID());
							pd.put("createTime", DateUtil.getTime());
							doctorService.saveUser(pd);// zhuce
							map.put("message", "注册成功");
							String ip = getRemortIP(pd.get("phone").toString());
							pd.put("IP", ip);
							// 根据医生id查询医生随机数
							PageData tn = doctorService.findById(pd);
							pd.put("trueName", "知心医生" + tn.get("randomNumDoc"));
							doctorService.editDoctorInfo(pd);
							// 这里要去腾讯云那注册下，注册下！
							AppTencentCloudController.OneImport(pd.getString("doctorId"), pd.getString("trueName"),
									CommonConfig.DOC_HEAD_URL, cloudSignService);

							PageData doctorInfo = doctorService.findByPhone(pd);
							doctorInfo.remove("password");

							// 登录成功后，处理医生的登录状态
							doctorService.editIsDeng(doctorInfo);
							doctorInfo.put("onlineState", 1);

							String doctorQR = logindoctorQR(doctorInfo);
							doctorInfo.put("doctorQR", doctorQR);
							map.put("doctor", doctorInfo);

							map.put("result", "0000");
							// 最后这里我要返回腾讯云的sign，登录就返回
							String signCloud = "";
							if (IsNull.paramsIsNull(pd.get("IMlive")) == false
									&& "2".equals(pd.get("IMlive").toString())) {
								signCloud = AppTencentCloudController
										.getUserSign(doctorInfo.get("doctorId").toString());
							} else if (IsNull.paramsIsNull(pd.get("IMlive")) == false
									&& "3".equals(pd.get("IMlive").toString())) {
								signCloud = AppTencentCloudController
										.getUserSign_h(doctorInfo.get("doctorId").toString());
							}

							map.put("signCloud", signCloud);

							// 添加时段统计
							PageData dd = new PageData();
							String[] region = DituUtil.getprovince(ip);
							dd.put("flag", DateUtil.getTime1().substring(0, 2));
							dd.put("dayDate", DateUtil.getDay());
							dd.put("region", region[1]);
							dd.put("province", region[0]);
							dd.put("patOrDoc", "1");
							if (IsNull.paramsIsNull(pd.get("state")) == false) {
								if ("1".equals(pd.get("state").toString())) {
									dd.put("terminal", "安卓");
								} else if ("2".equals(pd.get("state").toString())) {
									dd.put("terminal", "IOS");
								} else if ("3".equals(pd.get("state").toString())) {
									dd.put("terminal", "小程序");
								} else if ("4".equals(pd.get("state").toString())) {
									dd.put("terminal", "web端");
								}
							}
							PageData findByParam = activeHoursService.findByParam(dd);
							if (findByParam != null) {
								// 更新时段数据
								dd.put("hoursId", findByParam.get("hoursId"));
								dd.put("addUser", 1 + Integer.parseInt(findByParam.get("addUser").toString()));
								activeHoursService.updateHoursACt(dd);
							} else {
								// 添加时段数据
								dd.put("addUser", 1);
								dd.put("createTime", DateUtil.getTime());
								activeHoursService.save(dd);
							}
							return AppUtil.returnObject(new PageData(), map);
						} else {
							map.put("result", "1021");
							map.put("message", "手机号正确");// 手机号不存在,发验证码注册
							return AppUtil.returnObject(new PageData(), map);
						}
					}
				} else {
					map.put("result", "1041");
					map.put("message", "请输入正确手机号");
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			map.put("result", "9999");
			map.put("message", "系统繁忙");
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 医生设置自己的擅长疾病
	 * 
	 * @auther 董雪蕊
	 * @date 2018年2月23日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/setGoodDise", method = RequestMethod.POST)
	@ResponseBody
	public Object setGoodDise() {
		logBefore(logger, "医生设置自己的擅长疾病");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("shanDisease"))) {
				result = "9993";
				message = "参数异常";
			} else {
				doctorService.editDoctorInfo(pd);
				result = "0000";
				message = "成功";
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
	 * 医生设置自己的从业年限
	 * 
	 * @auther 董雪蕊
	 * @date 2018年2月26日
	 * @version 1.2
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/setStartWork", method = RequestMethod.POST)
	@ResponseBody
	public Object setStartWork() {
		logBefore(logger, "医生设置自己的从业年限");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("startWorkYear"))) {
				result = "9993";
				message = "参数异常";
			} else {
				String startWorkYear = pd.get("startWorkYear").toString();
				pd.put("workYearS", DateUtil.getDiffYear(startWorkYear, DateUtil.getTime()));
				doctorService.editDoctorInfo(pd);
				result = "0000";
				message = "成功";
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
	 * @Explain 医生二维码生成
	 * @author 霍学杰
	 * @param content
	 * @param fileName
	 * @param doctor
	 * @return
	 * @throws Exception
	 */

	public String logindoctorQR(PageData doctor) throws Exception {

		String doctorQR = "";

		String logoPath = "";
		String destPath = "";

		String content = "";
		String fileName = "";

		JsonObject lan1 = new JsonObject();
		if (IsNull.paramsIsNull(doctor.get("trueName")) == false) {
			lan1.addProperty("trueName", doctor.get("trueName").toString());
		} else {
			lan1.addProperty("trueName", "知心医生" + doctor.get("randomNumDoc").toString());
		}
		if (IsNull.paramsIsNull(doctor.get("headUrl")) == false) {
			lan1.addProperty("headUrl", doctor.get("headUrl").toString());
		} else {
			lan1.addProperty("headUrl", CommonConfig.DOC_HEAD_URL);
		}

		if (IsNull.paramsIsNull(doctor.get("clinicId")) == false) {
			lan1.addProperty("clinicId", doctor.get("clinicId").toString());
		} else {
			lan1.addProperty("clinicId", "");
		}

		if (IsNull.paramsIsNull(doctor.get("totalPingStar")) == false) {
			lan1.addProperty("totalPingStar", doctor.get("totalPingStar").toString());
		} else {
			lan1.addProperty("totalPingStar", "");
		}

		if (IsNull.paramsIsNull(doctor.get("sex")) == false) {
			lan1.addProperty("sex", doctor.get("sex").toString());
		} else {
			lan1.addProperty("sex", "1");
		}

		if (IsNull.paramsIsNull(doctor.get("age")) == false) {
			lan1.addProperty("age", doctor.get("age").toString());
		} else {
			lan1.addProperty("age", "0");
		}

		content = "https://ws.120ksb.com/zxys_sys/api/doctor?id=" + doctor.get("doctorId").toString() + "&json=" + lan1;
		fileName = doctor.get("doctorId").toString();

		switch (CommonConfig.PIC_FLAG) {
		case 0:
			logoPath = "E:\\ApacheServe\\tomcat_pic\\webapps\\pic\\512.png";// logo地址
			// 头像处理
			int starNum = 0;
			int endNum = 0;
			if (IsNull.paramsIsNull(doctor.get("headUrl")) == false) {
				String ss = doctor.get("headUrl").toString();
				starNum = ss.length();
				ss = ss.replaceAll("http://jk.120ksb.com:8003/pic/picResource/zxys_sys/", "");
				endNum = ss.length();
				if (starNum > endNum) {
					String[] split = ss.split("/");
					if (split.length > 1) {
						logoPath = "E:\\ApacheServe\\tomcat_pic\\webapps\\pic\\picResource\\zxys_sys\\" + split[0]
								+ "\\" + split[1];
					}
				} else {
					ss = doctor.get("headUrl").toString();
					starNum = ss.length();
					ss = ss.replaceAll("https://ws.120ksb.com/picture/api/", "");
					endNum = ss.length();
					if (starNum > endNum) {
						String[] split = ss.split("/");
						if (split.length > 1) {
							logoPath = "E:\\apache-tomcat-7.0.72\\webapps\\picture\\api\\" + split[0] + "\\" + split[1];
						}
					} else {
						ss = doctor.get("headUrl").toString();
						starNum = ss.length();
						ss = ss.replaceAll("http://jk.120ksb.com:8003/touxiang/", "");
						endNum = ss.length();
						if (starNum > endNum) {
							if (ss.length() > 1) {
								logoPath = "E:\\ApacheServe\\tomcat_pic\\webapps\\touxiang\\" + ss;
							}
						}
					}

				}
			}
			destPath = "E:\\apache-tomcat-7.0.72\\webapps\\picture\\zxys\\";// 存放目录
			QRCodeUtil.encode(content, logoPath, destPath, fileName, true);
			doctorQR = "https://ws.120ksb.com/picture/zxys/" + fileName + ".jpg";
			break;
		case 2:
			destPath = "D:\\tomcat\\apache-tomcat-7.0.68\\webapps\\pic\\zxys\\";// 存放目录
			QRCodeUtil.encode(content, logoPath, destPath, fileName, true);

			doctorQR = "http://192.168.42.215:8086/pic/zxys/" + fileName + ".jpg";
			break;
		case 3:
			destPath = "E:\\server\\tomcat-8.5.8022\\webapps\\pic\\zxys\\";// 存放目录
			QRCodeUtil.encode(content, logoPath, destPath, fileName, true);

			doctorQR = "http://222.222.24.149:8022/pic/zxys/" + fileName + ".jpg";
			break;
		case 1:
			destPath = "E:\\server\\apache-tomcat-8.5.20\\webapps\\pic\\zxys\\";// 存放目录
			QRCodeUtil.encode(content, logoPath, destPath, fileName, true);

			doctorQR = "http://222.222.24.144:8083/pic/zxys/" + fileName + ".jpg";
			break;
		default:
			break;
		}
		return doctorQR;
	}

	public String getRemortIP() throws Exception {
		HttpServletRequest request = this.getRequest();
		String ip = "";
		if (request.getHeader("x-forwarded-for") == null) {
			ip = request.getRemoteAddr();
		} else {
			ip = request.getHeader("x-forwarded-for");
		}
		return ip;
	}

	public PageData putPd(PageData pd) {

		pd.put("province", "");
		pd.put("city", "");
		pd.put("area", "");
		pd.put("detailAddress", "");

		if (IsNull.paramsIsNull(pd.get("IP"))) {
			pd.put("IP", "");
		} else {
			String[] getprovince = DituUtil.getprovince(pd.getString("IP"));
			if (IsNull.paramsIsNull(getprovince[0]) == false) {
				pd.put("province", getprovince[0]);
			}
			if (IsNull.paramsIsNull(getprovince[1]) == false) {
				pd.put("city", getprovince[1]);
			}
		}
		// 添加地址
		if (IsNull.paramsIsNull(pd.get("longitude")) == false && IsNull.paramsIsNull(pd.get("latitude")) == false) {
			String location = pd.get("longitude").toString() + "," + pd.get("latitude").toString();
			PageData data = DituUtil.GPSToaddress(location);
			pd.putAll(data);
		} else {
			pd.put("longitude", "");
			pd.put("latitude", "");
		}
		return pd;
	}
}
