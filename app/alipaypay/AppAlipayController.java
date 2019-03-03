package com.fh.controller.app.alipaypay;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.fh.config.AlipayConfig;
import com.fh.config.CommonConfig;
import com.fh.config.CommonMessage;
import com.fh.config.WXPayConfig;
import com.fh.controller.base.BaseController;
import com.fh.controller.wxpay.WxPayUtil;
import com.fh.service.app.ClinicService;
import com.fh.service.app.CouponService;
import com.fh.service.app.DoctorMoneyDetailService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.OrderService;
import com.fh.service.app.VIPService;
import com.fh.service.app.WxPayService;
import com.fh.service.app.ZfbPayService;
import com.fh.service.statistics.InviteCodeService;
import com.fh.util.AlipayNotify;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.DoubleUtil;
import com.fh.util.tools.IsNull;
import com.fh.util.tools.jPush.examples.jPush;

/**
 * 描述：医生端支付
 * 
 * @time
 */
@Controller
@RequestMapping("/api/alipay")
public class AppAlipayController extends BaseController {

	@Resource
	ZfbPayService zfbPayService;
	@Resource
	OrderService orderService;
	@Resource
	WxPayService wxPayService;
	@Resource
	ClinicService clinicService;
	@Resource
	CouponService couponService;
	@Resource
	InviteCodeService inviteCodeService;
	@Resource
	DoctorService doctorService;
	@Resource
	DoctorMoneyDetailService doctorMoneyDetailService;
	@Resource
	VIPService vipService;

	/**
	 * #####已作废,勿删,ios提醒版本过低，请及时更新#####
	 */
	@RequestMapping(value = "/toPay", method = RequestMethod.POST)
	@ResponseBody
	public Object toPay() {
		logBefore(logger, "准备去支付");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000"; // 正常返回
		String retMessage = "服务器暂无响应!";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("payType")) || IsNull.paramsIsNull(pd.get("zuofei"))) {
				result = "9993";
				retMessage = "版本过低，请及时更新";
			} else {
			}
		} catch (Exception e) {
			result = "9999";
			retMessage = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("retMessage", retMessage);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * app医生端 使用 描述：准备去支付
	 * 
	 * @Description:
	 * @author 王立飞
	 * @date 2018年8月20日下午5:51:21
	 */
	@RequestMapping(value = "/toPayDoc", method = RequestMethod.POST)
	@ResponseBody
	public Object toPayDoc() {
		logBefore(logger, "准备去支付");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000"; // 正常返回
		String retMessage = "服务器暂无响应!";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("payType")) || IsNull.paramsIsNull(pd.get("payStyle"))) {
				result = "9993";
				retMessage = CommonMessage.CODE_9993;
			} else {

				pd.put("vId", pd.get("payStyle"));
				PageData data = vipService.findById(pd);
				if (data != null) {
					String payMoney = data.get("vPrice").toString();
					String isCoupon = "false";
					if (IsNull.paramsIsNull(pd.get("couponId")) == false) {// 一人一个邀请码
						PageData couponData = couponService.findById(pd);
						if (IsNull.paramsIsNull(couponData) == false) {
							payMoney = data.get("dPrice").toString();
							isCoupon = "true";
						}
					}
					// 支付宝
					// 生成未支付订单表
					PageData orderData = new PageData();
					orderData.put("orderId", this.get32UUID());
					orderData.put("doctorId", pd.get("doctorId"));
					orderData.put("clinicId", pd.get("clinicId"));
					orderData.put("payMoney", payMoney);
					orderData.put("payId", "");
					orderData.put("payType", pd.get("payType"));
					orderData.put("payStyle", pd.get("payStyle"));// 1一等会员 2二等会员 3三等会员
					orderData.put("isCoupon", isCoupon);
					if (IsNull.paramsIsNull(pd.get("couponId"))) {
						orderData.put("couponId", "");
					} else {
						orderData.put("couponId", pd.get("couponId"));
					}
					orderData.put("payName", data.get("vName"));
					orderService.save(orderData);
					//
					pd.put("orderId", orderData.get("orderId"));
					pd.put("payMoney", payMoney);
					pd.put("title", CommonConfig.PAY_TITLE);
					if (pd.get("payType").toString().equals("zfb")) {
						String alipayReturnContent = zfbPay(pd);
						map.put("alipayReturnContent", alipayReturnContent);
					} else if (pd.get("payType").toString().equals("wx")) {
						PageData wxData = wxPay(pd);
						map.put("wxData", wxData);
					}
					map.put("orderId", orderData.get("orderId"));
					result = "0000";
					retMessage = "成功";
				}
			}
		} catch (Exception e) {
			result = "9999";
			retMessage = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("retMessage", retMessage);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	/**
	 * 
	 * 描述：准备去支付---医生去聊天的次数 购买
	 * 
	 * @author 董雪蕊
	 * @date 2017年12月12日
	 * @version 2.0
	 * @return
	 */
	@RequestMapping(value = "/toPayNum", method = RequestMethod.POST)
	@ResponseBody
	public Object toPayNum() {
		logBefore(logger, "准备去支付---医生去聊天的次数 购买");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "10000"; // 正常返回
		String retMessage = "服务器暂无响应!";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("payType")) || IsNull.paramsIsNull(pd.get("payStyle"))
					|| IsNull.paramsIsNull(pd.get("answerNum"))) {
				result = "9993";
				retMessage = CommonMessage.CODE_9993;
			} else {
				int payMoney = 0;// 支付金额
				int answerNum = Integer.parseInt(pd.get("answerNum").toString());
				switch (answerNum) {
				case 10:
					payMoney = 10;
					break;
				case 50:
					payMoney = 30;
					break;
				case 100:
					payMoney = 50;
					break;
				default:
					break;
				}

				if (payMoney != 0) {
					// 支付宝
					// 生成未支付订单表
					PageData orderData = new PageData();
					orderData.put("orderId", this.get32UUID());
					orderData.put("doctorId", pd.get("doctorId"));
					orderData.put("clinicId", pd.get("clinicId"));
					orderData.put("payMoney", payMoney);
					orderData.put("payId", "");
					orderData.put("payType", pd.get("payType"));
					orderData.put("isCoupon", "");
					orderData.put("couponId", "");
					orderData.put("payStyle", pd.get("payStyle"));
					orderData.put("answerNum", pd.get("answerNum"));
					orderService.save(orderData);
					//
					pd.put("orderId", orderData.get("orderId"));
					pd.put("payMoney", payMoney);
					pd.put("title", "购买回答次数支付");
					if (pd.get("payType").toString().equals("zfb")) {
						String alipayReturnContent = zfbPay(pd);
						map.put("alipayReturnContent", alipayReturnContent);
					} else if (pd.get("payType").toString().equals("wx")) {
						PageData wxData = wxPay(pd);
						map.put("wxData", wxData);
					}
					map.put("orderId", orderData.get("orderId"));
				}

				result = "0000";
				retMessage = "成功";
			}
		} catch (Exception e) {
			result = "9999";
			retMessage = "服务器异常";
			e.printStackTrace();
		} finally {
			map.put("result", result);
			map.put("retMessage", retMessage);
			logAfter(logger);
		}
		return AppUtil.returnObject(new PageData(), map);
	}

	///////////// ***********以下为支付宝支付模块***********////////////////
	/**
	 * step1:实例化客户端 关键参数说明： 配置参数 示例值解释 获取方式/示例值 URL 支付宝网关（固定）
	 * https://openapi.alipay.com/gateway.do APP_ID APPID即创建应用后生成 获取见上面创建应用并获取APPID
	 * APP_PRIVATE_KEY 开发者应用私钥，由开发者自己生成 获取见上面配置密钥 FORMAT 参数返回格式，只支持json json（固定）
	 * CHARSET 请求和签名使用的字符编码格式，支持GBK和UTF-8 开发者根据实际工程编码配置 ALIPAY_PUBLIC_KEY
	 * 支付宝公钥，由支付宝生成 获取详见上面配置密钥 SIGN_TYPE 商户生成签名字符串所使用的签名算法类型，目前支持RSA2和RSA，推荐使用RSA2
	 */
	/**
	 * step2 直接调用相关API
	 * https://doc.open.alipay.com/docs/doc.htm?spm=a219a.7629140.0.0.KNw2nh&treeId=193&articleId=105465&docType=1
	 * 请求参数： out_trade_no 商户订单号（临时支付订单号） auth_code 支付授权码 什么鬼 subject 订单标题
	 * total_amount 订单金额（单位为元 精确小数点后两位） body 订单描述
	 */
	public static String zfbPay(PageData pd) throws AlipayApiException {
		String alipayReturnContent = "";
		AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, AlipayConfig.APP_ID,
				AlipayConfig.APP_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, AlipayConfig.ALIPAY_PUBLIC_KEY,
				AlipayConfig.SIGN_TYPE);
		AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
		request.setApiVersion(AlipayConfig.VERSION);
		request.setNotifyUrl(AlipayConfig.NOTICEURL);
		// 业务参数
		String out_trade_no = pd.get("orderId").toString(); // 商户网站唯一订单号
		String total_amount = pd.get("payMoney").toString() + ""; //
		String subject = pd.get("title").toString();
		// String subject =CommonConfig.PAY_TITLE;//标题
		// String body = "88,s,s,s=";//
		Float money = Float.parseFloat(total_amount);
//		Float money=0.01f;

		request.setBizContent("{ \"subject\":\"" + subject + "\"," + " \"out_trade_no\":\"" + out_trade_no + "\","
				+ " \"total_amount\":\"" + money + "\","
//				+ " \"body\":\"" + body + "\","               	
//				+ " \"biz_content\":\"" + 88 + "\"," 
				+ " \"product_code\":\"QUICK_MSECURITY_PAY\"}");
		AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
		if (response.isSuccess()) {
			alipayReturnContent = response.getBody();
		} else {
			System.out.println("调用失败");
		}
		return alipayReturnContent;
	}

	/**
	 * 描述：支付成功通知,生成真正支付成功订单信息 作者：董江宁 时间：2017-3-17
	 */
	/**
	 * 描述：服务器异步通知参数 准备创建一张表进行存储 notify_time 通知时间 notify_type 通知类型 notify_id 通知校验ID
	 * app_id 支付宝分配开发者的应用ID charset 编码格式 version 版本号 sign_type 签名类型 sign 签名 trade_no
	 * 支付宝交易凭证号 out_trade_no 商户订单号
	 */
	/**
	 * 
	 * @author 董雪蕊
	 * @description 支付宝异步返回接口
	 * @date 2017年12月17日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/zfbO")
	public void zfbO(PrintWriter out) {
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			Map<String, String> map = pd;
			if (AlipayNotify.newVerify(map)) {
				System.out.println("验签通过");
				PageData alipayNoticePd = zfbPayService.findByParam(pd);
				if (IsNull.paramsIsNull(alipayNoticePd)) {
					// 1》创建支付宝回调记录
					pd.put("zfbPayId", this.get32UUID());
					zfbPayService.save(pd);
					pd.put("orderId", pd.get("out_trade_no"));
					pd.put("payId", pd.get("trade_no"));// 支付宝交易号
					pd.put("payState", "1");
					pd.put("sucesTime", DateUtil.getTime());
					orderService.edit(pd);
					// 将诊所的状态修改为VIP
					PageData orderData = orderService.findById(pd);
					if (IsNull.paramsIsNull(orderData) == false) {
						pd.put("clinicId", orderData.get("clinicId"));
						// 查询诊所状态
						PageData clinicData = clinicService.findzsClinic(pd);
						if (IsNull.paramsIsNull(clinicData) == false) {
							pd.put("isVip", "1");
							String now = DateUtil.getTime();
							if (IsNull.paramsIsNull(clinicData.get("vipEndTime")) == false
									&& DateUtil.compareDate(clinicData.get("vipEndTime").toString(), now)) {
								pd.put("vipEndTime", DateUtil.getAfterYearDate("1",
										clinicData.get("vipEndTime").toString(), "yyyy-MM-dd"));
							} else {
								pd.put("vipEndTime", DateUtil.getAfterYearDate("1", now, "yyyy-MM-dd"));
							}
							clinicService.updVip(pd);
						}
						// 分销奖励
						pd.put("doctorId", orderData.get("doctorId"));
						double payMoney = Double.parseDouble(orderData.get("payMoney").toString());
						PageData invite = invite(pd);
						if (invite != null) {
							// 查询返利金
							pd.put("vId", orderData.get("payStyle"));
							PageData data = vipService.findById(pd);
							if (data != null) {
								if (IsNull.paramsIsNull(invite.get("2"))) {
									// 返20%奖励金
									double parseInt = Double.parseDouble(data.get("ClassA").toString());
									Double Proportion = DoubleUtil.mul(parseInt, 0.01);
									Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
									// 将医生的余额修改
									pd.put("doctorId", invite.get("1"));
									SaveMoney(pd, orderData, shangMoney);
								} else {
									if (IsNull.paramsIsNull(invite.get("2")) == false) {
										// 返10%奖励金
										double parseInt = Double.parseDouble(data.get("ClassB").toString());
										Double Proportion = DoubleUtil.mul(parseInt, 0.01);
										Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
										// 将医生的余额修改
										pd.put("doctorId", invite.get("2"));
										SaveMoney(pd, orderData, shangMoney);
									}
									if (IsNull.paramsIsNull(invite.get("1")) == false) {
										// 返20%奖励金
										double parseInt = Double.parseDouble(data.get("ClassA").toString());
										Double Proportion = DoubleUtil.mul(parseInt, 0.01);
										Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
										// 将医生的余额修改
										pd.put("doctorId", invite.get("1"));
										SaveMoney(pd, orderData, shangMoney);
									}
								}
							}
						}
					}
				}
				out.write("success");
			} else {
				out.write("fail");
			}

			out.close();
		} catch (

		Exception e) {
			logger.error(e.toString(), e);
		}

	}
	///////////// ***********以下为微信支付模块***********////////////////

	public PageData wxPay(PageData pd) {
		// 返回值
		PageData retData = new PageData();
		// 随机字符串
		String nonce_str = WxPayUtil.getNonceStr();
		// 订单编号
		String out_trade_no = pd.get("orderId").toString();
		// 支付金额，单位为分
//		int total_fee =1;
		String total_fee = WxPayUtil.getMoney(pd.get("payMoney").toString()); // 元换算成分
		//
//		String body =CommonConfig.PAY_TITLE;
		String body = pd.getString("title");
		// 商户号
		String mchId = WXPayConfig.mch_id;
		// 交易类型
		String tradeType = WXPayConfig.trade_type;
		// 支付密钥
		String key = WXPayConfig.key;
		// 微信支付完成后给该链接发送消息，判断订单是否完成
		String notifyUrl = WXPayConfig.notify_url;
		// app id
		String appid = WXPayConfig.appid;
		// 发起支付设备ip
		String spbillCreateIp = WXPayConfig.spbill_create_ip;
		// 附加数据，商户携带的订单的自定义数据 (原样返回到通知中,这类我们需要系统中订单的id 方便对订单进行处理)
		String attach = "hehe";

		// 我们后面需要键值对的形式，所以先装入map
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("appid", appid);
		sParaTemp.put("attach", attach);
		sParaTemp.put("body", body);
		sParaTemp.put("mch_id", mchId);
		sParaTemp.put("nonce_str", nonce_str);
		sParaTemp.put("notify_url", notifyUrl);
		sParaTemp.put("out_trade_no", out_trade_no);
		sParaTemp.put("spbill_create_ip", spbillCreateIp);
		sParaTemp.put("total_fee", total_fee + "");
		sParaTemp.put("trade_type", tradeType);

		// 去掉空值 跟 签名参数(空值不参与签名，所以需要去掉)
		Map<String, String> map1 = WxPayUtil.paraFilter(sParaTemp);
		/**
		 * 按照 参数=参数值&参数2=参数值2 这样的形式拼接（拼接需要按照ASCII码升序排列）
		 */
		String mapStr = WxPayUtil.createLinkString(map1);
		// MD5运算生成签名
		String sign = WxPayUtil.sign(mapStr, key, "utf-8").toUpperCase();
		sParaTemp.put("sign", sign);
		/**
		 * 组装成xml参数,此处偷懒使用手动组装，严格代码可封装一个方法，XML标排序需要注意，ASCII码升序排列
		 */
		String xml = "<xml>" + "<appid>" + appid + "</appid>" + "<attach>" + attach + "</attach>" + "<body>" + body
				+ "</body>" + "<mch_id>" + mchId + "</mch_id>" + "<nonce_str>" + nonce_str + "</nonce_str>"
				+ "<notify_url>" + notifyUrl + "</notify_url>" + "<out_trade_no>" + out_trade_no + "</out_trade_no>"
				+ "<spbill_create_ip>" + spbillCreateIp + "</spbill_create_ip>" + "<total_fee>" + total_fee
				+ "</total_fee>" + "<trade_type>" + tradeType + "</trade_type>" + "<sign>" + sign + "</sign>"
				+ "</xml>";
		// 统一下单url，生成预付id
		String url = "https://api.mch.weixin.qq.com/pay/unifiedorder";
		String result = WxPayUtil.httpRequest(url, "POST", xml);

		// Map<String, String> paramMap = new HashMap<String, String>();
		String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
		// 得到预支付id
		String prepay_id = "";
		try {
			prepay_id = WxPayUtil.getPayNo(result);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		// String packages = "prepay_id="+prepay_id;
		String nonceStr1 = WxPayUtil.getNonceStr();
		// 开始第二次签名
		// 统一下单接口返回正常的prepay_id，再按签名规范重新生成签名后，将数据传输给APP。
		// 参与签名的字段名为appid，noncestr,package,partnerid，prepayid，timestamp，注意：package的值格式为Sign=WXPay

		Map<String, String> preData = new HashMap<String, String>();
		preData.put("appid", appid);
		preData.put("noncestr", nonceStr1);
		preData.put("package", "Sign=WXPay");
		preData.put("partnerid", WXPayConfig.mch_id);
		preData.put("prepayid", prepay_id);
		preData.put("timestamp", timeStamp);
		// 去掉空值 跟 签名参数(空值不参与签名，所以需要去掉)
		Map<String, String> rMap = WxPayUtil.paraFilter(preData);
		/**
		 * 按照 参数=参数值&参数2=参数值2 这样的形式拼接（拼接需要按照ASCII码升序排列）
		 */
		String mapS = WxPayUtil.createLinkString(rMap);
		// MD5运算生成签名
		String sign2 = WxPayUtil.sign(mapS, key, "utf-8").toUpperCase();
		preData.put("sign", sign2);
		retData.putAll(preData);
		return retData;
	}

	/**
	 * 微信异步通知接口 董雪蕊 2017-12-15
	 */
	@SuppressWarnings("rawtypes")
	@RequestMapping(value = "/informWx")
	@ResponseBody
	public Object informWx() throws Exception {
		System.out.println("进入微信通知");
		HttpServletRequest request = getRequest();
		// 1.获取微信发送的xml
		StringBuffer sb = new StringBuffer();
		InputStream is = null;
		String xml = "";
		try {
			is = request.getInputStream();
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			String s = "";
			while ((s = br.readLine()) != null) {
				sb.append(s);
			}
			xml = sb.toString(); // 次即为接收到微信端发送过来的xml数据
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (StringUtils.isBlank(xml)) {
			return null;
		}
		// 2.xml转换Map、JSON
		Map<String, String> xmlMap = new HashMap<String, String>();
		Document document = null;
		try {
			document = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element element = document.getRootElement();
		Iterator iterator = element.elementIterator();
		while (iterator.hasNext()) {
			Element tag = (Element) iterator.next();
			xmlMap.put(tag.getName(), tag.getText());
		}
		// 3.验签
		Element return_xml = DocumentHelper.createElement("xml");
		// 校验下参数是否正确
		if (WxPayUtil.verifyWeixinNotify(xmlMap, WXPayConfig.key)) {
			System.out.println("成功验签");
			if ("SUCCESS".equals(xmlMap.get("return_code"))) {
				if ("SUCCESS".equals(xmlMap.get("result_code"))) {
					// 第一步：判断支付订单号是否存在 false 则存储内容 true 返回内容
					PageData pd = new PageData();
					pd.put("out_trade_no", xmlMap.get("out_trade_no"));// 商户订单号
					PageData wxNoticePd = wxPayService.findById(pd);
					if (IsNull.paramsIsNull(wxNoticePd) == false) {
						return_xml.addElement("return_code").setText("SUCCESS");
						return_xml.addElement("return_msg").setText("OK");
						return return_xml.asXML();
					}
					PageData data = new PageData();
					data.putAll(xmlMap);
					data.put("wxPayId", this.get32UUID());
					wxPayService.save(data);
					pd.put("trade_state", "SUCCESS");
					pd.put("orderId", pd.get("out_trade_no"));
					pd.put("payId", xmlMap.get("transaction_id"));// 微信交易号
					pd.put("payState", "1");
					pd.put("sucesTime", DateUtil.getTime());
					orderService.edit(pd);
					// ===============================2017-12-25 添加 医生购买次数

					PageData orderData = orderService.findById(pd);
					// 将诊所的状态修改为VIP
					pd.put("clinicId", orderData.get("clinicId"));
					// 查询诊所状态
					PageData clinicData = clinicService.findzsClinic(pd);
					if (IsNull.paramsIsNull(clinicData) == false) {
						pd.put("isVip", "1");
						String now = DateUtil.getTime();
						if (IsNull.paramsIsNull(clinicData.get("vipEndTime")) == false
								&& DateUtil.compareDate(clinicData.get("vipEndTime").toString(), now)) {
							pd.put("vipEndTime", DateUtil.getAfterYearDate("1", clinicData.get("vipEndTime").toString(),
									"yyyy-MM-dd"));
						} else {
							pd.put("vipEndTime", DateUtil.getAfterYearDate("1", now, "yyyy-MM-dd"));
						}
						clinicService.updVip(pd);
					}
					// 分销奖励
					pd.put("doctorId", orderData.get("doctorId"));
					double payMoney = Double.parseDouble(orderData.get("payMoney").toString());
					PageData invite = invite(pd);
					if (invite != null) {
						// 查询返利金
						pd.put("vId", orderData.get("payStyle"));
						PageData data1 = vipService.findById(pd);
						if (data1 != null) {
							if (IsNull.paramsIsNull(invite.get("2"))) {
								// 返20%奖励金
								double parseInt = Double.parseDouble(data1.get("ClassA").toString());
								Double Proportion = DoubleUtil.mul(parseInt, 0.01);
								Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
								// 将医生的余额修改
								pd.put("doctorId", invite.get("1"));
								SaveMoney(pd, orderData, shangMoney);
							} else {
								if (IsNull.paramsIsNull(invite.get("2")) == false) {
									// 返10%奖励金
									double parseInt = Double.parseDouble(data1.get("ClassB").toString());
									Double Proportion = DoubleUtil.mul(parseInt, 0.01);
									Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
									// 将医生的余额修改
									pd.put("doctorId", invite.get("2"));
									SaveMoney(pd, orderData, shangMoney);
								}
								if (IsNull.paramsIsNull(invite.get("1")) == false) {
									// 返20%奖励金
									double parseInt = Double.parseDouble(data1.get("ClassA").toString());
									Double Proportion = DoubleUtil.mul(parseInt, 0.01);
									Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
									// 将医生的余额修改
									pd.put("doctorId", invite.get("1"));
									SaveMoney(pd, orderData, shangMoney);
								}
							}
						}
					}
					return_xml.addElement("return_code").setText("SUCCESS");
					return_xml.addElement("return_msg").setText("OK");
					return return_xml.asXML();
				} else {
					// 业务结果失败返回
					return_xml.addElement("return_code").setText("SUCCESS");
					return_xml.addElement("return_msg").setText("OK");
					System.out.println("业务结果失败返回");
					return return_xml.asXML();
				}
			} else {
				// 失败返回
				return_xml.addElement("return_code").setText("SUCCESS");
				return_xml.addElement("return_msg").setText("OK");
				System.out.println("失败返回");
				return return_xml.asXML();
			}

		} else {
			// 失败返回
			System.out.println("验签失败");
			return_xml.addElement("return_code").setText("FAIL");
			return_xml.addElement("return_msg").setText("验签失败");
			return return_xml.asXML();
		}
	}

	/**
	 * 从微信得到订单的状态 并插入微信订单记录 修改订单微信支付状态
	 * 
	 * @throws DocumentException
	 */
	@SuppressWarnings("rawtypes")
	public String getState(String out_trade_no) throws Exception {
		String trade_state = "NORETURN";
		// 我们后面需要键值对的形式，所以先装入map
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("appid", WXPayConfig.appid);
		sParaTemp.put("mch_id", WXPayConfig.mch_id);
		sParaTemp.put("nonce_str", WxPayUtil.getNonceStr());
		sParaTemp.put("out_trade_no", out_trade_no);

		// 去掉空值 跟 签名参数(空值不参与签名，所以需要去掉)
		Map<String, String> map1 = WxPayUtil.paraFilter(sParaTemp);
		/**
		 * 按照 参数=参数值&参数2=参数值2 这样的形式拼接（拼接需要按照ASCII码升序排列）
		 */
		String mapStr = WxPayUtil.createLinkString(map1);
		// MD5运算生成签名
		String sign = WxPayUtil.sign(mapStr, WXPayConfig.key, "utf-8").toUpperCase();
		sParaTemp.put("sign", sign);
		/**
		 * 组装成xml参数,此处偷懒使用手动组装，严格代码可封装一个方法，XML标排序需要注意，ASCII码升序排列
		 */
		String xml = "<xml>" + "<appid>" + sParaTemp.get("appid") + "</appid>" + "<mch_id>" + sParaTemp.get("mch_id")
				+ "</mch_id>" + "<nonce_str>" + sParaTemp.get("nonce_str") + "</nonce_str>" + "<out_trade_no>"
				+ sParaTemp.get("out_trade_no") + "</out_trade_no>" + "<sign>" + sign + "</sign>" + "</xml>";

		String url = "https://api.mch.weixin.qq.com/pay/orderquery";
		String result = WxPayUtil.httpRequest(url, "POST", xml);
		// 1.获取微信发送的xml
		StringBuffer sb = new StringBuffer();
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(result.getBytes("UTF-8"));
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			String s = "";
			while ((s = br.readLine()) != null) {
				sb.append(s);
			}
			xml = sb.toString(); // 次即为接收到微信端发送过来的xml数据
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (StringUtils.isBlank(xml)) {
			return null;
		}
		// 2.xml转换Map、JSON
		Map<String, String> xmlMap = new HashMap<String, String>();
		Document document = null;
		try {
			document = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element element = document.getRootElement();
		Iterator iterator = element.elementIterator();
		while (iterator.hasNext()) {
			Element tag = (Element) iterator.next();
			xmlMap.put(tag.getName(), tag.getText());
		}

		//
		if (xmlMap.get("return_code").equals("SUCCESS") || xmlMap.get("result_code").equals("SUCCESS")) {
			trade_state = xmlMap.get("trade_state");
			PageData pd = new PageData();
			pd.put("orderId", xmlMap.get("out_trade_no"));
			PageData wPayData = wxPayService.findById(pd);
			if (IsNull.paramsIsNull(wPayData)) {
				pd.putAll(xmlMap);
				pd.put("wxPayId", this.get32UUID());
				wxPayService.save(pd);
				pd.put("payId", xmlMap.get("transaction_id"));// 微信交易号
				pd.put("trade_state", xmlMap.get("trade_state"));//
				if (trade_state.equals("SUCCESS")) {
					pd.put("sucesTime", DateUtil.getTime());
					pd.put("payState", "1");
					// 将诊所的状态修改为VIP
					PageData orderData = orderService.findById(pd);
					if (IsNull.paramsIsNull(orderData) == false) {
						pd.put("clinicId", orderData.get("clinicId"));
						// 查询诊所状态
						PageData clinicData = clinicService.findzsClinic(pd);
						if (IsNull.paramsIsNull(clinicData) == false) {
							pd.put("isVip", "1");
							String now = DateUtil.getTime();
							if (IsNull.paramsIsNull(clinicData.get("vipEndTime")) == false
									&& DateUtil.compareDate(clinicData.get("vipEndTime").toString(), now)) {
								pd.put("vipEndTime", DateUtil.getAfterYearDate("1",
										clinicData.get("vipEndTime").toString(), "yyyy-MM-dd"));
							} else {
								pd.put("vipEndTime", DateUtil.getAfterYearDate("1", now, "yyyy-MM-dd"));
							}
							clinicService.updVip(pd);
						}
						// 分销奖励
						pd.put("doctorId", orderData.get("doctorId"));
						double payMoney = Double.parseDouble(orderData.get("payMoney").toString());
						PageData invite = invite(pd);
						if (invite != null) {
							// 查询返利金
							pd.put("vId", orderData.get("payStyle"));
							PageData data = vipService.findById(pd);
							if (data != null) {
								if (IsNull.paramsIsNull(invite.get("2"))) {
									// 返20%奖励金
									double parseInt = Double.parseDouble(data.get("ClassA").toString());
									Double Proportion = DoubleUtil.mul(parseInt, 0.01);
									Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
									// 将医生的余额修改
									pd.put("doctorId", invite.get("1"));
									SaveMoney(pd, orderData, shangMoney);
								} else {
									if (IsNull.paramsIsNull(invite.get("2")) == false) {
										// 返10%奖励金
										double parseInt = Double.parseDouble(data.get("ClassB").toString());
										Double Proportion = DoubleUtil.mul(parseInt, 0.01);
										Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
										// 将医生的余额修改
										pd.put("doctorId", invite.get("2"));
										SaveMoney(pd, orderData, shangMoney);
									}
									if (IsNull.paramsIsNull(invite.get("1")) == false) {
										// 返20%奖励金
										double parseInt = Double.parseDouble(data.get("ClassA").toString());
										Double Proportion = DoubleUtil.mul(parseInt, 0.01);
										Double shangMoney = DoubleUtil.mul(payMoney, Proportion);
										// 将医生的余额修改
										pd.put("doctorId", invite.get("1"));
										SaveMoney(pd, orderData, shangMoney);
									}
								}
							}
						}
					}
				} else {
					pd.put("payState", "0");
					pd.put("payId", "");
				}
				orderService.edit(pd);

			}
		}
		return trade_state;
	}

	/**
	 * 
	 * @author 董雪蕊
	 * @description 查询微信订单支付状态
	 * @date 2017年12月16日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/getWxState")
	@ResponseBody
	public Object getWxState() {
		logBefore(logger, "查询微信订单支付状态");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("clinicId"))
					|| IsNull.paramsIsNull(pd.get("orderId"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				PageData ordData = orderService.findById(pd);
				if (IsNull.paramsIsNull(ordData) == false && IsNull.paramsIsNull(ordData.get("trade_state")) == false) {
					// 微信交易状态SUCCESS—支付成功 REFUND—转入退款 NOTPAY—未支付 CLOSED—已关闭
					// REVOKED—已撤销（刷卡支付）USERPAYING--用户支付中 PAYERROR--支付失败(其他原因，如银行返回失败)
					// NORETURN-未返回-自己定义状态
					String trade_state = ordData.get("trade_state").toString();
					if (trade_state.equals("NORETURN")) {// 没有收到微信的回调，去查询
						trade_state = getState(pd.get("orderId").toString());
					} else {
						trade_state = ordData.get("trade_state").toString();
					}
					map.put("trade_state", trade_state);
					result = "0000";
					retMessage = "查询成功";
				} else {
					result = "3002";
					retMessage = CommonMessage.CODE_3002;
				}

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
	 * @description 查询邀请码是否正确
	 * @date 2017年12月17日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/verfyCoupon")
	@ResponseBody
	public Object verfyCoupon() {
		logBefore(logger, "查询邀请码是否正确");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("couponId"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				String couponId = pd.get("couponId").toString().toUpperCase();
				pd.put("couponId", couponId);
				PageData data = couponService.findById(pd);
				if (IsNull.paramsIsNull(data)) {
					result = "3003";
					retMessage = "验证失败";
				} else {
					result = "0000";
					retMessage = "验证成功";
				}

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
	 * @description 查询用户是否购买、到期时间、价格
	 * @date 2017年12月17日
	 * @version 1.0
	 * @param pd
	 * @return
	 */
	@RequestMapping(value = "/selInfo")
	@ResponseBody
	public Object name() {
		logBefore(logger, "查询用户是否购买、到期时间、价格");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = new PageData();
		pd = this.getPageData();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 第一步：校验参数
			if (IsNull.paramsIsNull(pd.get("clinicId"))) {
				result = "9993";
				retMessage = "参数异常";
				map.put("retMessage", retMessage);
				map.put("result", result);
				return AppUtil.returnObject(new PageData(), map);
			} else {
				String now = DateUtil.getTime();
				PageData data = new PageData();
				data.put("isVip", "0");
				data.put("vipContinueTime", DateUtil.getAfterYearDate("1", now, "yyyy-MM-dd"));
				PageData cliData = clinicService.findzsClinic(pd);
				if (IsNull.paramsIsNull(cliData) == false && IsNull.paramsIsNull(cliData.get("isVip")) == false
						&& IsNull.paramsIsNull(cliData.get("vipEndTime")) == false) {
					int isVip = Integer.parseInt(cliData.get("isVip").toString());
					String vipEndTime = cliData.get("vipEndTime").toString();
					if (DateUtil.compareDate(vipEndTime, now)) {
						vipEndTime = DateUtil.getAfterYearDate("1", vipEndTime, "yyyy-MM-dd");
						data.put("isVip", isVip);
						data.put("vipContinueTime", vipEndTime);
					}
				}
				// data.put("smallMoney", CommonConfig.SMALL_MONEY);
				// data.put("bigMoney", CommonConfig.BIG_MONEY);
				result = "0000";
				retMessage = "成功";
				map.put("db", data);
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
	 * @Description:根据医生id查询父邀请医生
	 * @param @param pd
	 * @param @return
	 * @return PageData
	 * @author 王立飞
	 * @throws Exception
	 * @date 2018年8月19日上午10:53:25
	 */
	public PageData invite(PageData pd) throws Exception {
		PageData invite = new PageData();
		PageData docData = inviteCodeService.findByDocId(pd);
		if (docData != null) {
			if (!"0".equals(docData.get("fatherUserId").toString())) {
				// 有父邀请
				pd.put("inviteCode", docData.get("fatherUserId"));
				PageData data1 = inviteCodeService.findById(pd);
				if (data1 != null) {
					invite.put("1", data1.get("doctorId"));
					if (!"0".equals(data1.get("fatherUserId").toString())) {
						// 有父邀请
						pd.put("inviteCode", data1.get("fatherUserId"));
						PageData data2 = inviteCodeService.findById(pd);
						if (data2 != null) {
							invite.put("2", data2.get("doctorId"));
						}
					}
				}
			}
		}
		return invite;
	}

	/**
	 * @Description:修改医生余额与明细并推送
	 * @author 王立飞
	 * @date 2018年8月19日下午3:10:38
	 */
	private void SaveMoney(PageData pd, PageData orderData, Double shangMoney) throws Exception {
		PageData docData = doctorService.findById(pd);
		if (IsNull.paramsIsNull(docData) == false) {
			// 金钱
			double totalMoney = Double.parseDouble(docData.get("totalMoney").toString());
			double remainderMoney = Double.parseDouble(docData.get("remainderMoney").toString());
			docData.put("totalMoney", DoubleUtil.add(totalMoney, shangMoney));
			docData.put("remainderMoney", DoubleUtil.add(remainderMoney, shangMoney));
			doctorService.editDoctorInfo(docData);
			// 将此次订单信息存入 医生交易明细表
			docData.put("detailType", "0");
			docData.put("money", shangMoney);
			docData.put("explainTitle", "奖励金");
			docData.put("moneyType", "0");
			doctorMoneyDetailService.save(docData);
			// 推送消息
			// 您邀请的 xxx，成功注册成为 VIP 会员，奖励您 xxx元。
			String messageConet = "您邀请的" + docData.get("trueName").toString() + "，成功注册成为 VIP 会员，奖励您" + shangMoney
					+ "元!";

			Collection<String> alias = new ArrayList<>();
			Collection<String> registrationId = new ArrayList<>();
			registrationId.add(docData.get("phoneSole").toString());
			String toUserId = docData.get("doctorId").toString();
			alias.add(toUserId);
			String title = "奖励金通知";

			String phoneSole = "";
			String huaWeiToken = "";
			String miRegId = "";
			String mzPushId = "";
			phoneSole = docData.getString("phoneSole");
			huaWeiToken = docData.getString("huaWeiToken");
			miRegId = docData.getString("miRegId");
			mzPushId = docData.getString("mzPushId");

			jPush.sendAll(alias, registrationId, title, "59", messageConet, orderData.get("doctorId").toString(), "0",
					toUserId, "1", DateUtil.getDay(), DateUtil.getTime(), "", "", "", "", huaWeiToken, miRegId,
					mzPushId, phoneSole);
		}
	}
}
