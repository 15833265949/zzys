package com.fh.controller.app.tencentCloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.config.TencentCloudConfig;
import com.fh.controller.base.BaseController;
import com.fh.controller.wxpay.WxPayUtil;
import com.fh.service.app.CloudSignService;
import com.fh.util.AppUtil;
import com.fh.util.PageData;
import com.fh.util.httprequest.HttpRequest;
import com.fh.util.tools.IsNull;
import com.tls.sigcheck.tls_sigcheck;

import java.io.*;

/**
 * 描述：腾讯云调用
 * 
 * @author 董雪蕊
 * @date 2017.12.22
 */

@Controller
@RequestMapping(value = "/api/tencentCloud")
public class AppTencentCloudController extends BaseController {

	@Resource
	private CloudSignService cloudSignService;

	/**
	 * 生成用户签名
	 * 
	 * @param userId 用户ID
	 * @return
	 */
	public static String getUserSign(String userId) {
		String sign = "error";
		tls_sigcheck demo = new tls_sigcheck();

		demo.loadJniLib(TencentCloudConfig.CHECK_DLL_PATH);
		int ret = demo.tls_gen_signature_ex2(TencentCloudConfig.APP_ID, userId, TencentCloudConfig.PRIVATE_KEY);

		if (0 != ret) {
		} else {
			sign = demo.getSig();
		}
		return sign;
	}

	/**
	 * 生成用户签名
	 * 
	 * @param userId 用户ID
	 * @return
	 */
	public static String getUserSign_h(String userId) {
		String sign = "error";
		tls_sigcheck demo = new tls_sigcheck();

		demo.loadJniLib(TencentCloudConfig.CHECK_DLL_PATH);
		int ret = demo.tls_gen_signature_ex2(TencentCloudConfig.APP_ID_h, userId, TencentCloudConfig.PRIVATE_KEY_h);

		if (0 != ret) {
		} else {
			sign = demo.getSig();
		}
		return sign;
	}

	/**
	 * 注册时向腾讯云注册个用户 返回结果：resuInteger -1 失败 0 成功
	 * 
	 * @param userId
	 * @param trueName
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static Integer OneImport(String userId, String trueName, String FaceUrl, CloudSignService cloudSignService) {
		int resuInteger = -1;
		try {
			// 导入一个的地址
			PageData siData = cloudSignService.findById(null);
			String signAdmin = siData.getString("CloudSign");
			String random = WxPayUtil.getNonceStr();
			String ONE_IMPORT = "https://console.tim.qq.com/v4/im_open_login_svc/account_import?" + "usersig="
					+ signAdmin + "&identifier=" + TencentCloudConfig.ADMIN_NAME + "&sdkappid="
					+ TencentCloudConfig.APP_ID + "&random=" + random + "&contenttype=json";

			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("Identifier", userId);
			if (IsNull.paramsIsNull(trueName) == false) {
				paramMap.put("Nick", trueName);
			}
			if (IsNull.paramsIsNull(FaceUrl) == false) {
				paramMap.put("FaceUrl", FaceUrl);
			}
			paramMap.put("Type", 0);
			ObjectMapper json = new ObjectMapper();
			String params = json.writeValueAsString(paramMap);
			String result = HttpRequest.sendPost(ONE_IMPORT, params);
			JSONObject retJSON = JSONObject.fromObject(result);
			String ActionStatus = retJSON.getString("ActionStatus");
			if (ActionStatus.equals("OK")) {
				resuInteger = 0;
			}
		} catch (Exception e) {

		}
		return resuInteger;
	}

	/**
	 * 修改个人资料调用 董雪蕊 2017-12-23
	 * 
	 * @param userId
	 * @param Tag_Profile_IM_Nick          昵称
	 * @param sex                          性别 1.男 0.女
	 * @param Tag_Profile_IM_BirthDay      生日
	 * @param Tag_Profile_IM_SelfSignature 个性签名
	 * @param Tag_Profile_IM_Image         头像URL
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setUserInfo(String userId, String Tag_Profile_IM_Nick, int sex, String Tag_Profile_IM_BirthDay,
			String Tag_Profile_IM_SelfSignature, String Tag_Profile_IM_Image, CloudSignService cloudSignService) {
		try {
			// 导入一个的地址
			PageData siData = cloudSignService.findById(null);
			String signAdmin = siData.getString("CloudSign");
			String random = WxPayUtil.getNonceStr();
			String ONE_IMPORT = "https://console.tim.qq.com/v4/profile/portrait_set?" + "usersig=" + signAdmin
					+ "&identifier=" + TencentCloudConfig.ADMIN_NAME + "&sdkappid=" + TencentCloudConfig.APP_ID
					+ "&random=" + random + "&contenttype=json";
			// 拼接
			Map<String, Object> paramMap = new HashMap<>();
			List<Map> ProfileItemList = new ArrayList<>();
			// 昵称
			if (IsNull.paramsIsNull(Tag_Profile_IM_Nick) == false) {
				Map tMap = new HashMap<>();
				tMap.put("Tag", "Tag_Profile_IM_Nick");
				tMap.put("Value", Tag_Profile_IM_Nick);
				ProfileItemList.add(tMap);
			}
			// 性别Gender_Type_Unknown：没设置性别； Gender_Type_Female：女性；
			// Gender_Type_Male：男性。
			if (IsNull.paramsIsNull(sex) == false && sex != 3) {
				String Tag_Profile_IM_Gender = "Gender_Type_Male";
				if (sex == 1) {
					Tag_Profile_IM_Gender = "Gender_Type_Female";
				}
				Map tMap = new HashMap<>();
				tMap.put("Tag", "Tag_Profile_IM_Gender");
				tMap.put("Value", Tag_Profile_IM_Gender);
				ProfileItemList.add(tMap);
			}
			// 个性签名 长度不得超过500个字节
			if (IsNull.paramsIsNull(Tag_Profile_IM_SelfSignature) == false) {
				Map tMap = new HashMap<>();
				tMap.put("Tag", "Tag_Profile_IM_SelfSignature");
				tMap.put("Value", Tag_Profile_IM_SelfSignature);
				ProfileItemList.add(tMap);
			}
			// 头像
			if (IsNull.paramsIsNull(Tag_Profile_IM_Image) == false) {
				Map tMap = new HashMap<>();
				tMap.put("Tag", "Tag_Profile_IM_Image");
				tMap.put("Value", Tag_Profile_IM_Image);
				ProfileItemList.add(tMap);
			}

			paramMap.put("From_Account", userId);
			paramMap.put("ProfileItem", ProfileItemList);
			ObjectMapper json = new ObjectMapper();
			String params = json.writeValueAsString(paramMap);
//			String result = 
					HttpRequest.sendPost(ONE_IMPORT, params);
//			JSONObject retJSON = JSONObject.fromObject(result);
//			String ActionStatus = retJSON.getString("ActionStatus");
		} catch (Exception e) {

		}
	}

	/**
	 * 获取用户在线状态
	 */
	public static String getIsOnline(String userId, CloudSignService cloudSignService) {
		String resu = "not";
		try {
			// 导入一个的地址
			PageData siData = cloudSignService.findById(null);
			String signAdmin = siData.getString("CloudSign");
			String random = WxPayUtil.getNonceStr();
			String ONE_IMPORT = "https://console.tim.qq.com/v4/openim/querystate?" + "usersig=" + signAdmin
					+ "&identifier=" + TencentCloudConfig.ADMIN_NAME + "&sdkappid=" + TencentCloudConfig.APP_ID
					+ "&random=" + random + "&contenttype=json";

			Map<String, Object> paramMap = new HashMap<>();
			List<String> userList = new ArrayList<>();
			userList.add(userId);
			paramMap.put("To_Account", userList);
			ObjectMapper json = new ObjectMapper();
			String params = json.writeValueAsString(paramMap);
			String result = HttpRequest.sendPost(ONE_IMPORT, params);
			JSONObject retJSON = JSONObject.fromObject(result);
			JSONArray listArray = JSONArray.fromObject(retJSON.getString("QueryResult"));
			String State = listArray.getJSONObject(0).getString("State");
			resu = State;
		} catch (Exception e) {

		}
		return resu;
	}

	/**
	 * 调取腾讯云得到消息聊天记录，并保存 董雪蕊
	 * {"ChatType":"C2C","MsgTime":"2018031517","File":[{"URL":"https://download.tim.qq.com/msg_history/3/ca689bb67830e04628b911e88207e89a8fbdde40.gz","ExpireTime":"2018-03-16
	 * 09:49:28","FileSize":2044,"FileMD5":"21586c773118bc40071d190f0acb412b","GzipSize":793,"GzipMD5":"6e30e751a6257acca606242446851b40"}],"ActionStatus":"OK","ErrorInfo":"","ErrorCode":0}
	 * 2018-03-15
	 * 
	 * @throws Exception
	 */
	public static String getHistoryNews(CloudSignService cloudSignService, String hourTime) {
		// 导入一个的地址
		String result = "error";
		try {
			PageData siData = cloudSignService.findById(null);
			String signAdmin = siData.getString("CloudSign");
			String random = WxPayUtil.getNonceStr();
			String ONE_IMPORT = "https://console.tim.qq.com/v4/open_msg_svc/get_history?" + "usersig=" + signAdmin
					+ "&identifier=" + TencentCloudConfig.ADMIN_NAME + "&sdkappid=" + TencentCloudConfig.APP_ID
					+ "&random=" + random + "&contenttype=json";

			Map<String, String> paramMap = new HashMap<>();
			paramMap.put("ChatType", "C2C");
			paramMap.put("MsgTime", hourTime);
			ObjectMapper json = new ObjectMapper();
			String params = json.writeValueAsString(paramMap);
			String getMess = HttpRequest.sendPost(ONE_IMPORT, params);
			JSONObject messObject = JSONObject.fromObject(getMess);
			if (messObject.getString("ActionStatus").equals("OK")) {
				JSONArray fileArray = JSONArray.fromObject(messObject.getString("File"));
				String retFileString = "";
				for (int i = 0; i < fileArray.size(); i++) {
					JSONObject fileObject = fileArray.getJSONObject(i);
					if (i == 0) {
						retFileString = fileObject.getString("URL");
					} else {
						retFileString = retFileString + "," + fileObject.getString("URL");
					}
				}
				result = retFileString;
			} else {// {"ActionStatus":"FAIL","ErrorInfo":"Err_File_Not_Ready","ErrorCode":1004}
				result = result + ":" + messObject.getString("ErrorCode") + ":" + messObject.getString("ErrorInfo");
			}
		} catch (Exception e) {
		}
		return result;
	}

	/**
	 * 调取腾讯云得到消息聊天记录，并保存 董雪蕊 2018-03-15
	 * 
	 * @throws Exception
	 */
	@RequestMapping(value = "/getHistoryNews")
	@ResponseBody
	public Object getHistoryNews() {
		logBefore(logger, "调取腾讯云得到消息聊天记录，并保存");
		Map<String, Object> map = new HashMap<String, Object>();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 导入一个的地址
			PageData siData = cloudSignService.findById(null);
			String signAdmin = siData.getString("CloudSign");
			String random = WxPayUtil.getNonceStr();
			String ONE_IMPORT = "https://console.tim.qq.com/v4/open_msg_svc/get_history?" + "usersig=" + signAdmin
					+ "&identifier=" + TencentCloudConfig.ADMIN_NAME + "&sdkappid=" + TencentCloudConfig.APP_ID
					+ "&random=" + random + "&contenttype=json";

			Map<String, String> paramMap = new HashMap<>();
			paramMap.put("ChatType", "C2C");
			paramMap.put("MsgTime", "2018031405");
			ObjectMapper json = new ObjectMapper();
			String params = json.writeValueAsString(paramMap);
			HttpRequest.sendPost(ONE_IMPORT, params);
			result = "0000";
			retMessage = "成功";

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
	 * 根据UUID得到音频文件 董雪蕊 2018-03-15
	 * 
	 * {"ActionStatus":"OK","errorcode":0,"ErrorInfo":"","url_info":[{"file_type":3,"file_id":"305d02010004563054020100041231343431313532303938313036313137373702037a1afd02042416a3b402045aaa399f0424373330373435363037303434353331363531315fcf6fd4ec00ca3d78c8397803887824910201000201000400","url":"https://grouptalk.c2c.qq.com/asn.com/stddownload_common_file?ver=1&openid=144115209799806238&bid=10001&authkey=3043020101043c303a02010102010102040cea523202037a1afd0204e6dde2650204e6dde26502037a1db902044f89740e020438fd03b702045ab5a8280204212b8ae70400&fileid=305d02010004563054020100041231343431313532303938313036313137373702037a1afd02042416a3b402045aaa399f0424373330373435363037303434353331363531315fcf6fd4ec00ca3d78c8397803887824910201000201000400&filetype=2106"}]}
	 * 
	 * @throws Exception
	 */
	@RequestMapping(value = "/getHistoryUrl")
	@ResponseBody
	public Object getHistoryUrl() {
		logBefore(logger, "调取腾讯云得到消息聊天记录，并保存");
		Map<String, Object> map = new HashMap<String, Object>();
		String result = "0000";
		String retMessage = "成功";
		try {
			// 导入一个的地址
			PageData siData = cloudSignService.findById(null);
			String signAdmin = siData.getString("CloudSign");
			String random = WxPayUtil.getNonceStr();
			String ONE_IMPORT = "https://console.tim.qq.com/v4/rich_media/query_file_url?" + "usersig=" + signAdmin
					+ "&identifier=" + TencentCloudConfig.ADMIN_NAME + "&sdkappid=" + TencentCloudConfig.APP_ID
					+ "&random=" + random + "&contenttype=json";

			List<Map<String, Object>> list = new ArrayList<>();
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("file_type", 3);
			paramMap.put("file_id",
					"305d02010004563054020100041231343431313532303938313036313137373702037a1afd02042416a3b402045aaa399f0424373330373435363037303434353331363531315fcf6fd4ec00ca3d78c8397803887824910201000201000400");
			paramMap.put("storage_platform", 0);
			list.add(paramMap);

			Map<String, Object> getMap = new HashMap<>();
			getMap.put("Download_Info", list);
			ObjectMapper json = new ObjectMapper();
			String params = json.writeValueAsString(getMap);
			HttpRequest.sendPost(ONE_IMPORT, params);
			result = "0000";
			retMessage = "成功";

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
	 * 根据UUID得到语音消息源文件地址
	 * 
	 * @param UUID
	 * @return
	 */
	@SuppressWarnings("unused")
	public static String getSoundFile(CloudSignService cloudSignService, String UUID) {
		String result = "error";
		try {
			// 导入一个的地址
			PageData siData = cloudSignService.findById(null);
			String signAdmin = siData.getString("CloudSign");
			String random = WxPayUtil.getNonceStr();
			String ONE_IMPORT = "https://console.tim.qq.com/v4/rich_media/query_file_url?" + "usersig=" + signAdmin
					+ "&identifier=" + TencentCloudConfig.ADMIN_NAME + "&sdkappid=" + TencentCloudConfig.APP_ID
					+ "&random=" + random + "&contenttype=json";
			List<Map<String, Object>> list = new ArrayList<>();
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("file_type", 3);
			paramMap.put("file_id", UUID);
			paramMap.put("storage_platform", 0);
			list.add(paramMap);
			Map<String, Object> getMap = new HashMap<>();
			getMap.put("Download_Info", list);
			ObjectMapper json = new ObjectMapper();
			String params = json.writeValueAsString(getMap);
			String resu = HttpRequest.sendPost(ONE_IMPORT, params);
			JSONObject jsonObject = JSONObject.fromObject(resu);
			if (jsonObject.getString("ActionStatus").equals("OK")) {
				JSONArray fileArray = JSONArray.fromObject(jsonObject.getString("url_info"));
				result = jsonObject.getString("url");
			} else {// {
				result = result + ":" + jsonObject.getString("errorcode") + ":" + jsonObject.getString("ErrorInfo");
			}
		} catch (Exception e) {

		}
		return result;
	}
}
