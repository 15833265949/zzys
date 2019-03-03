package com.fh.controller.app.doctorMoneyDetail;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fh.config.CommonMessage;
import com.fh.controller.base.BaseController;
import com.fh.service.app.DoctorService;
import com.fh.service.app.WithdrawService;
import com.fh.util.AppUtil;
import com.fh.util.DateUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/*
 * 获取提现数据接口
 */

@Controller
@RequestMapping("/api/withdraw")
public class WithdrawController extends BaseController {

	@Autowired
	private WithdrawService withdrawService;
	@Resource
	DoctorService doctorService;

	/*
	 * 获取页面数据
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Object save() {
		logBefore(logger, "申请提现");
		Map<String, Object> map = new HashMap<String, Object>();
		PageData pd = this.getPageData();
		String result = "";
		String message = "";
		try {
			if (IsNull.paramsIsNull(pd.get("doctorId")) || IsNull.paramsIsNull(pd.get("money"))
					|| IsNull.paramsIsNull(pd.get("zfbTrueName")) || IsNull.paramsIsNull(pd.get("zfbAccount"))) {
				result = "9993";
				message = CommonMessage.CODE_9993;
			} else {
				// 查询当前剩余
				PageData data = doctorService.findById(pd);
				Double remainderMoney = Double.parseDouble((String) data.get("remainderMoney"));
				Double money = Double.parseDouble((String) pd.get("money"));
				// 判断剩余金额是否大于提现金额
				if (remainderMoney < money) {
					// 余额不足
					result = "2222";
					message = "余额不足";
				} else if (money < 5) {
					result = "2221";
					message = "最少提现5元";
				} else {
					pd.put("isSuccess", 1); // 提现状态 0/已成功 1待审批
					pd.put("createTime", DateUtil.getTime());
					int rows = withdrawService.save(pd);
					if (rows > 0) {
						// 成功
						result = "0000";
						message = "已提交申请,请等待";

					} else {
						// 失败
						result = "1111";
						message = "提交失败";
					}
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
}
