package com.fh.controller.app.alipaypay;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.config.CommonConfig;
import com.fh.config.WXPayConfig;
import com.fh.controller.base.BaseController;
import com.fh.controller.wxpay.WxPayUtil;
import com.fh.service.app.ChargeService;
import com.fh.service.app.ClinicService;
import com.fh.service.app.ClinicUserService;
import com.fh.service.app.Clinic_checkinService;
import com.fh.service.app.ConversationLastService;
import com.fh.service.app.DayCountService;
import com.fh.service.app.DoctorMoneyDetailService;
import com.fh.service.app.DoctorService;
import com.fh.service.app.MonthCountService;
import com.fh.service.app.OrderPatiService;
import com.fh.service.app.PatientService;
import com.fh.service.app.WxPayService;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;
import com.fh.util.weixin.openId;

/**
 * 描述：微信支付处理
 * 
 * @time
 */
@Controller
@RequestMapping("/api/patiWX")
@SuppressWarnings("rawtypes")
public class AppPatiWXAlipayController extends BaseController {
	@Resource
	OrderPatiService orderPatiService;
	@Resource
	ConversationLastService conversationLastService;
	@Resource
	DoctorService doctorService;
	@Resource
	WxPayService wxPayService;
	@Resource
	PatientService patientService;
	@Resource
	DoctorMoneyDetailService doctorMoneyDetailService;
	@Resource
	private ChargeService chargeService;
	@Resource
	private Clinic_checkinService clinic_checkinService;
	@Resource
	private ClinicUserService clinicUserService;
	@Resource
	private ClinicService clinicService;
	@Resource
	private DayCountService dayCountService;
	@Resource
	private MonthCountService monthCountService;

	/**
	 * app支付
	 * 
	 * @param pd
	 * @return
	 */
	public static PageData wxPay(PageData pd) {
		// 返回值
		PageData retData = new PageData();
		// 随机字符串
		String nonce_str = WxPayUtil.getNonceStr();
		// 订单编号
		String out_trade_no = pd.get("orderPatiId").toString();
		// 支付金额，单位为分
		// int total_fee =1;
		String total_fee = WxPayUtil.getMoney(pd.get("payMoney").toString()); // 元换算成分
		//
		// String body =CommonConfig.PAY_TITLE;
		String body = pd.getString("title");
		// 商户号
		String mchId = WXPayConfig.PATI_mch_id;
		// 交易类型
		String tradeType = WXPayConfig.PATI_trade_type;
		// 支付密钥
		String key = WXPayConfig.PATI_key;
		// 微信支付完成后给该链接发送消息，判断订单是否完成
		String notifyUrl = WXPayConfig.PATI_notify_url;
		// app id
		String appid = WXPayConfig.PATI_APP_ID;
		// 发起支付设备ip
		String spbillCreateIp = WXPayConfig.PATI_spbill_create_ip;
		// 附加数据，商户携带的订单的自定义数据 (原样返回到通知中,这类我们需要系统中订单的id 方便对订单进行处理)
		// 放的是支付业务类型
		String attach = pd.get("payStyle").toString();

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
		// app参数
		preData.put("appid", appid);
		preData.put("noncestr", nonceStr1);
		preData.put("package", "Sign=WXPay");
		preData.put("partnerid", WXPayConfig.PATI_APP_ID);
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
	@RequestMapping(value = "/wxReply")
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
		if (WxPayUtil.verifyWeixinNotify(xmlMap, WXPayConfig.PATI_key)) {
			System.out.println("成功验签");
			if ("SUCCESS".equals(xmlMap.get("return_code"))) {
				if ("SUCCESS".equals(xmlMap.get("result_code"))) {
					// 第一步：判断支付订单号是否存在 false 则存储内容 true 返回内容
					PageData pd = new PageData();
					pd.put("out_trade_no", xmlMap.get("out_trade_no"));// 商户订单号
					PageData wxNoticePd = wxPayService.findById(pd);
					if (IsNull.paramsIsNull(wxNoticePd)) {
						pd.putAll(xmlMap);
						pd.put("wxPayId", this.get32UUID());
						pd.put("trade_state", "SUCCESS");
						wxPayService.save(pd);
						// 第二步：根据支付类型，跳到不同的逻辑
						int payStyle = Integer.parseInt(xmlMap.get("attach").toString());
						AppPatiPayController appPatiPayController = new AppPatiPayController();
						switch (payStyle) {
						case 0:
							appPatiPayController.shangPaySuccess(pd, orderPatiService, conversationLastService,
									doctorService, patientService, doctorMoneyDetailService);
							break;
						case 1:
							appPatiPayController.chuPaySuccess(pd, clinicUserService, clinic_checkinService,
									chargeService, orderPatiService, doctorService, patientService,
									doctorMoneyDetailService, clinicService, monthCountService, dayCountService);
							break;
						case 2:
							appPatiPayController.yaoPaySuccess(pd, clinicUserService,
									clinic_checkinService, chargeService, orderPatiService, doctorService,
									patientService, doctorMoneyDetailService, monthCountService, dayCountService);
							break;
						case 3:
							appPatiPayController.zhuanPaySuccess(pd, orderPatiService, conversationLastService,
									doctorService, patientService, doctorMoneyDetailService);
							break;
						default:
							break;
						}
					}
					return_xml.addElement("return_code").setText("SUCCESS");
					return_xml.addElement("return_msg").setText("OK");
					return return_xml.asXML();
				} else {
					// 业务结果失败返回
					return_xml.addElement("return_code").setText("SUCCESS");
					return_xml.addElement("return_msg").setText("OK");
					return return_xml.asXML();
				}
			} else {
				// 失败返回
				return_xml.addElement("return_code").setText("SUCCESS");
				return_xml.addElement("return_msg").setText("OK");
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
	public String getState(String out_trade_no, WxPayService wxPayService, OrderPatiService orderPatiService,
			ConversationLastService conversationLastService, DoctorService doctorService, PatientService patientService,
			DoctorMoneyDetailService doctorMoneyDetailService) throws Exception {
		String trade_state = "NORETURN";
		// 我们后面需要键值对的形式，所以先装入map
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("appid", WXPayConfig.PATI_APP_ID);
		sParaTemp.put("mch_id", WXPayConfig.PATI_mch_id);
		sParaTemp.put("nonce_str", WxPayUtil.getNonceStr());
		sParaTemp.put("out_trade_no", out_trade_no);

		// 去掉空值 跟 签名参数(空值不参与签名，所以需要去掉)
		Map<String, String> map1 = WxPayUtil.paraFilter(sParaTemp);
		/**
		 * 按照 参数=参数值&参数2=参数值2 这样的形式拼接（拼接需要按照ASCII码升序排列）
		 */
		String mapStr = WxPayUtil.createLinkString(map1);
		// MD5运算生成签名
		String sign = WxPayUtil.sign(mapStr, WXPayConfig.PATI_key, "utf-8").toUpperCase();
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

		// 判断状态
		if (xmlMap.get("return_code").equals("SUCCESS") || xmlMap.get("result_code").equals("SUCCESS")) {
			trade_state = xmlMap.get("trade_state");
			PageData pd = new PageData();
			pd.put("orderId", xmlMap.get("out_trade_no"));
			pd.put("out_trade_no", xmlMap.get("out_trade_no"));
			PageData wPayData = wxPayService.findById(pd);
			if (IsNull.paramsIsNull(wPayData)) {
				pd.putAll(xmlMap);
				pd.put("wxPayId", this.get32UUID());
				wxPayService.save(pd);
				if (IsNull.paramsIsNull(xmlMap.get("trade_state")) == false
						&& "SUCCESS".equals(xmlMap.get("trade_state").toString())
						&& IsNull.paramsIsNull(pd.get("attach")) == false) {
					int payStyle = Integer.parseInt(pd.get("attach").toString());
					switch (payStyle) {
					case 0:
						AppPatiPayController appPatiPayController = new AppPatiPayController();
						appPatiPayController.shangPaySuccess(pd, orderPatiService, conversationLastService,
								doctorService, patientService, doctorMoneyDetailService);
						break;
					default:
						break;
					}
				}
			}
		}
		return trade_state;
	}

	/************************************* 以下为小程序的********************************************************* */

	/**
	 * 小程序支付
	 * 
	 * @param pd
	 * @return
	 * @throws Exception
	 */
	public static PageData wxPay2(PageData pd, PatientService patientService) throws Exception {
		// 返回值
		PageData retData = new PageData();
		// app id
		String appid = CommonConfig.AppID;
		// 商户号
		String mchId = CommonConfig.PATI_mch_id;
		// 随机字符串
		String nonce_str = WxPayUtil.getNonceStr();
		// 商品简单描述
		String body = pd.getString("title");
		// 订单编号
		String out_trade_no = pd.get("orderPatiId").toString();
		// 支付金额，单位为分
		String total_fee = WxPayUtil.getMoney(pd.get("payMoney").toString()); // 元换算成分
		// 发起支付设备ip
		String spbillCreateIp = WXPayConfig.PATI_spbill_create_ip;
		// 微信支付完成后给该链接发送消息，判断订单是否完成
		String notifyUrl = CommonConfig.PATI_notify_url;
		// 交易类型
		String tradeType = CommonConfig.PATI_trade_type;
		// 支付密钥
		String key = CommonConfig.PATI_API_SECRET;
		// 附加数据，商户携带的订单的自定义数据 (原样返回到通知中,这类我们需要系统中订单的id 方便对订单进行处理)
		// 放的是支付业务类型
		String attach = pd.get("payStyle").toString();
		// 获取openId
		// String openid ="";
		String code = pd.get("code").toString();
		String openid = openId.getOpenId(code);
		// PageData patientData = patientService.findById(pd);
		// if (IsNull.paramsIsNull(patientData)==false &&
		// IsNull.paramsIsNull(patientData.get("wxopenId"))==false) {
		// openid = patientData.get("wxopenId").toString();
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
		sParaTemp.put("total_fee", total_fee);
		sParaTemp.put("trade_type", tradeType);
		sParaTemp.put("openid", openid);
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
				+ "<notify_url>" + notifyUrl + "</notify_url>" + "<openid>" + openid + "</openid>" + "<out_trade_no>"
				+ out_trade_no + "</out_trade_no>" + "<spbill_create_ip>" + spbillCreateIp + "</spbill_create_ip>"
				+ "<total_fee>" + total_fee + "</total_fee>" + "<trade_type>" + tradeType + "</trade_type>" + "<sign>"
				+ sign + "</sign>" + "</xml>";
		// 统一下单url，生成预付id
		String url = "https://api.mch.weixin.qq.com/pay/unifiedorder";
		String result = WxPayUtil.httpRequest(url, "POST", xml);
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
		// app参数
		// preData.put("appid", appid);
		// preData.put("noncestr", nonceStr1);
		// preData.put("package", "Sign=WXPay");
		// preData.put("partnerid", WXPayConfig.PATI_APP_ID);
		// preData.put("prepayid", prepay_id);
		// preData.put("timestamp", timeStamp);
		// 小程序参数
		preData.put("appId", appid);
		preData.put("nonceStr", nonceStr1);
		preData.put("package", "prepay_id=" + prepay_id);
		preData.put("signType", "MD5");
		preData.put("timeStamp", timeStamp);
		// 去掉空值 跟 签名参数(空值不参与签名，所以需要去掉)
		Map<String, String> rMap = WxPayUtil.paraFilter(preData);
		/**
		 * 按照 参数=参数值&参数2=参数值2 这样的形式拼接（拼接需要按照ASCII码升序排列）
		 */
		String mapS = WxPayUtil.createLinkString(rMap);
		// MD5运算生成签名
		String sign2 = WxPayUtil.sign(mapS, key, "utf-8").toUpperCase();
		// preData.put("sign", sign2);
		preData.put("paySign", sign2);
		retData.putAll(preData);

		return retData;
	}

	/**
	 * 微信异步通知接口 小程序 董雪蕊 2017-12-15
	 */
	@RequestMapping(value = "/wxReply2")
	@ResponseBody
	public Object wxReply2() throws Exception {
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
		if (WxPayUtil.verifyWeixinNotify(xmlMap, CommonConfig.PATI_API_SECRET)) {
			System.out.println("成功验签");
			if ("SUCCESS".equals(xmlMap.get("return_code"))) {
				if ("SUCCESS".equals(xmlMap.get("result_code"))) {
					// 第一步：判断支付订单号是否存在 false 则存储内容 true 返回内容
					PageData pd = new PageData();
					pd.put("out_trade_no", xmlMap.get("out_trade_no"));// 商户订单号
					PageData wxNoticePd = wxPayService.findById(pd);
					if (IsNull.paramsIsNull(wxNoticePd)) {
						pd.putAll(xmlMap);
						pd.put("wxPayId", this.get32UUID());
						pd.put("trade_state", "SUCCESS");
						wxPayService.save(pd);
						// 第二步：根据支付类型，跳到不同的逻辑
						AppPatiPayController appPatiPayController = new AppPatiPayController();
						int payStyle = Integer.parseInt(xmlMap.get("attach").toString());
						switch (payStyle) {
						case 0:
							appPatiPayController.shangPaySuccess(pd, orderPatiService, conversationLastService,
									doctorService, patientService, doctorMoneyDetailService);
							break;
						case 1:
							appPatiPayController.chuPaySuccess(pd, clinicUserService, clinic_checkinService,
									chargeService, orderPatiService, doctorService, patientService,
									doctorMoneyDetailService, clinicService, monthCountService, dayCountService);
							break;
						case 2:
							appPatiPayController.yaoPaySuccess(pd, clinicUserService,
									clinic_checkinService, chargeService, orderPatiService, doctorService,
									patientService, doctorMoneyDetailService, monthCountService, dayCountService);
							break;
						case 3:
							appPatiPayController.zhuanPaySuccess(pd, orderPatiService, conversationLastService,
									doctorService, patientService, doctorMoneyDetailService);
							break;
						default:
							break;
						}
					}
					return_xml.addElement("return_code").setText("SUCCESS");
					return_xml.addElement("return_msg").setText("OK");
					return return_xml.asXML();
				} else {
					// 业务结果失败返回
					return_xml.addElement("return_code").setText("SUCCESS");
					return_xml.addElement("return_msg").setText("OK");
					return return_xml.asXML();
				}
			} else {
				// 失败返回
				return_xml.addElement("return_code").setText("SUCCESS");
				return_xml.addElement("return_msg").setText("OK");
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
	public String getState2(String out_trade_no, WxPayService wxPayService, OrderPatiService orderPatiService,
			ConversationLastService conversationLastService, DoctorService doctorService, PatientService patientService,
			DoctorMoneyDetailService doctorMoneyDetailService) throws Exception {
		String trade_state = "NORETURN";
		// 我们后面需要键值对的形式，所以先装入map
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("appid", CommonConfig.AppID);
		sParaTemp.put("mch_id", WXPayConfig.PATI_mch_id);
		sParaTemp.put("nonce_str", WxPayUtil.getNonceStr());
		sParaTemp.put("out_trade_no", out_trade_no);

		// 去掉空值 跟 签名参数(空值不参与签名，所以需要去掉)
		Map<String, String> map1 = WxPayUtil.paraFilter(sParaTemp);
		/**
		 * 按照 参数=参数值&参数2=参数值2 这样的形式拼接（拼接需要按照ASCII码升序排列）
		 */
		String mapStr = WxPayUtil.createLinkString(map1);
		// MD5运算生成签名
		String sign = WxPayUtil.sign(mapStr, CommonConfig.PATI_API_SECRET, "utf-8").toUpperCase();
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

		// 判断状态
		if (xmlMap.get("return_code").equals("SUCCESS") || xmlMap.get("result_code").equals("SUCCESS")) {
			trade_state = xmlMap.get("trade_state");
			PageData pd = new PageData();
			pd.put("orderId", xmlMap.get("out_trade_no"));
			PageData wPayData = wxPayService.findById(pd);
			if (IsNull.paramsIsNull(wPayData)) {
				pd.putAll(xmlMap);
				pd.put("wxPayId", this.get32UUID());
				wxPayService.save(pd);
				if (IsNull.paramsIsNull(xmlMap.get("trade_state")) == false
						&& "SUCCESS".equals(xmlMap.get("trade_state").toString())
						&& IsNull.paramsIsNull(pd.get("attach")) == false) {
					int payStyle = Integer.parseInt(pd.get("attach").toString());
					switch (payStyle) {
					case 0:
						AppPatiPayController appPatiPayController = new AppPatiPayController();
						appPatiPayController.shangPaySuccess(pd, orderPatiService, conversationLastService,
								doctorService, patientService, doctorMoneyDetailService);
						break;
					default:
						break;
					}
				}
			}
		}
		return trade_state;
	}

}
