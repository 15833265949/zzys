package com.fh.controller.app.alipaypay;

import java.io.PrintWriter;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.fh.config.AlipayConfig;
import com.fh.controller.base.BaseController;
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
import com.fh.service.app.ZfbPayService;
import com.fh.util.AlipayNotify;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 描述：支付宝支付处理
 * 
 * @time
 */
@Controller
@RequestMapping("/api/patiZFB")
public class AppPatiZFBAlipayController extends BaseController {

	@Resource
	ZfbPayService zfbPayService;
	@Resource
	OrderPatiService orderPatiService;
	@Resource
	ConversationLastService conversationLastService;
	@Resource
	DoctorService doctorService;
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

	///////////// ***********以下为支付宝支付模块***********////////////////
	/**
	 * step1:实例化客户端 关键参数说明： 配置参数 示例值解释 获取方式/示例值 URL 支付宝网关（固定）
	 * https://openapi.alipay.com/gateway.do APP_ID APPID即创建应用后生成
	 * 获取见上面创建应用并获取APPID APP_PRIVATE_KEY 开发者应用私钥，由开发者自己生成 获取见上面配置密钥 FORMAT
	 * 参数返回格式，只支持json json（固定） CHARSET 请求和签名使用的字符编码格式，支持GBK和UTF-8 开发者根据实际工程编码配置
	 * ALIPAY_PUBLIC_KEY 支付宝公钥，由支付宝生成 获取详见上面配置密钥 SIGN_TYPE
	 * 商户生成签名字符串所使用的签名算法类型，目前支持RSA2和RSA，推荐使用RSA2
	 */
	/**
	 * step2 直接调用相关API
	 * https://doc.open.alipay.com/docs/doc.htm?spm=a219a.7629140.0.0.KNw2nh&treeId=193&articleId=105465&docType=1
	 * 请求参数： out_trade_no 商户订单号（临时支付订单号） auth_code 支付授权码 什么鬼 subject 订单标题
	 * total_amount 订单金额（单位为元 精确小数点后两位） body 订单描述
	 */
	public static String zfbPay(PageData pd) throws AlipayApiException {
		String alipayReturnContent = "";
		AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, AlipayConfig.PATI_APP_ID,
				AlipayConfig.PATI_APP_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET,
				AlipayConfig.PATI_ALIPAY_PUBLIC_KEY, AlipayConfig.SIGN_TYPE);
		AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
		request.setApiVersion(AlipayConfig.VERSION);
		request.setNotifyUrl(AlipayConfig.PATI_NOTICEURL);
		// 支付宝附加参数，本系统中，支付业务类型
		String passback_params = pd.get("payStyle").toString();
		// 业务参数
		String out_trade_no = pd.get("orderPatiId").toString(); // 商户网站唯一订单号
		String total_amount = pd.get("payMoney").toString() + "";
		String subject = pd.get("title").toString();
		Float money = Float.parseFloat(total_amount);
		// Float money=0.01f;

		request.setBizContent("{ \"subject\":\"" + subject + "\"," + " \"out_trade_no\":\"" + out_trade_no + "\","
				+ " \"total_amount\":\"" + money + "\"," + " \"passback_params\":\"" + passback_params + "\","
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
	 * 描述：支付成功通知（只处理支付成功通知，成功后的业务根据类型来调用） 作者：董雪蕊 时间：2018-2-27 描述：服务器异步通知参数
	 * 准备创建一张表进行存储 notify_time 通知时间 notify_type 通知类型 notify_id 通知校验ID app_id
	 * 支付宝分配开发者的应用ID charset 编码格式 version 版本号 sign_type 签名类型 sign 签名 trade_no
	 * 支付宝交易凭证号 out_trade_no 商户订单号
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/zfbReply")
	public void zfbReply(PrintWriter out) {
		System.out.println("进入方法");
		PageData pd = new PageData();
		try {
			pd = this.getPageData();
			Map<String, String> map = pd;
			if (AlipayNotify.PatiNewVerify(map)) {
				System.out.println("验签通过");
				// 1》创建支付宝回调记录
				PageData alipayNoticePd = zfbPayService.findByParam(pd);
				if (IsNull.paramsIsNull(alipayNoticePd)) {
					pd.put("zfbPayId", this.get32UUID());
					zfbPayService.save(pd);
					// 根据支付的类型处理不同的业务
					int payStyle = Integer.parseInt(pd.get("passback_params").toString());
					AppPatiPayController appPatiPayController = new AppPatiPayController();
					switch (payStyle) {
					case 0:
						appPatiPayController.shangPaySuccess(pd, orderPatiService, conversationLastService,
								doctorService, patientService, doctorMoneyDetailService);
						break;
					case 1:
						appPatiPayController.chuPaySuccess(pd, clinicUserService, clinic_checkinService, chargeService,
								orderPatiService, doctorService, patientService, doctorMoneyDetailService,
								clinicService, monthCountService, dayCountService);
						break;
					case 2:
						appPatiPayController.yaoPaySuccess(pd, clinicUserService,
								clinic_checkinService, chargeService, orderPatiService, doctorService, patientService,
								doctorMoneyDetailService, monthCountService, dayCountService);
						break;
					case 3:
						appPatiPayController.zhuanPaySuccess(pd, orderPatiService, conversationLastService,
								doctorService, patientService, doctorMoneyDetailService);
						break;
					default:
						break;
					}
				}
				out.write("success");
			} else {
				out.write("fail");
			}

			out.close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

}
