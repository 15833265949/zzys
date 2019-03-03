package com.fh.controller.app.patient;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.config.CommonConfig;
import com.fh.config.CommonMessage;
import com.fh.controller.app.tencentCloud.AppTencentCloudController;
import com.fh.controller.base.BaseController;
import com.fh.controller.newapi.patient.PatientLoginController;
import com.fh.controller.statistics.invitecode.InviteCodeControler;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.CloudSignService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.LoginLogService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.NewPushService;
import com.fh.service.app.NewsService;
import com.fh.service.app.OnlineCountService;
import com.fh.service.app.PatientService;
import com.fh.service.statistics.ActiveHoursService;
import com.fh.service.statistics.AppletService;
import com.fh.service.statistics.AscriptionService;
import com.fh.service.statistics.InviteCodeService;
import com.fh.service.statistics.TjFunctionService;
import com.fh.service.statistics.TjPerinforService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.MapDistance;
import com.fh.util.PageData;
import com.fh.util.ditu.DituUtil;
import com.fh.util.tools.EmojiUtil;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PhoneCheck;
import com.fh.util.tools.aes.AES;
import com.fh.util.tools.jPush.examples.jPush;
import com.fh.util.weixin.openId;

import net.sf.json.JSONObject;

/**
 * Title:患者模块所有接口列表
 * 
 * @author 霍学杰
 * @date 2017年8月20日
 * @version 1.0
 */
@Controller
@RequestMapping(value = "/api/patient")
@SuppressWarnings("rawtypes")
public class PatientControler extends BaseController {
	@Resource
	private PatientService patientService;
	@Resource(name = "loginLogService")
	private LoginLogService loginLogService;
	@Resource
	private DoctorService doctorService;
	@Resource
	private TjFunctionService functionService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private ClinicUserService clinicUserService;
	@Resource
	private MonthCountService monthCountService;
	@Resource
	private NewPushService newPushService;
	@Resource
	private NewsService newsService;
	@Resource
	private CloudSignService cloudSignService;
	@Resource
	private ActiveHoursService activeHoursService;
	@Resource
	private OnlineCountService onlineCountService;
	@Resource
	private InviteCodeService inviteCodeService;
	@Resource
	private AscriptionService ascriptionService;
	@Resource
	private TjPerinforService perinforService;
	@Resource
	private AppletService appletService;
	
	@Resource
	private TjPerinforService tjPerinforService;

	/**
	 * 1. 描述：登录
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月20日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	@ResponseBody
	public Object login() {
		logBefore(logger, "患者端-登录");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 验证 用户名 密码 不为空
			if (IsNull.paramsIsNull(pd.get("phone")) || IsNull.paramsIsNull(pd.get("password"))) {
				result = "9993";
				message = "参数异常";
			} else {
				String PASSWORD = pd.get("password").toString().trim();
				String passwd = new SimpleHash("SHA-1", PASSWORD).toString(); // 密码加密
				pd.put("password", passwd);
				PageData patient = patientService.userLogin(pd);// 登录
				if (patient != null) {
					map.put("db", patient);
					result = "0000"; // success
					message = "登录成功";

					// 最后这里我要返回腾讯云的sign，登录就返回
					String signCloud = "";
					if (IsNull.paramsIsNull(pd.get("IMlive")) == false && "2".equals(pd.get("IMlive").toString())) {
						signCloud = AppTencentCloudController.getUserSign(patient.getString("patientId"));
					} else if (IsNull.paramsIsNull(pd.get("IMlive")) == false
							&& "3".equals(pd.get("IMlive").toString())) {
						signCloud = AppTencentCloudController.getUserSign_h(patient.getString("patientId"));
					}

					map.put("signCloud", signCloud);
				} else {// 用户不存在
					map.put("patient", new PageData());
					result = "1003";
					message = CommonMessage.CODE_1003;// 密码错误！
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
	 * 7.描述：患者端 修改手机号
	 * 
	 * @author 董雪蕊
	 * @date 2017年12月25日
	 * @version 1.0
	 * @return
	 */
	@RequestMapping(value = "/editPhone", method = RequestMethod.POST)
	@ResponseBody
	public Object editPhone() {
		logBefore(logger, "患者端 修改手机号");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("phone")) || IsNull.paramsIsNull(pd.get("patientId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				// 查询手机号是否存在
				PageData db = patientService.findByPhone(pd);
				if (IsNull.paramsIsNull(db)) {
					patientService.editPhone(pd);
					result = "0000";
					message = "修改成功";
				} else {
					result = "1104";
					message = CommonMessage.CODE_1104;// 手机号已存在，修改失败！
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
	 * 3.描述：患者端 修改密码
	 * 
	 * @author 霍学杰
	 * @date 2017年7月7日
	 * @version 1.0
	 * @return
	 */
	@RequestMapping(value = "/editPass", method = RequestMethod.POST)
	@ResponseBody
	public Object editPass() {
		logBefore(logger, "患者端 修改密码");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("password")) || IsNull.paramsIsNull(pd.get("patientId"))
					|| IsNull.paramsIsNull(pd.get("oldPass"))) {
				result = "9993";
				message = "参数异常";
			} else {
				// 查询患者详情
				PageData db = patientService.findPinfo(pd);
				String OldPass = pd.get("oldPass").toString().trim();
				String oldPass = new SimpleHash("SHA-1", OldPass).toString();
				if (IsNull.paramsIsNull(db) == false && IsNull.paramsIsNull(db.get("password")) == false
						&& db.get("password").equals(oldPass)) {
					String PASSWORD = pd.get("password").toString().trim();
					String passwd = new SimpleHash("SHA-1", PASSWORD).toString(); // 密码加密
					pd.put("password", passwd);
					patientService.editPass(pd);
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
	 * 4.描述：退出登录
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月24日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/loginOut", method = RequestMethod.POST)
	@ResponseBody
	public Object loginOut() {
		logBefore(logger, "患者端-退出登录");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("patientId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("loginId", pd.get("patientId"));
				// 根据医生ID查询日志信息
				PageData db = loginLogService.findLoginLog(pd);
				if (db != null) {
					pd.put("logId", db.get("logId"));
					pd.put("logoutTime", DateUtil.getTime());
					// 退出登录时修改日志信息
					loginLogService.editLogoutTime(pd);
					result = "0000";
					message = "退出成功";
				} else {// 用户登录信息不存在
					result = "1002";
					message = "退出成功";
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
	 * 5.描述： 0 忘记密码修改密码 1 医生注册医患交互系统 2 游客模式注册
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月24日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/composite", method = RequestMethod.POST)
	@ResponseBody
	public Object composite() throws Exception {
		logBefore(logger, "APP医生注册医患交互系统");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("type"))) {
				result = "9993";
				message = "参数异常";
			} else {
				int type = Integer.parseInt(pd.get("type").toString());
				if (type == 1) {// 注册
					if (IsNull.paramsIsNull(pd.get("phone")) || IsNull.paramsIsNull(pd.get("password"))) {
						result = "9993";
						message = "参数异常";
					} else {
						if ("17606448981".equals(pd.get("phone").toString())) {
							// 黑名单,回头黑人多了,建库
							result = "1041";
							message = "此账号存在违规操作，不能进行注册";
						} else {
							boolean mobile = PhoneCheck.isMobile(pd.get("phone").toString());
							if (mobile) {
								// 检测用户是否使用邀请码
								if (IsNull.paramsIsNull(pd.get("inviteCode")) == false) {
									String inviteCode = InviteCodeControler.InviteCode(0, pd, inviteCodeService);
									if (IsNull.paramsIsNull(inviteCode) == false) {
										pd.put("inviteCode", inviteCode);
									} else {
										result = "6003";
										message = "邀请码错误";
										map.put("result", result);
										map.put("message", message);
										return AppUtil.returnObject(new PageData(), map);
									}
								} else {
									String creatInviteCode = InviteCodeControler.CreatInviteCode(0, inviteCodeService);
									pd.put("inviteCode", creatInviteCode);
								}

								pd.put("IP", getRemortIP());
								pd.put("createTime", DateUtil.getTime());
								String passwd = new SimpleHash("SHA-1", pd.get("password")).toString(); // 密码加密
								pd.put("password", passwd);
								Random random = new Random();
								Thread.sleep(random.nextInt(1000));
								PageData findByPhone = patientService.findByPhone(pd);
								if (findByPhone == null) {
									pd.put("patientId", this.get32UUID());
									// pd.put("randomNum", enrollRandom());
									patientService.save(pd);
									PageData patient = patientService.findPinfo(pd);
									// 根据患者id查询医生随机数
									PageData dd = new PageData();
									dd.put("patientId", patient.get("patientId"));
									dd.put("trueName", "知心" + patient.get("randomNum").toString());
									patientService.editPatientInfo(dd);
									// 腾讯云那注册下，注册下！
									AppTencentCloudController.OneImport(dd.getString("patientId"),
											dd.getString("trueName"), CommonConfig.PAT_HEAD_URL, cloudSignService);
									// 添加时段统计
									PatientLoginController.patSAveHours(pd, activeHoursService);

								} else {
									pd.put("patientId", findByPhone.get("patientId"));
									patientService.editPass2(pd);
								}
								PageData patient = patientService.findPinfo(pd);
								map.put("patient", patient);
								result = "0000";
								message = "注册成功";

								// 添加深度链接注册数统计
								if (IsNull.paramsIsNull(pd.get("IMEI")) == false) {
									PageData imei = ascriptionService.findByIMEI(pd);
									if (imei != null) {
										// 更新注册数
										pd.put("id", imei.get("id"));
										pd.put("register", "1");
										pd.put("userId", pd.get("patientId"));
										ascriptionService.updateData(pd);
									}
								}
							} else {
								map.put("result", "1041");
								map.put("message", "手机号不正确");
							}
						}
					}
				} else if (type == 0) {
					// 修改密码
					if (IsNull.paramsIsNull(pd.get("phone")) || IsNull.paramsIsNull(pd.get("password"))) {
						result = "9993";
						message = "参数异常";
					} else {
						String Pass = pd.get("password").toString().trim();
						String passwd = new SimpleHash("SHA-1", Pass).toString(); // 密码加密
						pd.put("password", passwd);
						patientService.editPass2(pd);
						result = "0000";
						message = "修改成功";
					}
				} else if (type == 2) {// 游客模式

					boolean international = false;// 国内版
					if (IsNull.paramsIsNull(pd.get("international")) == false) {
						if ("1".equals(pd.get("international").toString())) {
							international = true;// 国际版
							pd.put("international", "1");// 国际
						}
					}
					// 先生成患者id
					pd.put("patientId", this.get32UUID());
					// 查询深度链接是否存在
					if (IsNull.paramsIsNull(pd.get("IMEI")) == false) {
						PageData imei = ascriptionService.findByIMEI(pd);
						if (imei != null) {
							pd.put("scanGenre", "2");// 0 正常 1扫码授权 2深度链接
							PageData pp = new PageData();
							pp.put("id", imei.get("id"));
							pp.put("userId", pd.get("patientId"));
							ascriptionService.updateData(pp);// 添加userId
						}
					}
					String creatInviteCode = InviteCodeControler.CreatInviteCode(0, inviteCodeService);
					pd.put("inviteCode", creatInviteCode);
					pd.put("IP", getRemortIP());
					pd.put("createTime", DateUtil.getTime());
					Random random = new Random();
					Thread.sleep(random.nextInt(1000));
					pd.put("headUrl", CommonConfig.PAT_HEAD_URL);

					pd.put("visitor", "1");// 游客
					patientService.saveVisitor(pd);

					// 添加小程序引流统计
					if (IsNull.paramsIsNull(pd.get("doctorId")) == false) {
						appletService.save(pd);
					}

					// 根据患者id查询医生随机数
					PageData patient = patientService.findPinfo(pd);
					if (patient != null) {
						PageData dd = new PageData();
						if (international) {
							// 国际
							dd.put("trueName", "Tourist" + patient.get("randomNum").toString());
						} else {
							// 国内
							dd.put("trueName", "游客" + patient.get("randomNum").toString());
						}
						dd.put("patientId", patient.get("patientId"));
						patientService.editPatientInfo(dd);
						// 腾讯云那注册下，注册下！
						AppTencentCloudController.OneImport(dd.getString("patientId"), dd.getString("trueName"),
								CommonConfig.PAT_HEAD_URL, cloudSignService);

						// 最后这里我要返回腾讯云的sign，登录就返回
						String signCloud = "";
						if (IsNull.paramsIsNull(pd.get("IMlive")) == false && "2".equals(pd.get("IMlive").toString())) {
							signCloud = AppTencentCloudController.getUserSign(patient.getString("patientId"));
						} else if (IsNull.paramsIsNull(pd.get("IMlive")) == false
								&& "3".equals(pd.get("IMlive").toString())) {
							signCloud = AppTencentCloudController.getUserSign_h(patient.getString("patientId"));
						}
						map.put("signCloud", signCloud);

						// 添加时段统计
						PatientLoginController.patSAveHours(pd, activeHoursService);

						patient = patientService.findPinfo(pd);
						map.put("patient", patient);
						result = "0000";
						message = "注册成功";
					}
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
	 * 获取登录用户的IP
	 * 
	 * @throws Exception
	 */
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

	/**
	 * 6.描述：患者端-查询手机号是否存在
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月24日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/findByPhone", method = RequestMethod.POST)
	@ResponseBody
	public Object findByPhone() {
		logBefore(logger, "患者端-查询手机号是否存在");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// 判断 当前登录人id
			if (IsNull.paramsIsNull(pd.get("phone"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData db = patientService.findByPhone(pd);
				if (IsNull.paramsIsNull(db) == false) {
					if (IsNull.paramsIsNull(db.get("password")) == false) {
						result = "1101";
						message = CommonMessage.CODE_1101;// 存在有密码，去登录！
					} else {
						result = "1100";
						message = CommonMessage.CODE_1100;// ;//存在无密码，去设置密码！
					}

				} else {// 用户不存在
					result = "1102";
					message = CommonMessage.CODE_1102;// ;//不存在，去注册！
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
	 * 7.描述：患者端 查询个人信息
	 * 
	 * @auther 霍学杰
	 * @date 2017年8月24日
	 * @version 1.0
	 * @return
	 */
	@RequestMapping(value = "/findpatient", method = RequestMethod.POST)
	@ResponseBody
	public Object findpatient() {
		logBefore(logger, "患者端 查询个人信息");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("patientId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				PageData patient = patientService.findPinfo(pd);
				if (patient != null) {
					map.put("patient", patient);
					result = "0000";
					message = "查询成功";
				} else {
					map.put("patient", new PageData());
					result = "1002";
					message = "信息不存在";
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
	 * 8.1 描述：完善个人信息，微信小程序与客户端一起用
	 * 
	 * @author 霍学杰
	 * @date 2017年8月24日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/editPatInfoXCX", method = RequestMethod.POST)
	@ResponseBody
	public Object editPatInfoXCX() {
		logBefore(logger, "修改个人资料");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("patientId"))) {
				result = "9993";
				message = "参数异常";
			} else {

				if (IsNull.paramsIsNull(pd.get("patientMedicalHistory")) == false) {
					boolean specialChar = EmojiUtil.isSpecialChar(pd.get("patientMedicalHistory").toString());
					if (specialChar) {
						result = "1003";
						message = "不能包含特殊字符";
						map.put("result", result);
						map.put("message", message);
						return AppUtil.returnObject(new PageData(), map);
					}
					// 郭立成修改（既往病历/过敏史超过255个字符 sql报错返回服务器异常问题）
					if (pd.get("patientMedicalHistory").toString().length() > 250) {
						result = "1003";
						message = "既往病历/过敏史不能超过250个字符！";
						map.put("result", result);
						map.put("message", message);
						return AppUtil.returnObject(new PageData(), map);
					}
					// 郭立成修改
				}

				if (IsNull.paramsIsNull(pd.get("birthTime")) == false) {
					String birthTime = pd.get("birthTime").toString();
					map.put("age", DateUtil.getDiffYear(birthTime, DateUtil.getTime()));
					pd.put("age", map.get("age"));
				} else {
					map.put("age", 0);
				}

				// 昵称
				String trueName = "";
				if (IsNull.paramsIsNull(pd.get("trueName")) == false) {
					trueName = pd.get("trueName").toString();

					patientService.editPatientInfo(pd);

				} else {
					patientService.editPatientInfo(pd);
				}

				// 添加统计
				pd.put("userId", pd.get("patientId"));
				perinforService.save(pd);

				// 性别 sex=1
				int sex = 1;
				if (IsNull.paramsIsNull(pd.get("sex")) == false) {
					sex = Integer.parseInt(pd.get("sex").toString());
				}

				// 头像
				String headUrl = "";
				if (IsNull.paramsIsNull(pd.get("headUrl")) == false) {
					headUrl = pd.get("headUrl").toString();
				}

				// 修改腾讯云个人资料
				AppTencentCloudController.setUserInfo(pd.get("patientId").toString(), trueName, sex, "", "", headUrl,
						cloudSignService);
				result = "0000";
				message = "修改成功";
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
	 * 10.1描述：查询诊所 方圆xx公里内的诊所列表 患者端、小程序 、web端都用
	 * 
	 * @author 霍学杰
	 * @date 2017年9月18日
	 * @version 1.0
	 * @return
	 */

	@RequestMapping(value = "/findZSlist2", method = RequestMethod.POST)
	@ResponseBody
	public Object findZSlist2() {
		logBefore(logger, "查询诊所 方圆xx公里内的诊所列表");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			// type 1小程序 2 安卓 3 iOS
			if (IsNull.paramsIsNull(pd.get("latitude")) || IsNull.paramsIsNull(pd.get("longitude"))
					|| IsNull.paramsIsNull(pd.get("type"))) {
				result = "9993";
				message = "参数异常";
			} else {
				if (CommonConfig.IsOnline) {// 在线上时查不出来知心诊所
					pd.put("isOnline", 1);
				}
				// ---------------------------------------------------------------------------------
				// 2018-03-14 修改，改为方圆多少公里数据库查询时，程序算出经纬度范围，再通过数据库调用索引查询，提高查询速度
				// 第一步：得到方圆多少公里的经纬度
				long startTime = System.currentTimeMillis();// 获取当前时间
				Map rMap = MapDistance.getAround(pd.get("longitude").toString(), pd.get("latitude").toString(), "3000");// 调用方法得到经纬度范围
				pd.putAll(rMap);
				List<PageData> list2 = new ArrayList<>();
				// 第二步：根据是否给了自己的经纬度，来查询得到诊所列表；给了自己的经纬度，计算的是自己距离诊所的距离，不给经纬度，计算的是查询点距离诊所的距离
				pd.put("locLatitude", pd.get("latitude"));
				pd.put("locLongitude", pd.get("longitude"));
				list2 = clinicService.findClinic4(pd);
				// 第三步：处理数据
				DecimalFormat df = new DecimalFormat("######0.0");
				DecimalFormat df1 = new DecimalFormat("######");
				List<PageData> list = new ArrayList<PageData>();
				if (list2 != null && list2.size() > 0) {
					int sizea = list2.size();
					if (sizea > 30) {
						sizea = 30;
					}
					// content:诊所名称
					for (int i = 0; i < sizea; i++) {
						PageData dd = list2.get(i);
						if (dd != null) {
							// state 状态 0.待审核 1.已通过 2.审核失败 3.待绑定4.标记
							// 距离最近的 把图标放大 2017.10.21
							if (i == 0) {
								dd.put("width", 40);
								dd.put("height", 56);
							}
							dd.put("id", i);
							// 距离
							Double distance1 = Double.parseDouble(dd.get("distance").toString());
							String distance = "";
							if (distance1 > 500) {
								distance = df.format(distance1 / 1000) + "km";
							} else {
								distance = df1.format(distance1) + "m";
							}

							dd.put("distance", distance);

							int flag = Integer.parseInt(dd.get("state").toString());
							if (flag == 1) {
								int more_time = 0;
								if (IsNull.paramsIsNull(dd.get("openHours")) == false) {
									String yytime = dd.get("openHours").toString();
									String[] str = yytime.split("-");
									more_time = DateUtil.more_time(str[0], str[1]);
								}
								if (more_time == 1) {
									dd.put("iconPath", "../../image/tb_press.png"); // 入驻成功
								} else if (more_time == 2) {
									dd.put("iconPath", "../../image/tb_uncheck.png");// 不在营业时间内
								} else if (more_time == 0) {
									dd.put("iconPath", "../../image/tb_press.png"); // 入驻成功
								}
							} else if (flag == 4) {
								int more_time = 0;
								if (IsNull.paramsIsNull(dd.get("openHours")) == false) {
									String yytime = dd.get("openHours").toString();
									String[] str = yytime.split("-");
									more_time = DateUtil.more_time(str[0], str[1]);
								}
								if (more_time == 1) {
									dd.put("iconPath", "../../image/tb_normal.png"); // 标记成功
								} else if (more_time == 2) {
									dd.put("iconPath", "../../image/tb_uncheck.png");// 不在营业时间内
								} else if (more_time == 0) {
									dd.put("iconPath", "../../image/tb_normal.png"); // 标记成功
								}
							} else {
								dd.put("iconPath", "../../image/tb_none.png");// 未入住成功
							}
							list.add(i, dd);
						}
					}
				}
				long endTime = System.currentTimeMillis();
				System.out.println("findZSlist2  程序运行时间：" + (endTime - startTime) + "ms");
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
					result = "0000";
					message = "成功";
				} else {
					result = "0000";
					map.put("list", new ArrayList<PageData>());
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
	 * 13描述：患者端 挂号页面 提交挂号信息v3
	 * 
	 * @author 霍学杰
	 * @date 2017年9月27日
	 * @version 1.1
	 * @return
	 */
	@RequestMapping(value = "/saveGH3", method = RequestMethod.POST)
	@ResponseBody
	public Object saveGH3() {
		logBefore(logger, "患者端-挂号");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("patientName"))
					|| IsNull.paramsIsNull(pd.get("patientSex")) || IsNull.paramsIsNull(pd.get("patientAge"))
					|| IsNull.paramsIsNull(pd.get("patientCondition"))
					|| IsNull.paramsIsNull(pd.get("patientMedicalHistory")) || IsNull.paramsIsNull(pd.get("patientId"))
					|| IsNull.paramsIsNull(pd.get("phone"))) {
				result = "9993";
				message = "参数异常";
			}

			if (PhoneCheck.isMobile(pd.get("phone").toString()) == false) {
				result = "1003";
				message = "手机号格式不正确";
			} else {
				// 查询诊所信息 得到name
				PageData findzsClinic = clinicService.findzsClinic(pd);
				pd.put("clinicName", findzsClinic.get("clinicName"));// 得到name

				try {
					String str = findzsClinic.get("photos").toString();

					String[] split = str.split(",");
					pd.put("photos", split[0]);
				} catch (Exception e1) {
				}

				PageData db = new PageData();
				db.put("patientId", pd.get("patientId"));
				PageData findPinfo = patientService.findPinfo(pd);
				// =============存储formid
				if (IsNull.paramsIsNull(pd.get("formId")) == false) {
					String formIdS[] = pd.get("formId").toString().split(",");
					for (int i = 0; i < formIdS.length; i++) {
						PageData dd = new PageData();
						try {
							dd.put("userId", findPinfo.get("patientId"));
							dd.put("wxopenId", findPinfo.get("wxopenId"));
							dd.put("formId", pd.get("formId"));
							dd.put("state", 0);
							dd.put("createTime", DateUtil.getTime());
							if (pd.get("formId").toString().length() < 15) {
								newPushService.save(pd);
							}
						} catch (Exception e) {
							System.out.println("openId为空=======");
						}
					}
				}
				// =============以上
				try {
					pd.put("headUrl", findPinfo.get("headUrl"));
				} catch (Exception e) {
					pd.put("headUrl", "");
				}
				db.put("sy", 1);
				db.put("clinicId", pd.get("clinicId"));

				// 查询初复诊
				pd.put("patientVisit", "0");// 初诊
				String findczfz = findczfz(pd);
				if ("复诊".equals(findczfz)) {
					pd.put("patientVisit", "1");// 复诊
				}

				pd.put("checkinTime", DateUtil.getTime());
				pd.put("checktime", DateUtil.getTime1());
				pd.put("checkinId", this.get32UUID());
				pd.put("state", 0);
				pd.put("time", DateUtil.getDay() + " 01:00:00");
				PageData num = clinic_checkinService.findGHnum(pd);
				int number = 0;
				if (num != null) {
					number = Integer.parseInt(num.get("number").toString());
				}
				pd.put("number", number + 1);// 当前号码加1

				clinic_checkinService.save(pd);// 添加挂号信息
				result = "0000";
				message = "成功";
				PageData findGH = clinic_checkinService.findGH(pd);// 查询挂号信息
				List<PageData> list = clinic_checkinService.findGHlist(pd);
				map.put("findGH", findGH);
				if (list != null && list.size() > 0) {
					map.put("size", list.size());// 排队人数
				} else {
					map.put("size", 0);
				}

				// 推送 挂号消息
				String sex = "";
				if ("0".equals(pd.get("patientSex").toString())) {
					sex = "女";
				} else {
					sex = "男";
				}
				String cfz = "";
				if ("0".equals(pd.get("patientVisit").toString())) {
					cfz = "初诊";
				} else {
					cfz = "复诊";
				}
				String messageConet = "患者：" + pd.getString("patientName") + " " + sex + " " + pd.get("patientAge") + " "
						+ cfz + " 挂号了！";
				pd.put("ss", "ss");
				List<PageData> findcylist = clinicUserService.findcylist(pd);
				PageData dd1 = new PageData();
				if (findcylist != null && findcylist.size() > 0) {
					for (int j = 0; j < findcylist.size(); j++) {
						dd1 = findcylist.get(j);//
						int authPlat = Integer.parseInt(dd1.get("authPlat").toString());
						if (authPlat == 1 || authPlat == 2 || authPlat == 6) {

							String phoneSole = "";
							String huaWeiToken = "";
							String miRegId = "";
							String mzPushId = "";
							phoneSole = dd1.getString("phoneSole");
							huaWeiToken = dd1.getString("huaWeiToken");
							miRegId = dd1.getString("miRegId");
							mzPushId = dd1.getString("mzPushId");

							Collection<String> alias = new ArrayList<>();
							Collection<String> registrationId = new ArrayList<>();
							registrationId.add(dd1.get("phoneSole").toString());
							String toUserId = dd1.get("doctorId").toString();
							alias.add(toUserId);
							String title = "挂号提醒";
							jPush.sendAll(alias, registrationId, title, "16", messageConet, "", "0", toUserId, "1",
									DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
									mzPushId, phoneSole);
						}
					}
				}

				// 更新诊所挂号总数
				PageData dd = new PageData();
				dd.put("year", DateUtil.getYear());
				dd.put("month", DateUtil.getMonth());
				dd.put("clinicId", pd.get("clinicId"));
				PageData findClinicsy2 = monthCountService.findClinicsy2(dd);// 诊所
				if (findClinicsy2 != null) {
					dd.put("cmonthId", findClinicsy2.get("cmonthId"));
					dd.put("ghNum", Integer.parseInt(findClinicsy2.get("ghNum").toString()) + 1);
					monthCountService.updateCount2(dd);
				} else {
					// 为空 新增月数据 把上个月数据带过来
					PageData findClinicyear = monthCountService.findClinicyear(dd);
					if (findClinicyear != null) {
						int ghNum = Integer.parseInt(findClinicyear.get("ghNum").toString()) + 1;
						dd.put("fuYearCount", findClinicyear.get("fuYearCount"));
						dd.put("chuYearCount", findClinicyear.get("chuYearCount"));
						dd.put("sumYearCount", findClinicyear.get("sumYearCount"));
						dd.put("inMoney", findClinicyear.get("inMoney"));
						dd.put("inMoneyCh", findClinicyear.get("inMoneyCh"));
						dd.put("inMoneyWe", findClinicyear.get("inMoneyWe"));
						dd.put("ghNum", ghNum);
						dd.put("cmonthId", this.get32UUID());
						monthCountService.save2(dd);
					} else {
						dd.put("cmonthId", this.get32UUID());
						monthCountService.save2(dd);
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
	 * 18.描述：小程序 判断当前能否挂号
	 * 
	 * 
	 * 是否可以预约挂号 流程
	 * 
	 * @author 霍学杰
	 * @date 2017年10月4日
	 * @version 1.2
	 * @return
	 */
	@RequestMapping(value = "/getGHstate", method = RequestMethod.POST)
	@ResponseBody
	public Object getGHstate() {
		logBefore(logger, "小程序 判断当前能否挂号");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		String resultYu = "10000";
		try {
			if (IsNull.paramsIsNull(pd.get("patientId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("checkinTime", DateUtil.getDay() + " 01:00:00");

				pd.put("sy", 1);
				List<PageData> list = clinic_checkinService.getGHstate(pd);
				if (list != null && list.size() > 0) {
					result = "1001";
					message = "当前挂号未完成";
				} else {
					pd.put("sy", 2);
					List<PageData> list1 = clinic_checkinService.getGHstate(pd);
					if (list1 != null && list1.size() > 1) {
						result = "1002";
						message = "您今天已经取消两次挂号了";
					} else {
						pd.put("sy", 3);
						List<PageData> list2 = clinic_checkinService.getGHstate(pd);
						if (list2 != null && list2.size() > 2) {
							result = "1003";
							message = "您今天已经两次挂号没去了";
						} else {
							result = "0000";
							message = "可以挂号";
						}
					}
				}

				// 查询预约的状态
				pd.put("isYu", "true");
				PageData data = clinic_checkinService.getGHstateYu(pd);
				if (IsNull.paramsIsNull(data) == false) {
					resultYu = "3204";
				} else {
					resultYu = "0000";
				}
			}
		} catch (Exception e) {
			result = "9999";
			message = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("resultYu", resultYu);
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	// ===========================新版接口========================================================

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
	public Object editRegistId() {
		logBefore(logger, "更新推送注册ID");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId"))) {
				result = "9993";
				message = "参数异常";
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
				patientService.editPushId(pd);
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

	// 预约挂号
	/**
	 * 13描述：患者端预约挂号 患者挂号第一次只推送无消息，患者预约挂号第一次只推送无消息
	 * 
	 * @author 董雪蕊
	 * @date 2017年12月19日
	 * @version 1.1
	 * @return
	 */
	@RequestMapping(value = "/yuSave", method = RequestMethod.POST)
	@ResponseBody
	public Object yuSave() {
		logBefore(logger, "患者端预约挂号");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("clinicId")) || IsNull.paramsIsNull(pd.get("patientName"))
					|| IsNull.paramsIsNull(pd.get("patientSex")) || IsNull.paramsIsNull(pd.get("patientAge"))
					|| IsNull.paramsIsNull(pd.get("patientCondition")) || IsNull.paramsIsNull(pd.get("patientId"))
					|| IsNull.paramsIsNull(pd.get("isYu")) || IsNull.paramsIsNull(pd.get("yuTime"))) {
				result = "9993";
				message = "参数异常";
			} else if (pd.get("isYu").toString().equals("true") && IsNull.paramsIsNull(pd.get("yuTime"))) {
				result = "9993";
				message = "参数异常";
			}

			if (PhoneCheck.isMobile(pd.get("phone").toString()) == false) {
				result = "1003";
				message = "手机号格式不正确";
			} else {
				// 处理不能预约的逻辑
				Boolean canYu = true;
				// a.完成预约前不能预约 b.如果今天到时间没有来，系统取消了，不能预约
				PageData YuData = clinic_checkinService.findHaveYu(pd);// 1.先查询是否有未处理的预约
				if (IsNull.paramsIsNull(YuData) == false && IsNull.paramsIsNull(YuData.get("yuTime")) == false) {
					int yuState = Integer.parseInt(YuData.get("yuState").toString());
					if (yuState == 1) {// a
						canYu = false;
						result = "3200";
						message = CommonMessage.CODE_3200;
					} else if (yuState == 6 && DateUtil.compareEqueal(YuData.get("checkinTime").toString(),
							DateUtil.getTime(), "yyyy-MM-dd")) {
						result = "3201";
						message = CommonMessage.CODE_3201;
						canYu = false;
					}
				}
				// 去预约
				if (canYu) {
					// 查询诊所信息 得到name
					PageData findzsClinic = clinicService.findzsClinic(pd);
					pd.put("clinicName", findzsClinic.get("clinicName"));// 得到name
					// 得到图片
					try {
						String str = findzsClinic.get("photos").toString();
						String[] split = str.split(",");
						pd.put("photos", split[0]);
					} catch (Exception e1) {
						pd.put("photos", "http://jk.120ksb.com:8003/pic/icon_zs.png");
					}
					PageData db = new PageData();
					db.put("patientId", pd.get("patientId"));
					PageData findPinfo = patientService.findPinfo(pd);
					try {
						pd.put("headUrl", findPinfo.get("headUrl"));
					} catch (Exception e) {
						pd.put("headUrl", "");
					}
					db.put("sy", 1);
					db.put("clinicId", pd.get("clinicId"));
					// 查询初复诊
					pd.put("patientVisit", "0");// 初诊
					String findczfz = findczfz(pd);
					if ("复诊".equals(findczfz)) {
						pd.put("patientVisit", "1");// 复诊
					}
					pd.put("checkinTime", pd.get("yuTime"));
					pd.put("checktime", pd.get("yuTime").toString().substring(11));
					pd.put("yuTime", DateUtil.getTime());
					pd.put("checkinId", this.get32UUID());
					pd.put("state", 0);
					pd.put("number", -1);
					pd.put("time", DateUtil.getDay() + " 01:00:00");
					pd.put("yuState", 1);
					pd.put("weight", "0");
					clinic_checkinService.save(pd);// 添加挂号信息
					result = "0000";
					message = "成功";
					// =============存储formid
					if (IsNull.paramsIsNull(pd.get("formId")) == false) {
						String formIdS[] = pd.get("formId").toString().split(",");
						for (int i = 0; i < formIdS.length; i++) {
							PageData dd = new PageData();
							try {
								dd.put("userId", findPinfo.get("patientId"));
								dd.put("wxopenId", findPinfo.get("wxopenId"));
								dd.put("formId", pd.get("formId"));
								dd.put("state", 0);
								dd.put("createTime", DateUtil.getTime());
								if (pd.get("formId").toString().length() < 15) {
									newPushService.save(pd);
								}
							} catch (Exception e) {
								System.out.println("openId为空=======");
							}
						}
					}
					// =============以上
					// ==================推送 挂号消息
					String sex = "";
					if ("0".equals(pd.get("patientSex").toString())) {
						sex = "女";
					} else {
						sex = "男";
					}
					String cfz = "";
					if ("0".equals(pd.get("patientVisit").toString())) {
						cfz = "初诊";
					} else {
						cfz = "复诊";
					}
					String messageConet = "患者：" + pd.getString("patientName") + " " + sex + " " + pd.get("patientAge")
							+ " " + cfz + " 预约挂号了，请您及时处理！";
					pd.put("ss", "ss");
					List<PageData> findcylist = clinicUserService.findcylist(pd);
					PageData dd1 = new PageData();
					if (findcylist != null && findcylist.size() > 0) {
						for (int j = 0; j < findcylist.size(); j++) {
							dd1 = findcylist.get(j);//
							int authPlat = Integer.parseInt(dd1.get("authPlat").toString());
							if (authPlat == 1 || authPlat == 2 || authPlat == 6) {

								String phoneSole = "";
								String huaWeiToken = "";
								String miRegId = "";
								String mzPushId = "";
								phoneSole = dd1.getString("phoneSole");
								huaWeiToken = dd1.getString("huaWeiToken");
								miRegId = dd1.getString("miRegId");
								mzPushId = dd1.getString("mzPushId");

								Collection<String> alias = new ArrayList<>();
								Collection<String> registrationId = new ArrayList<>();
								registrationId.add(dd1.get("phoneSole").toString());
								String toUserId = dd1.get("doctorId").toString();
								alias.add(toUserId);
								String title = "预约挂号提醒";
								jPush.sendAll(alias, registrationId, title, "26", messageConet, "", "0", toUserId, "1",
										DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
										mzPushId, phoneSole);

								// 插入预约消息
								PageData news = new PageData();
								String doctorId = dd1.get("doctorId").toString();
								news.put("title", title);
								news.put("type", 26);
								news.put("flag", 0);// 未推送
								news.put("messageContent", messageConet);
								news.put("fromUserId", "");
								news.put("fromRoleFlag", "0");
								news.put("toRoleFlag", "1");
								news.put("creatDate", DateUtil.getDay());
								news.put("creatTime", DateUtil.getTime());
								news.put("sysdrugName", "");
								news.put("skitId", pd.get("checkinId"));
								news.put("headUrlNew", "");
								news.put("huoDongUrl", "");
								news.put("toUserId", doctorId);
								news.put("pushTime",
										DateUtil.getFauTime(CommonConfig.TIME20, news.get("creatTime").toString()));
								newsService.save(news);
								news.put("pushTime",
										DateUtil.getFauTime(CommonConfig.TIME40, news.get("creatTime").toString()));
								newsService.save(news);
								// 系统处理未接的，只给医生
								news.put("title", "预约挂号失败通知");
								news.put("messageContent",
										"由于" + pd.getString("patientName") + "患者的预约挂号在一小时内未能及时处理，系统已帮您自动拒绝本次预约挂号。");
								news.put("type", 33);
								news.put("pushTime",
										DateUtil.getFauTime(CommonConfig.TIME60, news.get("creatTime").toString()));
								newsService.save(news);
								if (j == 0) {
									// 系统处理未接的，给患者
									news.put("title", "预约挂号失败通知");
									news.put("type", 32);
									news.put("flag", 0);// 未推送
									news.put("messageContent",
											"由于" + pd.getString("clinicName") + "诊所对您的预约挂号在一小时内未能及时作出回应，系统取消了本次预约挂号");
									news.put("fromUserId", "");
									news.put("fromRoleFlag", "0");
									news.put("toRoleFlag", "2");
									news.put("creatDate", DateUtil.getDay());
									news.put("creatTime", DateUtil.getTime());
									news.put("sysdrugName", "");
									news.put("skitId", pd.get("checkinId"));
									news.put("headUrlNew", "");
									news.put("huoDongUrl", "");
									news.put("toUserId", pd.get("patientId"));
									news.put("pushTime",
											DateUtil.getFauTime(CommonConfig.TIME60, news.get("creatTime").toString()));
									newsService.save(news);
								}
							}
						}
					}
					// 更新诊所挂号总数
					PageData dd = new PageData();
					dd.put("year", DateUtil.getYear());
					dd.put("month", DateUtil.getMonth());
					dd.put("clinicId", pd.get("clinicId"));
					PageData findClinicsy2 = monthCountService.findClinicsy2(dd);// 诊所
					if (findClinicsy2 != null) {
						dd.put("cmonthId", findClinicsy2.get("cmonthId"));
						dd.put("ghNum", Integer.parseInt(findClinicsy2.get("ghNum").toString()) + 1);
						monthCountService.updateCount2(dd);
					} else {
						// 为空 新增月数据 把上个月数据带过来
						PageData findClinicyear = monthCountService.findClinicyear(dd);
						if (findClinicyear != null) {
							int ghNum = Integer.parseInt(findClinicyear.get("ghNum").toString()) + 1;
							dd.put("fuYearCount", findClinicyear.get("fuYearCount"));
							dd.put("chuYearCount", findClinicyear.get("chuYearCount"));
							dd.put("sumYearCount", findClinicyear.get("sumYearCount"));
							dd.put("inMoney", findClinicyear.get("inMoney"));
							dd.put("inMoneyCh", findClinicyear.get("inMoneyCh"));
							dd.put("inMoneyWe", findClinicyear.get("inMoneyWe"));
							dd.put("ghNum", ghNum);
							dd.put("cmonthId", this.get32UUID());
							monthCountService.save2(dd);// 添加诊所月统计记录
						} else {
							dd.put("cmonthId", this.get32UUID());
							monthCountService.save2(dd);// 添加诊所月统计记录
						}
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
	 * 
	 * @author 董雪蕊
	 * @description 患者取消预约
	 * @date 2017年12月19日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@SuppressWarnings("unused")
	@RequestMapping(value = "/cancelYu")
	@ResponseBody
	public Object cancelYu() {
		logBefore(logger, "患者取消预约");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("patientId")) || IsNull.paramsIsNull(pd.get("checkinId"))) {
				result = "9993";
				message = "参数异常";
			} else {
				// 根据id查询挂号详情
				PageData checkinData = clinic_checkinService.findByidxq(pd);
				if (IsNull.paramsIsNull(checkinData) == false
						&& IsNull.paramsIsNull(checkinData.get("yuState")) == false
						&& IsNull.paramsIsNull(checkinData.get("yuTime")) == false) {
					int yuState = Integer.parseInt(checkinData.get("yuState").toString());
					String yuTime = checkinData.get("checkinTime").toString();
					long xx = DateUtil.getMinSub(DateUtil.getTime(), yuTime);
					if (DateUtil.getMinSub(DateUtil.getTime(), yuTime) > 30) {// 当现在时间大于接诊时间30分钟时，可以取消预约
						if("1".equals(checkinData.get("state").toString())) {
							result = "1002";
							message = "医生已接诊，取消失败！";
						}else {
							pd.put("state", 3);
							pd.put("yuState", 2);
							clinic_checkinService.updatestate(pd);
							// 消息表删除未推送的
							pd.put("skitId", pd.get("checkinId"));
							newsService.delNoPush2(pd);
							if (IsNull.paramsIsNull(checkinData.get("doctorId")) == false) {
								// 插入预约消息
								PageData news = new PageData();
								String doctorId = checkinData.get("doctorId").toString();
								String patientName = "";
								if (IsNull.paramsIsNull(checkinData.get("patientName")) == false) {
									patientName = checkinData.getString("patientName");
								}
								news.put("title", "预约取消通知");
								news.put("type", 40);
								news.put("flag", 1);// 未推送
								news.put("messageContent",
										patientName + "取消了" + checkinData.get("checkinTime").toString() + "的挂号");
								news.put("fromUserId", "");
								news.put("fromRoleFlag", "0");
								news.put("toRoleFlag", "1");
								news.put("creatDate", DateUtil.getDay());
								news.put("creatTime", DateUtil.getTime());
								news.put("sysdrugName", "");
								news.put("skitId", checkinData.get("checkinId"));
								news.put("headUrlNew", "");
								news.put("huoDongUrl", "");
								news.put("toUserId", doctorId);
								newsService.save(news);
								PageData docData = doctorService.findById(checkinData);// 查询医生信息
								// 推送
								if (IsNull.paramsIsNull(docData) == false
										&& IsNull.paramsIsNull(docData.get("phoneSole")) == false) {
									String phoneSole = "";
									String huaWeiToken = "";
									String miRegId = "";
									String mzPushId = "";
									phoneSole = docData.getString("phoneSole");
									huaWeiToken = docData.getString("huaWeiToken");
									miRegId = docData.getString("miRegId");
									mzPushId = docData.getString("mzPushId");

									Collection<String> alias = new ArrayList<>();
									Collection<String> registrationId = new ArrayList<>();
									alias.add(doctorId);
									registrationId.add(docData.get("phoneSole").toString());
									jPush.sendAll(alias, registrationId, "预约取消通知", "40", news.getString("messageContent"),
											"", "0", news.getString("toUserId"), "1", DateUtil.getDay(), DateUtil.getTime(),
											"", "", "", "", huaWeiToken, miRegId, mzPushId, phoneSole);
								}
							} else {
								// 查询成员列表
								List<PageData> findcylist = clinicUserService.findcylist(checkinData);
								PageData dd1 = new PageData();
								if (findcylist != null && findcylist.size() > 0) {
									for (int j = 0; j < findcylist.size(); j++) {
										dd1 = findcylist.get(j);//
										// 插入预约消息
										PageData news = new PageData();
										String doctorId = dd1.get("doctorId").toString();
										String patientName = "";
										if (IsNull.paramsIsNull(checkinData.get("patientName")) == false) {
											patientName = checkinData.getString("patientName");
										}
										news.put("title", "预约取消通知");
										news.put("type", 40);
										news.put("flag", 1);// 未推送
										news.put("messageContent",
												patientName + "取消了" + checkinData.get("checkinTime").toString() + "的挂号");
										news.put("fromUserId", "");
										news.put("fromRoleFlag", "0");
										news.put("toRoleFlag", "1");
										news.put("creatDate", DateUtil.getDay());
										news.put("creatTime", DateUtil.getTime());
										news.put("sysdrugName", "");
										news.put("skitId", checkinData.get("checkinId"));
										news.put("headUrlNew", "");
										news.put("huoDongUrl", "");
										news.put("toUserId", doctorId);
										newsService.save(news);
										if (IsNull.paramsIsNull(dd1.get("phoneSole")) == false) {

											String phoneSole = "";
											String huaWeiToken = "";
											String miRegId = "";
											String mzPushId = "";
											phoneSole = dd1.getString("phoneSole");
											huaWeiToken = dd1.getString("huaWeiToken");
											miRegId = dd1.getString("miRegId");
											mzPushId = dd1.getString("mzPushId");

											Collection<String> alias = new ArrayList<>();
											Collection<String> registrationId = new ArrayList<>();
											alias.add(doctorId);
											registrationId.add(dd1.get("phoneSole").toString());
											jPush.sendAll(alias, registrationId, "预约取消通知", "40",
													news.getString("messageContent"), "", "0", news.getString("toUserId"),
													"1", DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken,
													miRegId, mzPushId, phoneSole);
										}
									}
								}
							}

							result = "0000";
							message = "取消预约成功！";
						}
					} else {
						result = "3203";
						message = CommonMessage.CODE_3203;
					}
				} else {
					result = "3202";
					message = CommonMessage.CODE_3202;// 该预约信息不存在！
				}

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
	 * ↓↓↓↓↓↓↓↓↓↓****************************接口 王立飞
	 * 2018-4-4*****************↓↓↓↓↓↓↓↓↓↓↓↓↓↓
	 */
	/**
	 * 描述：搜索默认查询医生 方圆xx公里内的医生列表
	 * 
	 * @author 王立飞
	 * @date 2018年4月4日
	 * @version 1.0
	 * @return
	 */
	@RequestMapping(value = "/findZSlist3", method = RequestMethod.POST)
	@ResponseBody
	public Object findZSlist3() {
		logBefore(logger, "搜索默认查询医生 方圆xx公里内的医生列表");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {// locLatitude locLongitude
				// type 1小程序 2 安卓 3iOS
			if (IsNull.paramsIsNull(pd.get("latitude")) || IsNull.paramsIsNull(pd.get("longitude"))
					|| IsNull.paramsIsNull(pd.get("type"))) {
				result = "9993";
				message = "参数异常";
			} else {
				// ---------------------------------------------------------------------------------
				// 方圆多少公里数据库查询时，程序算出经纬度范围，再通过数据库调用索引查询，提高查询速度
				// 第一步：得到方圆多少公里的经纬度
				Map rMap = MapDistance.getAround(pd.get("longitude").toString(), pd.get("latitude").toString(), "3000");// 调用方法得到经纬度范围
				pd.putAll(rMap);
				// 第二步：根据是否给了自己的经纬度，来查询得到医生列表；给了自己的经纬度，计算的是自己距离医生的距离，不给经纬度，计算的是查询点距离诊所的距离
				if (IsNull.paramsIsNull(pd.get("locLatitude")) || IsNull.paramsIsNull(pd.get("locLongitude"))) {
					pd.put("locLatitude", pd.get("latitude"));
					pd.put("locLongitude", pd.get("longitude"));
				}
				if (CommonConfig.IsOnline) {// 在线上时查不出来知心诊所
					pd.put("isOnline", 1);
				}
				List<PageData> list2 = doctorService.findDoctorXX(pd);
				// 第三步：处理数据
				DecimalFormat df = new DecimalFormat("######0.0");
				DecimalFormat df1 = new DecimalFormat("######");
				List<PageData> list = new ArrayList<PageData>();
				if (list2 != null && list2.size() > 0) {

					int sizea = list2.size();
					if (sizea > 30) {
						sizea = 30;
					}

					for (int i = 0; i < sizea; i++) {
						PageData dd = list2.get(i);
						if (dd != null) {
							dd.put("id", i);
							// 距离
							Double distance1 = Double.parseDouble(dd.get("distance").toString());
							String distance = "";
							if (distance1 > 500) {
								distance = df.format(distance1 / 1000) + "km";
							} else {
								distance = df1.format(distance1) + "m";
							}
							dd.put("distance", distance);
							list.add(i, dd);
						}
					}

				}
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
					result = "0000";
					message = "成功";
				} else {
					result = "0000";
					map.put("list", new ArrayList<PageData>());
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
	 * 搜索页面 模糊搜索擅长疾病 医生
	 * 
	 * @auther 王立飞
	 * @date 2018年4月4日
	 * @version 1.01
	 * @return
	 */
	@RequestMapping(value = "/findDoclist", method = RequestMethod.POST)
	@ResponseBody
	public Object findDoclist() {
		logBefore(logger, "搜索页面 模糊搜索擅长疾病 医生");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		long startTime = System.currentTimeMillis();// 获取当前时间
		try {
			if (IsNull.paramsIsNull(pd.get("latitude")) || IsNull.paramsIsNull(pd.get("longitude"))
					|| IsNull.paramsIsNull(pd.get("shanDisease"))) {
				result = "9993";
				message = "参数异常";
			} else if (pd.get("latitude").toString().equals("0") || pd.get("longitude").toString().equals("0")) {
				result = "5001";
				message = CommonMessage.CODE_5001;// 定位失败
			} else {
				// 方圆多少公里数据库查询时，程序算出经纬度范围，再通过数据库调用索引查询，提高查询速度
				// 第一步：得到方圆多少公里的经纬度
				Map rMap = MapDistance.getAround(pd.get("longitude").toString(), pd.get("latitude").toString(),
						"300000");// 调用方法得到经纬度范围
				pd.putAll(rMap);
				// 第二步：根据是否给了自己的经纬度，来查询得到医生列表；给了自己的经纬度，计算的是自己距离医生的距离，不给经纬度，计算的是查询点距离诊所的距离
				if (IsNull.paramsIsNull(pd.get("locLatitude")) || IsNull.paramsIsNull(pd.get("locLongitude"))) {
					pd.put("locLatitude", pd.get("latitude"));
					pd.put("locLongitude", pd.get("longitude"));
				}
				if (CommonConfig.IsOnline) {// 在线上时查不出来知心诊所
					pd.put("isOnline", 1);
				}
				List<PageData> list2 = doctorService.findDoclist(pd);

				DecimalFormat df = new DecimalFormat("######0.00");
				DecimalFormat df1 = new DecimalFormat("######");
				List<PageData> list = new ArrayList<PageData>();
				if (list2 != null && list2.size() > 0) {
					int sizea = list2.size();
					if (sizea > 40) {
						sizea = 40;
					}
					for (int i = 0; i < sizea; i++) {
						PageData dd = list2.get(i);
						if (dd != null) {// state 状态 0.待审核 1.已通过 2.审核失败 3.待绑定
							dd.put("id", i);
							// 距离
							Double distance1 = 0.0;
							String distance = "";
							if (IsNull.paramsIsNull(dd.get("distance")) == false) {
								distance1 = Double.parseDouble(dd.get("distance").toString());
								if (distance1 > 500) {
									distance = df.format(distance1 / 1000) + "km";
								} else {
									distance = df1.format(distance1) + "m";
								}
							} else {
								distance = "暂无定位";
							}
							dd.put("distance", distance);
							list.add(i, dd);
						}
					}
				}
				if (list != null && list.size() > 0) {
					map.put("list", list);
					map.put("size", list.size());
				} else {
					map.put("list", new ArrayList<PageData>());
					map.put("size", 0);
				}
				result = "0000";
				message = "成功";
				// 统计==============================================
				if (IsNull.paramsIsNull(pd.get("patientId")) == false) {
					pd.put("dayDate", DateUtil.getDay());
					pd.put("userId", pd.get("patientId"));
					pd.put("fType", "2");
					pd.put("fState", "首页搜索医生");
					PageData data = functionService.getDayUser(pd);
					if (data == null) {
						// 不存在,添加
						pd.put("fNum", "1");
						pd.put("updateTime", new Date());
						functionService.save(pd);
					} else {
						// 存在,更新数据
						int fNum = Integer.parseInt(data.get("fNum").toString());
						pd.put("fNum", fNum + 1);
						pd.put("fid", data.get("fid"));
						pd.put("updateTime", new Date());
						functionService.updateDayACt(pd);
					}
				}
				// ==============================================

			}
		} catch (Exception e) {
			result = "9999";
			message = "服务器异常";
			e.printStackTrace();
		} finally {
			long endTime = System.currentTimeMillis();
			System.out.println("程序运行时间：" + (endTime - startTime) + "ms");
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 获取微信信息 小程序 2018年05月04日
	 * 
	 * @author 霍学杰
	 * @date 2018年05月04日
	 * @version 1.0
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/getUserInfo")
	@ResponseBody
	public Object getUserInfo() {
		logBefore(logger, "获取微信信息 小程序");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			if (IsNull.paramsIsNull(pd.get("code")) || IsNull.paramsIsNull(pd.get("encrypted"))
					|| IsNull.paramsIsNull(pd.get("iv"))) {
				result = "9993";
				message = "参数异常";
			} else {
				pd.put("IP", getRemortIP());
				String[] openId_toke = AES.OpenId_toke(pd.get("code").toString());
				if (openId_toke != null && openId_toke.length > 0) {
					String wxDecrypt = AES.wxDecrypt(pd.get("encrypted").toString(), openId_toke[1],
							pd.get("iv").toString());
					JSONObject jsonObj = JSONObject.fromObject(wxDecrypt);
					pd.put("wxopenId", openId_toke[0]);
					pd.put("unionid", jsonObj.getString("unionId"));
					if (IsNull.paramsIsNull(pd.get("userInfo"))) {
						pd.put("phone", jsonObj.get("phoneNumber").toString());
					} else {
						if ("+++".equals(pd.get("userInfo").toString())) {
							pd.put("trueName", jsonObj.get("nickName").toString());
						} else {
							pd.put("trueName", pd.get("userInfo"));
						}

						pd.put("sex", jsonObj.get("gender").toString());
						pd.put("headUrl", jsonObj.get("avatarUrl").toString());
					}

					PageData loginThree = patientService.wxLogin(pd);

					if (loginThree != null) {
						pd.put("patientId", loginThree.get("patientId"));
						
						pd.put("userId", loginThree.get("patientId"));
						String avatarUrl=jsonObj.get("avatarUrl").toString();
						String trueName=pd.get("trueName").toString();
						List<PageData> list = tjPerinforService.findByIdList(pd);
						if(list != null && list.size() > 0) {
							for (PageData pageData : list) {
								if(IsNull.paramsIsNull(pageData.get("pname"))==false) {
									pd.remove("trueName");
									trueName=loginThree.get("trueName").toString();
									break;
								}
							}
						}
						String headUrl=loginThree.get("headUrl").toString();
						int startNum=headUrl.length();
						headUrl=headUrl.replaceAll("http://jk.120ksb.com:8003/pic/picResource/zxys_sys/", "");
						headUrl=headUrl.replaceAll("https://ws.120ksb.com/picture/api/", "");
						if(startNum > headUrl.length()) {
							pd.remove("headUrl");
							avatarUrl=loginThree.get("headUrl").toString();
						}
						
						patientService.editPatientInfo(pd);
						AppTencentCloudController.setUserInfo(loginThree.get("patientId").toString(),
								trueName, Integer.parseInt(loginThree.get("sex").toString()), "",
								"", avatarUrl, cloudSignService);
					} else {
						loginThree = patientService.LoginThree(pd);
						if (loginThree != null) {
							pd.put("patientId", loginThree.get("patientId"));
							
							pd.put("userId", loginThree.get("patientId"));
							String avatarUrl=jsonObj.get("avatarUrl").toString();
							String trueName=pd.get("trueName").toString();
							List<PageData> list = tjPerinforService.findByIdList(pd);
							if(list != null && list.size() > 0) {
								for (PageData pageData : list) {
									if(IsNull.paramsIsNull(pageData.get("pname"))==false) {
										pd.remove("trueName");
										trueName=loginThree.get("trueName").toString();
										break;
									}
								}
							}
							String headUrl=loginThree.get("headUrl").toString();
							int startNum=headUrl.length();
							headUrl=headUrl.replaceAll("http://jk.120ksb.com:8003/pic/picResource/zxys_sys/", "");
							headUrl=headUrl.replaceAll("https://ws.120ksb.com/picture/api/", "");
							if(startNum > headUrl.length()) {
								pd.remove("headUrl");
								avatarUrl=loginThree.get("headUrl").toString();
							}
							
							patientService.editPatientInfo(pd);
							AppTencentCloudController.setUserInfo(loginThree.get("patientId").toString(),
									trueName, Integer.parseInt(loginThree.get("sex").toString()),
									"", "",avatarUrl, cloudSignService);
						} else {
							loginThree = patientService.findByPhone(pd);
							if (loginThree != null) {
								pd.put("patientId", loginThree.get("patientId"));

								pd.put("userId", loginThree.get("patientId"));
								String avatarUrl=jsonObj.get("avatarUrl").toString();
								String trueName=pd.get("trueName").toString();
								List<PageData> list = tjPerinforService.findByIdList(pd);
								if(list != null && list.size() > 0) {
									for (PageData pageData : list) {
										if(IsNull.paramsIsNull(pageData.get("pname"))==false) {
											pd.remove("trueName");
											trueName=loginThree.get("trueName").toString();
											break;
										}
									}
								}
								String headUrl=loginThree.get("headUrl").toString();
								int startNum=headUrl.length();
								headUrl=headUrl.replaceAll("http://jk.120ksb.com:8003/pic/picResource/zxys_sys/", "");
								headUrl=headUrl.replaceAll("https://ws.120ksb.com/picture/api/", "");
								if(startNum > headUrl.length()) {
									pd.remove("headUrl");
									avatarUrl=loginThree.get("headUrl").toString();
								}
								patientService.editPatientInfo(pd);

								if (IsNull.paramsIsNull(pd.get("trueName")) == false) {
									AppTencentCloudController.setUserInfo(loginThree.get("patientId").toString(),
											trueName,
											Integer.parseInt(loginThree.get("sex").toString()), "", "",
											avatarUrl, cloudSignService);
								}
							} else {
								if (IsNull.paramsIsNull(pd.get("patientId")) == false) {
									pd.put("createTime", DateUtil.getTime());
									patientService.editWXNum(pd);// 绑定微信

									// 添加小程序引流统计
									if (IsNull.paramsIsNull(pd.get("doctorId")) == false) {
										appletService.save(pd);
									}

									loginThree = patientService.findPinfo(pd);
									// 添加时段统计
									PatientLoginController.patSAveHours(pd, activeHoursService);
									// 这里要去腾讯云那注册下，注册下！

									if ("+++".equals(pd.get("userInfo").toString())) {
										AppTencentCloudController.OneImport(loginThree.getString("patientId"),
												jsonObj.get("nickName").toString(), jsonObj.get("avatarUrl").toString(),
												cloudSignService);
									} else {
										pd.put("trueName", pd.get("userInfo"));
										AppTencentCloudController.OneImport(loginThree.getString("patientId"),
												pd.get("userInfo").toString(), jsonObj.get("avatarUrl").toString(),
												cloudSignService);
									}
								}
							}
						}
					}
					loginThree = patientService.findById(loginThree);
					loginThree.put("trueName", pd.get("trueName"));
					pd.put("trueName", loginThree.get("patientId"));
					map.put("db", loginThree);
					result = "0000"; // success
					message = "登录成功";
					// 最后这里我要返回腾讯云的sign，登录就返回
					String signCloud = "";
					if (IsNull.paramsIsNull(pd.get("IMlive")) == false && "2".equals(pd.get("IMlive").toString())) {
						signCloud = AppTencentCloudController.getUserSign(loginThree.getString("patientId"));
					} else if (IsNull.paramsIsNull(pd.get("IMlive")) == false
							&& "3".equals(pd.get("IMlive").toString())) {
						signCloud = AppTencentCloudController.getUserSign_h(loginThree.getString("patientId"));
					}
					map.put("signCloud", signCloud);

				} else {
					result = "1003";
					message = "code失效";
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
	 * 患者上线通知推送医生
	 * 
	 * @author 王立飞
	 * @date 2018年5月14日 下午2:59:40
	 */
	@RequestMapping(value = "/onlineNotification", method = RequestMethod.POST)
	@ResponseBody
	public Object tongzhi() {
		logBefore(logger, "患者上线通知推送医生");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "已通知";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("patientId"))
					|| IsNull.paramsIsNull(pd.get("EntryType"))) {
				result = "9993";
				message = "参数异常";
			} else {

				if ("1".equals(pd.get("EntryType").toString())) {
					pd.put("countNum", 1);
				}

				String[] region = DituUtil.getprovince(getRemortIP());
				pd.put("region", region[1]);
				pd.put("province", region[0]);
				pd.put("dayDate", DateUtil.getDay());
				PageData pageData = onlineCountService.findByPd(pd);
				if (IsNull.paramsIsNull(pageData)) {
					// 未推送过
					PageData doctorInfo = doctorService.findById(pd);
					PageData patientInfo = patientService.findPinfo(pd);
					String trueName = "";
					if (IsNull.paramsIsNull(patientInfo) == false) {
						trueName = patientInfo.get("trueName").toString();
					}
					// 保存
					onlineCountService.save(pd);

					// 推送消息
					String messageConet = "您的患者" + trueName + "已上线,请您及时沟通!";

					Collection<String> alias = new ArrayList<>();
					Collection<String> registrationId = new ArrayList<>();
					if (IsNull.paramsIsNull(doctorInfo) == false) {
						String phoneSole = "";
						String huaWeiToken = "";
						String miRegId = "";
						String mzPushId = "";
						phoneSole = doctorInfo.getString("phoneSole");
						huaWeiToken = doctorInfo.getString("huaWeiToken");
						miRegId = doctorInfo.getString("miRegId");
						mzPushId = doctorInfo.getString("mzPushId");
						registrationId.add(doctorInfo.get("phoneSole").toString());
						String toUserId = doctorInfo.get("doctorId").toString();
						alias.add(toUserId);
						String title = "患者上线提醒";
						jPush.sendAll(alias, registrationId, title, "51", messageConet, pd.get("patientId").toString(),
								"0", toUserId, "1", DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken,
								miRegId, mzPushId, phoneSole);
					}
				} else {

					pd.put("id", pageData.get("id"));
					onlineCountService.updateNum(pd);
				}

				result = "0000";
				message = "通知成功";
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
	 * 描述:异步查询 初诊复诊
	 * 
	 * @author 王立飞
	 * @date 2018年7月13日 下午2:01:41
	 */
	public String findczfz(PageData pd) throws Exception {

		String message = "";
		PageData findbyphonecheck = clinic_checkinService.findbyphonecheck(pd);
		if (findbyphonecheck != null) {
			message = "复诊";
		} else {
			PageData db = patientService.findByPhone(pd);
			if (db != null) {
				pd.put("patientId", db.get("patientId"));
				pd.put("sy", 1);
				List<PageData> list1 = clinic_checkinService.findhzJZlist(pd);
				if (list1 != null && list1.size() > 0) {
					message = "复诊";
				} else {
					message = "初诊";
				}
			} else {// 用户不存在
				message = "初诊";
			}
		}
		return message;
	}

	/**
	 * 游客模式绑定手机号或邮箱
	 * 
	 * @author 王立飞
	 * @date 2018年9月23日下午2:12:35
	 */
	@RequestMapping(value = "/bangdPhone", method = RequestMethod.POST)
	@ResponseBody
	public Object bangdPhone() {
		logBefore(logger, "游客模式绑定手机号或邮箱");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		boolean international = false;// 国内版
		if (IsNull.paramsIsNull(pd.get("international")) == false) {
			if ("1".equals(pd.get("international").toString())) {
				international = true;// 国际版
			}
		}
		String result;
		String message;
		if (international) {
			result = "10000";
			message = "Connection abnormality";
		} else {
			result = "10000";
			message = "连接异常";
		}
		try {
			if (IsNull.paramsIsNull(pd.get("patientId")) || IsNull.paramsIsNull(pd.get("password"))) {
				if (international) {
					result = "9993";
					message = "Parameter Anomaly";
				} else {
					result = "9993";
					message = "参数异常";
				}
			} else {
				// 查询个人信息
				PageData data = patientService.findPinfo(pd);
				// 检测用户是否使用邀请码
				if (IsNull.paramsIsNull(pd.get("inviteCode")) == false) {
					String inviteCode = InviteCodeControler.UpdateInviteCode(pd, data, inviteCodeService);
					if (IsNull.paramsIsNull(inviteCode)) {
						if (international) {
							result = "6003";
							message = "Invitation code error";
						} else {
							result = "6003";
							message = "邀请码错误";
						}
						map.put("result", result);
						map.put("message", message);
						return AppUtil.returnObject(new PageData(), map);
					}
				}
				if (international) {// mailbox
					// 国际版
					String PASSWORD = pd.get("password").toString().trim();
					pd.remove("password");
					PageData mailLogin = patientService.mailLogin(pd);// 查询邮箱是否存在
					if (mailLogin != null) {
						result = "1104";
						message = "The mailbox has been registered";
					} else {
						String passwd = new SimpleHash("SHA-1", PASSWORD).toString(); // 密码加密
						PageData dd = new PageData();
						dd.put("password", passwd);
						dd.put("patientId", pd.get("patientId"));
						dd.put("mailbox", pd.get("mailbox"));
						dd.put("visitor", "0");// 用户

						patientService.editMailbox(dd);

						if (international) {
							result = "0000";
							message = "Success";
						} else {
							result = "0000";
							message = "绑定成功";
						}
					}
				} else {
					// 国内版
					PageData db = patientService.findByPhone(pd);// phone
					if (IsNull.paramsIsNull(db)) {
						// 添加深度链接注册数统计
						if (IsNull.paramsIsNull(pd.get("IMEI")) == false) {
							PageData imei = ascriptionService.findByIMEI(pd);
							if (imei != null) {
								// 更新注册数
								pd.put("scanGenre", "2");// 0 正常 1扫码授权 2深度链接
								PageData pp = new PageData();
								pp.put("id", imei.get("id"));
								pp.put("register", "1");
								pp.put("userId", pd.get("patientId"));
								ascriptionService.updateData(pp);
							}
						}

						String passwd = new SimpleHash("SHA-1", pd.get("password")).toString(); // 密码加密
						pd.put("password", passwd);
						pd.put("visitor", "0");
						patientService.editPhone(pd);

						result = "0000";
						message = "绑定成功";
					} else {
						result = "1104";
						message = "手机号已存在，绑定失败！";
					}
				}
			}
		} catch (Exception e) {
			if (international) {
				result = "9999";
				message = "System Exception";
			} else {
				result = "9999";
				message = "系统异常";
			}
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 将登录的所有信息封装下
	 * 
	 * @return
	 */
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

	/**
	 * 5.1描述：知心医生 小程序
	 * 
	 * @auther 霍学杰
	 * @date 2017年9月29日
	 * @version 1.1
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/composite2", method = RequestMethod.POST)
	@ResponseBody
	public Object composite2() throws Exception {
		logBefore(logger, "知心医生 小程序");
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
				if ("17606448981".equals(pd.get("phone").toString())) {
					// 黑名单,黑名单,回头黑人多了,建库
					result = "1041";
					message = "此账号存在违规操作，不能进行注册";
				} else {
					boolean mobile = PhoneCheck.isMobile(pd.get("phone").toString());
					if (mobile) {
						// 检测用户是否使用邀请码
						if (IsNull.paramsIsNull(pd.get("inviteCode")) == false) {
							String inviteCode = InviteCodeControler.InviteCode(0, pd, inviteCodeService);
							if (IsNull.paramsIsNull(inviteCode) == false) {
								pd.put("inviteCode", inviteCode);
							} else {
								result = "6003";
								message = "邀请码错误";
								map.put("result", result);
								map.put("message", message);
								return AppUtil.returnObject(new PageData(), map);
							}
						} else {
							String creatInviteCode = InviteCodeControler.CreatInviteCode(0, inviteCodeService);
							pd.put("inviteCode", creatInviteCode);
						}

						PageData patient = patientService.findByPhone(pd);
						if (patient == null) {// 注册
							pd.put("IP", getRemortIP());
							pd.put("createTime", DateUtil.getTime());
							pd.put("headUrl", CommonConfig.PAT_HEAD_URL);
							pd.put("patientId", this.get32UUID());
							// pd.put("randomNum", enrollRandom());
							patientService.save(pd);
							patient = new PageData();
							patient = patientService.findPinfo(pd);
							map.put("patient", patient);
							result = "0000";
							message = "注册成功";
							PageData pp = new PageData();
							pp.put("patientId", patient.get("patientId"));
							pp.put("trueName", "知心" + patient.get("randomNum").toString());
							patientService.editPatientInfo(pp);
							// 添加时段统计
							PatientLoginController.patSAveHours(pd, activeHoursService);

							// 这里要去腾讯云那注册下，注册下！
							AppTencentCloudController.OneImport(patient.getString("doctorId"), pp.getString("trueName"),
									CommonConfig.PAT_HEAD_URL, cloudSignService);
						} else {
							map.put("patient", patient);
							result = "0000";
							message = "成功";
						}

						// ====================================
						// 最后这里我要返回腾讯云的sign，登录就返回
						String signCloud = "";
						if (IsNull.paramsIsNull(pd.get("IMlive")) == false && "2".equals(pd.get("IMlive").toString())) {
							signCloud = AppTencentCloudController.getUserSign(patient.getString("patientId"));
						} else if (IsNull.paramsIsNull(pd.get("IMlive")) == false
								&& "3".equals(pd.get("IMlive").toString())) {
							signCloud = AppTencentCloudController.getUserSign_h(patient.getString("patientId"));
						}
						map.put("signCloud", signCloud);

						if (IsNull.paramsIsNull(pd.get("code")) == false) {
							String code = pd.get("code").toString();
							String openid = openId.getOpenId(code);
							if ("".equals(openid) == false || "46103".equals(openid) == false) {
								PageData dd = new PageData();
								dd.put("patientId", patient.get("patientId"));
								dd.put("wxopenId", openid);
								PageData loginThree = patientService.LoginThree(dd);
								if (loginThree == null) {
									patientService.editPatientInfo(dd);
								}
								patient.put("wxopenId", openid);
							}
						} else {
							System.out.println("没有传code=======");
						}
					} else {
						map.put("result", "1041");
						map.put("message", "手机号不正确");
					}
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
}
