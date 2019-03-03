package com.fh.controller.app.clinic;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fh.config.CommonMessage;
import com.fh.controller.app.upload.UploadController;
import com.fh.controller.base.BaseController;
import com.fh.service.app.ClinicService;
import com.fh.service.app.DoctorService;
import com.fh.util.AppUtil;
import com.fh.util.PageData;
import com.fh.util.tools.IsNull;

/**
 * 作者：董雪蕊 类：新版诊所接口
 */

@Controller
@RequestMapping(value = "/api/clinic2")
public class AppClinic2Controller extends BaseController {

	@Resource
	private ClinicService clinicService;
	@Resource
	private DoctorService doctorService;

	/**
	 * 
	 * 描述：给诊所上传环境图片、特色疗法，只有管理员可以
	 * 
	 * @author 董雪蕊
	 * @date 2018年3月4日
	 * @version 2.0
	 * @param request
	 * @param files
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/picUpload", method = RequestMethod.POST)
	@ResponseBody
	public Object picUpload(HttpServletRequest request,
			@RequestParam(value = "files", required = false) MultipartFile[] files,
			@RequestParam(value = "doctorId", required = false) String doctorId,
			@RequestParam(value = "clinicId", required = false) String clinicId,
			@RequestParam(value = "characteristicCure", required = false) String characteristicCure) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		logBefore(logger, "诊所端 设置诊所营业时间");
		String result = "";
		String message = "";
		String pathAll = "";
		PageData pd = new PageData();
		pd.put("doctorId", doctorId);
		pd.put("clinicId", clinicId);
		pd.put("characteristicCure", characteristicCure);
		// 查询用户是否是诊所管理员
		PageData docData = doctorService.isGuan(pd);
		if (IsNull.paramsIsNull(docData) == false) {
			Boolean isTong = true;
			if (files != null && files.length > 0) {
				pathAll = UploadController.picUp(files);
				isTong = UploadController.isTong(pathAll);
			}
			if (isTong) {
				pd.put("environmentPicS", pathAll);
				pd.remove("state");
				clinicService.editClinic(pd);
				result = "0000";
				message = CommonMessage.CODE_0000;// 成功
			} else {
				result = "7003";
				message = CommonMessage.CODE_7003;// 图片上传失败，请检查网络连接！
			}

		} else {
			result = "7004";
			message = CommonMessage.CODE_7004;// 您不是诊所管理员，不能操作！
		}
		map.put("result", result);
		map.put("message", message);
		map.put("pathAll", pathAll);
		logAfter(logger);
		return AppUtil.returnObject(new PageData(), map);
	}

}
