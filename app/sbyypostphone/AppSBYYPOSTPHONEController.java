package com.fh.controller.app.sbyypostphone;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.controller.base.BaseController;
import com.fh.service.app.DoctorService;
import com.fh.service.app.PatientService;
import com.fh.service.app.SBYYPOSTPHONEService;
import com.fh.service.email.SendEmailService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.MD5;
import com.fh.util.PageData;
import com.fh.util.mail.SendMail163;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.PhoneCheck;

/**
 * 类名称：SBYYPOSTPHONEController 创建人：霍学杰 创建时间：2017.7.8
 */
@Controller
@RequestMapping(value = "/api/pushpostphone")
public class AppSBYYPOSTPHONEController extends BaseController {

	private final static Integer VALID_TIME = 30;// 过期时间30分钟

	private final static String singq = "zhixin";
	private final static String singh = ",.zxzs";

	@Resource(name = "sbyypostphoneService")
	private SBYYPOSTPHONEService sbyypostphoneService;

	@Resource
	private DoctorService doctorService;

	@Resource
	private PatientService patientService;

	@Resource
	private SendEmailService sendEmailService;

	/**
	 * 
	 * @author 董雪蕊
	 * @description 获取短信验证码
	 * @date 2018年3月25日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/getCode")
	@ResponseBody
	public Object getCode() {
		logBefore(logger, "获取短信验证码");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String message = "成功";
		try {
			String remortIP = getRemortIP("");
			if ("222.222.24.148".equals(remortIP) || "222.222.24.149".equals(remortIP)
					|| "222.222.24.150".equals(remortIP)) {
				// 第一步：校验参数
				if (IsNull.paramsIsNull(pd.get("postToPhoneNo"))) {
					result = "9993";
					message = "参数异常";
					map.put("message", message);
					map.put("result", result);
					return AppUtil.returnObject(new PageData(), map);
				} else {
					pd.put("endTime", DateUtil.getTime());

					PageData data = sbyypostphoneService.findByPhone(pd);
					map.put("db", data);
					result = "0000";
					message = "成功";
				}
			} else {
				result = "1002";
				message = "没有权限";
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
	 * 描述：发送验证短信接口 时间：2018.1.25
	 * 
	 * @author 霍学杰
	 * @return JSON
	 */
	@RequestMapping(value = "/postPhone", method = RequestMethod.POST)
	@ResponseBody
	public Object postPhone() {
		logBefore(logger, "postPhone");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		// 获取请求参数
		/*
		 * String result = "0000"; //正常返回 String message = "成功"; //正常返回
		 */ try {
			/**
			 * 加入请求判断 2018-1-22
			 */
			// 第一步：获取参数
			String sign = pd.getString("sign");// 短信验证码
			String phone = pd.getString("phone");// 手机号
			String RSADES = pd.getString("RSADES");// MD5加密后的串
			String sinMd4 = pd.getString("sinMd4");// 验证结果 图片验证码
			String sinID = pd.getString("sinID");// 验证ID 随机生成的串
			String actId = pd.getString("actId");// 验证标识 1.注册 2.找回密码3其他

			boolean mobile = PhoneCheck.isMobile(pd.get("phone").toString());
			// 第二步：校验参数 看是否为空
			if (mobile == false) {// 手机号不正确
				map.put("result", "1041");
				map.put("message", "手机号不正确");
				return AppUtil.returnObject(new PageData(), map);
			}
			String ip = getRemortIP(pd.get("phone").toString());
			PageData ddp = new PageData();
			ddp.put("IP", ip);
			ddp.put("phone", pd.get("phone"));

			if (StringUtils.isBlank(actId)) {// 为空
				map.put("result", "1042");
				map.put("message", "不对");
				return AppUtil.returnObject(new PageData(), map);
			}

			// 患者端修改手机号
			if (IsNull.paramsIsNull(pd.get("editPhone")) == false && "true".equals(pd.get("editPhone").toString())) {
				PageData findByPhone = patientService.findByPhone(pd);
				if (findByPhone != null) {
					map.put("result", "2033");
					map.put("message", "该手机号已注册");
					return AppUtil.returnObject(new PageData(), map);
				}
			}
			// 医生端修改手机号
			if (IsNull.paramsIsNull(pd.get("editPhone")) == false && "false".equals(pd.get("editPhone").toString())) {
				PageData findByPhone = doctorService.findByPhone(pd);
				if (findByPhone != null) {
					map.put("result", "2033");
					map.put("message", "该手机号已注册");
					return AppUtil.returnObject(new PageData(), map);
				}
			}

			// 第三步：处理逻辑
			PageData searchPd = new PageData();
			PageData returnPd;
			if (StringUtils.isBlank(sign)) {// 发送短信（验证码为空）

				if (IsNull.paramsIsNull(phone) || IsNull.paramsIsNull(RSADES)) {// 为空
					map.put("result", "1040");
					map.put("message", "手机号为空");
					return AppUtil.returnObject(new PageData(), map);
				} else {
					String ss = singq + phone + singh;
					if (RSADES.equals(MD5.md5(ss))) {
						System.out.println("MD5验证通过=====" + phone);
					} else {
						map.put("result", "1040");
						map.put("message", "格式不正确");
						return AppUtil.returnObject(new PageData(), map);
					}
				}

				if (IsNull.paramsIsNull(sinID) || IsNull.paramsIsNull(sinMd4)) {
					map.put("result", "1041");
					map.put("message", "图片验证码为空");
					return AppUtil.returnObject(new PageData(), map);
				} else {
					PageData findSign = sbyypostphoneService.findSign(pd);
					if (findSign != null) {
						String sinMd4s = sinMd4.toUpperCase();
						if (sinMd4s.equals(findSign.get("sinMd4").toString().toUpperCase())) {
							System.out.println("验证正确===sinMd4：" + sinMd4);
							ddp.put("state", "1");
							ddp.put("flag", "1");
							ddp.put("ipID", findSign.get("ipID"));
							sbyypostphoneService.editIp(ddp);
						} else {
							System.out.println("验证ID 随机生成的串:===" + sinID);
							System.out.println("验证手机号                   :===" + phone);
							System.out.println("验证结果 图片验证码   :===" + sinMd4);
							map.put("result", "1031");
							map.put("message", "图片验证码错误");
							return AppUtil.returnObject(new PageData(), map);
						}
					} else {
						map.put("result", "1033");
						map.put("message", "图片验证码超时，请刷新重新验证");
						return AppUtil.returnObject(new PageData(), map);
					}
				}
				searchPd.put("postToPhoneNo", phone);
				searchPd.put("actId", actId);// 验证标识 1.注册 2.找回密码3其他
				searchPd.put("sendTime", DateUtil.getDay() + " 00:00:11");// 发送时间
				Random random = new Random();
				int sl1 = random.nextInt(1000);
				int sl2 = random.nextInt(1000);
				int ss = 0;
				ss = sl1 + sl2;
				System.out.println("延迟发送=====" + ss);
				Thread.sleep(ss);
				returnPd = new PageData();
				returnPd = sbyypostphoneService.findCollectionByParams(searchPd);
				Long count = (Long) returnPd.get("counts");
				System.out.println("发送次数" + count);

				if (count >= 3) {// 判断发送次数是否超过
					map.put("result", "1043");
					map.put("message", "您今天的发送短信次数已超过！");
					return AppUtil.returnObject(new PageData(), map);
				}

				Integer newSign = getSign();
				String notes = getMsgNote(Integer.valueOf(actId), newSign);// 根据短信模板生成内容
				// save and send
				pd.put("Time", DateUtil.getDay() + " 00:01:01");
				List<PageData> toPip = sbyypostphoneService.getTOPip(pd);
				if (toPip != null && toPip.size() > 0) {
					for (int i = 0; i < toPip.size(); i++) {
						PageData spIP = new PageData();
						spIP = toPip.get(i);
						if (ip.equals(spIP.get("IP").toString())) {
							System.out.println("ip被禁用===========" + ip);
							map.put("result", "1043");
							map.put("message", "请求太过频繁，稍后再试！");
							return AppUtil.returnObject(new PageData(), map);
						}
					}
				}
				PageData createPd = new PageData();
				createPd.put("psan", "知心诊所");
				createPd.put("postToPhoneNo", phone);
				createPd.put("notes", notes);
				createPd.put("channel", 1);// 渠道 1.亿美
				createPd.put("createTime", new Date());

				createPd.put("endTime", DateUtil.getTime3(VALID_TIME));// 有效时间
				createPd.put("actId", actId);
				createPd.put("sign", newSign);
				createPd.put("flag", 0);// 状态 0.待发送 1.已发送2已校验
				createPd.put("SBYYPOSTPHONE_ID", this.get32UUID());

				sbyypostphoneService.save(createPd);// 保存到数据库
				map.put("result", "0000");
				map.put("message", "短信验证码发送成功！");

			} else {// 校验短信
				if (IsNull.paramsIsNull(phone)) {// 为空
					map.put("result", "1040");
					map.put("message", "手机号为空");
					return AppUtil.returnObject(new PageData(), map);
				}
				searchPd.put("postToPhoneNo", phone);
				searchPd.put("sign", sign);
				searchPd.put("actId", actId);
				searchPd.put("endTime", DateUtil.getTime());

				try {
					PageData db = sbyypostphoneService.validatePhoneSign(searchPd);

					if (db == null) {// 为空
						map.put("result", "1044");
						map.put("message", "短信验证码输入错误 ！");
						return AppUtil.returnObject(new PageData(), map);
					}

					if (!sign.equals(db.get("sign").toString())) {// 验证码不正确
						map.put("result", "1045");
						map.put("message", "短信验证码输入错误 ！");
						return AppUtil.returnObject(new PageData(), map);
					}
					// 修改验证码flag 状态
					db.put("flag", 2);// 状态 0.待发送 1.已发送2已校验
					sbyypostphoneService.edit(db);
					map.put("result", "0000");
					map.put("message", "短信验证码校验成功！");
				} catch (Exception e) {
					map.put("result", "9997");
					logger.error(e.getMessage());
				}
			}

		} catch (Exception e) {
			map.put("result", "9999");
			map.put("message", "系统繁忙");
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 描述：发送验证短信接口postPhoneWeb 时间：2018.1.25
	 * 
	 * @author 霍学杰
	 * @return JSON
	 */
	@RequestMapping(value = "/postPhoneWeb", method = RequestMethod.POST)
	@ResponseBody
	public Object postPhoneWeb() {
		logBefore(logger, "postPhoneWeb");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		// 获取请求参数
		/*
		 * String result = "0000"; //正常返回 String message = "成功"; //正常返回
		 */ try {
			/**
			 * 加入请求判断 2018-1-22
			 */

			String ip = getRemortIP(pd.get("phone").toString());
			PageData ddp = new PageData();
			ddp.put("IP", ip);
			ddp.put("phone", pd.get("phone"));

			// 第一步：获取参数
			String sign = pd.getString("sign");// 短信验证码
			String phone = pd.getString("phone");// 手机号
			String sinMd4 = pd.getString("sinMd4");// 图片验证结果
			String sinID = pd.getString("sinID");// 验证ID
			String actId = pd.getString("actId");// 验证标识 1.注册 2.找回密码3其他

			if (IsNull.paramsIsNull(phone)) {// 为空
				map.put("result", "1040");
				map.put("message", "手机号为空");
				return AppUtil.returnObject(new PageData(), map);
			}

			boolean mobile = PhoneCheck.isMobile(pd.get("phone").toString());
			// 第二步：校验参数 看是否为空
			if (mobile == false) {// 手机号不正确
				map.put("result", "1041");
				map.put("message", "手机号不正确");
				return AppUtil.returnObject(new PageData(), map);
			}

			if (StringUtils.isBlank(actId)) {// 为空
				map.put("result", "1042");
				map.put("message", "不对");
				return AppUtil.returnObject(new PageData(), map);
			}
			// 第三步：处理逻辑
			PageData searchPd = new PageData();
			PageData returnPd;
			if (StringUtils.isBlank(sign)) {// 发送短信（验证码为空）
				if (IsNull.paramsIsNull(sinID) || IsNull.paramsIsNull(sinMd4)) {
					map.put("result", "1041");
					map.put("message", "图片验证码为空");
					return AppUtil.returnObject(new PageData(), map);
				} else {
					PageData findSign = sbyypostphoneService.findSign(pd);
					if (findSign != null) {
						String sinMd4s = sinMd4.toUpperCase();
						if (sinMd4s.equals(findSign.get("sinMd4").toString().toUpperCase())) {
							System.out.println("验证正确===sinMd4：" + sinMd4);
							ddp.put("state", "1");
							ddp.put("flag", "2");
							ddp.put("ipID", findSign.get("ipID"));
							sbyypostphoneService.editIp(ddp);
						} else {
							map.put("result", "1031");
							map.put("message", "图片验证码错误");
							return AppUtil.returnObject(new PageData(), map);
						}
					} else {
						map.put("result", "1033");
						map.put("message", "图片验证码超时，请刷新重新验证");
						return AppUtil.returnObject(new PageData(), map);
					}
				}
				searchPd.put("postToPhoneNo", phone);
				searchPd.put("actId", actId);// 验证标识 1.注册 2.找回密码3其他
				searchPd.put("sendTime", DateUtil.getDay() + " 00:00:11");// 发送时间
				Random random = new Random();
				int sl1 = random.nextInt(1000);
				int sl2 = random.nextInt(1000);
				int ss = 0;
				ss = sl1 + sl2;
				System.out.println("延迟发送=====" + ss);
				Thread.sleep(ss);
				returnPd = new PageData();
				returnPd = sbyypostphoneService.findCollectionByParams(searchPd);
				Long count = (Long) returnPd.get("counts");
				System.out.println("发送次数" + count);

				if (count >= 3) {// 判断发送次数是否超过
					map.put("result", "1043");
					map.put("message", "您今天的发送短信次数已超过！");
					return AppUtil.returnObject(new PageData(), map);
				}

				Integer newSign = getSign();
				String notes = getMsgNote(Integer.valueOf(actId), newSign);// 根据短信模板生成内容
				// save and send
				pd.put("Time", DateUtil.getDay() + " 00:01:01");
				List<PageData> toPip = sbyypostphoneService.getTOPip(pd);
				if (toPip != null && toPip.size() > 0) {
					for (int i = 0; i < toPip.size(); i++) {
						PageData spIP = new PageData();
						spIP = toPip.get(i);
						if (ip.equals(spIP.get("IP").toString())) {
							System.out.println("ip被禁用===========" + ip);
							map.put("result", "1043");
							map.put("message", "请求太过频繁，稍后再试！");
							return AppUtil.returnObject(new PageData(), map);
						}
					}
				}
				PageData createPd = new PageData();
				createPd.put("psan", "知心诊所");
				createPd.put("postToPhoneNo", phone);
				createPd.put("notes", notes);
				createPd.put("channel", 1);// 渠道 1.亿美
				createPd.put("createTime", new Date());

				createPd.put("endTime", DateUtil.getTime3(VALID_TIME));// 有效时间
				createPd.put("actId", actId);
				createPd.put("sign", newSign);
				createPd.put("flag", 0);// 状态 0.待发送 1.已发送2已校验
				createPd.put("SBYYPOSTPHONE_ID", this.get32UUID());

				sbyypostphoneService.save(createPd);// 保存到数据库
				map.put("result", "0000");
				map.put("message", "短信验证码发送成功！");

			} else {// 校验短信
				searchPd.put("postToPhoneNo", phone);
				searchPd.put("sign", sign);
				searchPd.put("actId", actId);
				searchPd.put("endTime", DateUtil.getTime());

				try {
					PageData db = sbyypostphoneService.validatePhoneSign(searchPd);

					if (db == null) {// 为空
						map.put("result", "1044");
						map.put("message", "验证码输入错误 ！");
						return AppUtil.returnObject(new PageData(), map);
					}

					if (!sign.equals(db.get("sign").toString())) {// 验证码不正确
						map.put("result", "1045");
						map.put("message", "验证码输入错误 ！");
						return AppUtil.returnObject(new PageData(), map);
					}
					// 修改验证码flag 状态
					db.put("flag", 2);// 状态 0.待发送 1.已发送2已校验
					sbyypostphoneService.edit(db);
					map.put("result", "0000");
					map.put("message", "短信验证码校验成功！");
				} catch (Exception e) {
					map.put("result", "9997");
					logger.error(e.getMessage());
				}
			}

		} catch (Exception e) {
			map.put("result", "9999");
			map.put("message", "系统繁忙");
			e.printStackTrace();
			logger.error(e.getMessage());
		}

		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 描述：随机生成验证码图片
	 * 
	 * @author 霍学杰
	 * @param request
	 * @param response
	 * @throws Exception
	 * @date 2018.1.25
	 */
	@RequestMapping(value = "/create_code")
	public void createCode(HttpServletRequest request, HttpServletResponse response) throws Exception {

		PageData pd = new PageData();
		pd = this.getPageData(); // 获取请求参数

		String ip = getRemortIP("");
		pd.put("IP", ip);
		pd.put("flag", "3");
		int width = 105;// 宽
		int height = 30;// 高
		int avg = width / 5;

		BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);// 画布

		Graphics g = b.getGraphics();// 画笔
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);

		g.setColor(Color.BLACK);// 给画笔设置颜色
		// g.drawRect(0, 0, width - 1, height - 1); // 画外边框
		// 生成随机数
		char[] array = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
				'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
				'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
				'z' }; // 62
		char[] array3 = { '0', '1', '2', '3', '4' }; // 10
		char[] array1 = { '5', '6', '7', '8', '9' }; // 10

		char[] array4 = { '+', '-', '=' }; // 3
		Random r = new Random();
		StringBuffer sb = new StringBuffer();
		int flag = r.nextInt(10);
		int fuhao = 0;
		int sumss = 0;
		int sumsse = 0;
		if (flag > 5) {
			for (int i = 1; i <= 4; i++) {
				int num = 0; // 0-10
				char c = 0;
				if (i == 1) {
					num = r.nextInt(5);
					c = array1[num];// 每次循环就是一个随机码
					sumss = (int) c;
				} else if (i == 2) {
					num = r.nextInt(2); // 0-2
					c = array4[num];// 每次循环就是一个随机码
					fuhao = num;
				} else if (i == 3) {
					num = r.nextInt(5);
					c = array3[num];// 每次循环就是一个随机码
					sumsse = (int) c;
				} else if (i == 4) {
					c = '=';
				}
				Font font = new Font("Arial", Font.BOLD, 20);
				g.setFont(font);
				int gray = 130;// 0-255
				int black = 128;// 0-255
				int DARK_GRAY = 129;// 0-255

				Color color = new Color(gray, black, DARK_GRAY);
				g.setColor(color);

				g.drawString(String.valueOf(c), avg * i, 20);

				sb.append(c);
			}

			int randomCode = 0;
			switch (fuhao) {
			case 0:
				randomCode = sumss + sumsse - 96;
				break;
			case 1:
				randomCode = sumss - sumsse;
				break;
			}
			pd.put("sinMd4", randomCode);
			System.out.println(randomCode);
		} else {
			for (int i = 1; i <= 4; i++) {
				int num = r.nextInt(62); // 0-61
				char c = array[num];// 每次循环就是一个随机码

				Font font = new Font("Arial", Font.BOLD, 20);
				g.setFont(font);

				int gray = 130;// 0-255
				int black = 128;// 0-255
				int DARK_GRAY = 129;// 0-255

				Color color = new Color(gray, black, DARK_GRAY);
				g.setColor(color);

				g.drawString(String.valueOf(c), avg * i, 20);

				sb.append(c);
			}

			pd.put("sinMd4", sb.toString());

		}
		if (IsNull.paramsIsNull(pd.get("sinID")) == false) {
			//防止前端重复提交
			if(IsNull.paramsIsNull(sbyypostphoneService.findSign(pd))) {
				sbyypostphoneService.saveIP(pd);
			}
//			else {
//				sbyypostphoneService.updateBySinId(pd);
//			}
		} else {
			System.out.println("缺少参数----sinID");
		}

		// 扰乱线
		for (int i = 1; i <= 8; i++) {

			int x1 = r.nextInt(width);
			int y1 = r.nextInt(height);

			int x2 = r.nextInt(width);
			int y2 = r.nextInt(height);

			int gray = 204;// 0-255
			int black = 204;// 0-255
			int DARK_GRAY = 206;// 0-255

			Color color = new Color(gray, black, DARK_GRAY);
			g.setColor(color);

			g.drawLine(x1, y1, x2, y2);

		}

		System.out.println("系统生成的验证码是：" + sb.toString());

		/*
		 * // 系统生成的验证码存session HttpSession session = request.getSession();
		 * session.setAttribute("sysCode", sb.toString());
		 */

		// 图片的类型
		response.setContentType("image/jpeg");
		ServletOutputStream out = response.getOutputStream();
		ImageIO.write(b, "jpeg", out);
	}

	/**
	 * @Description: 获取msgnote
	 * @param actId
	 * @param actName
	 * @param sign
	 * @return
	 */
	private String getMsgNote(Integer actId, Integer sign) {
		String result = "";
		String dir = "SMSTemplate/";
		String vmPath = null;
		try {
			String proMapingPath = "/SMSTemplate/sms_mapping.properties";
			Properties properties = new Properties();
			InputStream in = getClass().getResourceAsStream(proMapingPath);
			properties.load(in);
			String fileName = (String) properties.get(String.valueOf(actId));
			vmPath = dir + fileName;
			VelocityEngine ve = new VelocityEngine();
			ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			ve.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
			ve.setProperty(Velocity.OUTPUT_ENCODING, "UTF-8");
			ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			ve.init();
			VelocityContext context = new VelocityContext();
			context.put("sign", sign);
			Template t = ve.getTemplate(vmPath);
			StringWriter writer = new StringWriter();
			t.merge(context, writer);
			result = writer.toString();
			in.close();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		logger.info(result);
		return result;
	}

	/**
	 * 获取短信验证码
	 * 
	 * @return
	 */
	private Integer getSign() {
		Random random = new Random();
		int x = random.nextInt(7999);
		return x + 1000;
	}

	/**
	 * 描述：发送邮箱验证码
	 * 
	 * @author 霍学杰
	 * @return JSON
	 * @date 2018年9月20日
	 */
	@RequestMapping(value = "/sendEmail", method = RequestMethod.POST)
	@ResponseBody
	public Object sendEmail() {
		logBefore(logger, "sendEmail");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "Connection abnormality";
		try {
			// 校验参数
			if (IsNull.paramsIsNull(pd.get("mailbox"))// 邮箱
					|| IsNull.paramsIsNull(pd.get("sinMd4"))// 图片验证结果
					|| IsNull.paramsIsNull(pd.get("sinID"))// 验证ID
			) {
				result = "9993";
				message = "Parameter Anomaly";
			} else {
				// 第一步：获取参数
				String sendEmail = pd.getString("mailbox");// 邮箱
				String sinMd4 = pd.getString("sinMd4");// 图片验证结果

				// 校验手机号
				boolean mobile = PhoneCheck.isEmail(pd.get("mailbox").toString());
				if (mobile == false) {// 手机号不正确
					result = "1041";
					message = "mailbox is incorrect";
					return AppUtil.returnObject(new PageData(), map);
				}

				// 添加短信发送记录
				// 先校验图片验证码是否正确
				String sinMd4s = sinMd4.toUpperCase();
				PageData findSign = sbyypostphoneService.findSign(pd);
				if (findSign != null) {
					if (sinMd4s.equals(findSign.get("sinMd4").toString().toUpperCase())) {
						PageData dd = new PageData();
						dd.put("phone", sendEmail);
						dd.put("state", "1");
						dd.put("flag", "3");
						dd.put("ipID", findSign.get("ipID"));

						sbyypostphoneService.editIp(dd);
					} else {
						result = "1032";
						message = "Image verification error";
						return AppUtil.returnObject(new PageData(), map);
					}
					// 发送邮件
					String TITLE = "Email receipt for verification";// 标题
					Integer sign = getSign();
					StringBuilder content = new StringBuilder();
					content.append("Dear Sir/Mam,\r\n\r").append("      [Intimate Doctor] verification code: ")
							.append(sign).append(", only valid within 5 minutes after receiving the code. ")
							.append("In order to keep your account safe, do not disclose the verification code to the others.");

					boolean sendState = SendMail163.sendEmail(sendEmail, TITLE, content.toString(), "1");
					if (sendState) {
						PageData email = new PageData();
						email.put("emailCode", sign);
						email.put("sendEmail", sendEmail);
						email.put("sendDate", DateUtil.getDay());
						email.put("endTime", DateUtil.getTime3(5));// 5分钟有效期
						email.put("genre", "【知心医生】");
						email.put("createTime", DateUtil.getTime());
						email.put("content", content.toString());
						sendEmailService.save(email);

						result = "0000";
						message = "Success";
					} else {
						result = "1003";
						message = "Failed to send";
					}
				} else {
					result = "1033";
					message = "Image verification code timed out, please refresh and re-verify";
				}
			}
		} catch (Exception e) {
			result = "9999";
			message = "System Exception";
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 校验邮箱验证码
	 * 
	 * @param pd
	 * @param sendEmailService
	 * @return
	 * @throws Exception
	 */
	public static int phoneCheckSign(PageData pd, SendEmailService sendEmailService) throws Exception {
		int checkType = 0;
		pd.put("currentTime", DateUtil.getTime());
		pd.put("sendEmail", pd.get("mailbox"));
		PageData findSign = sendEmailService.findSign(pd);
		if (findSign != null) {
			pd.put("emailId", findSign.get("emailId"));
			pd.put("fettle", 1);
			sendEmailService.updateFettle(pd);
			checkType = 1;
		} else {
			checkType = 2;
		}
		return checkType;
	}
	
	/**
	 * 描述：校验邮箱验证码
	 * 
	 * @author 霍学杰
	 * @return JSON
	 * @date 2018年9月21日
	 */
	@RequestMapping(value = "/checkEmail", method = RequestMethod.POST)
	@ResponseBody
	public Object checkEmail() {
		logBefore(logger, "checkEmail");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "Connection abnormality";
		try {
			// 校验参数
			if (IsNull.paramsIsNull(pd.get("mailbox"))// 邮箱
					|| IsNull.paramsIsNull(pd.get("emailCode"))//邮箱验证码
			) {
				result = "9993";
				message = "Parameter Anomaly";
			} else {
				int phoneCheckSign = phoneCheckSign(pd, sendEmailService);
				if(phoneCheckSign == 1) {
					
					result = "0000";
					message = "Success";
				} else {
					result = "1002";
					message = "Verification code error";
				}
			}
		} catch (Exception e) {
			result = "9999";
			message = "System Exception";
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			map.put("result", result);
			map.put("message", message);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}
	
	/**
	 * 
	* @Description:获取全部短信验证码
	* @author 王立飞
	* @date 2018年9月24日上午11:59:35
	 */
	@RequestMapping(value = "/findByAll")
	@ResponseBody
	public Object findByAll() {
		logBefore(logger, "获取全部短信验证码");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000";
		String message = "连接异常";
		try {
			Integer min = Integer.parseInt(pd.get("page").toString());
			Integer max = Integer.parseInt(pd.get("limit").toString());
				pd.put("min", min-1);
				pd.put("max", max);
				List<PageData> list = sbyypostphoneService.findByAll(pd);
				map.put("data", list);
				map.put("count", list.size());
				result = "0";
				message = "";

		} catch (Exception e) {
			e.printStackTrace();
			result = "9999";
			message = "系统异常";
		} finally {
			map.put("msg", message);
			map.put("code", result);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}
	
	

}
