package com.fh.controller.app.weixin;

import java.util.Map;

import com.fh.util.PageData;

public class Template {
	/**
	 * 支持模板 1 模板ID EEbJgkBXtJ-n5QRBdMN6Z5A5HWY4JDouC6QqtAFuMWg 标题 挂号取消通知 关键词 挂号时间
	 * {{keyword1.DATA}} 挂号人 {{keyword2.DATA}} 就诊地点 {{keyword3.DATA}} 就诊时间
	 * {{keyword4.DATA}} 取消时间 {{keyword5.DATA}} 取消原因 {{keyword6.DATA}}
	 * 
	 * 支持模板 2 模板ID -96Uzp0CKdgdNZMvVl3uqz6VEoGuCP04dgUtG5r4dV8 标题 挂号成功通知 关键词 挂号时间
	 * {{keyword1.DATA}} 就诊人 {{keyword2.DATA}} 就诊地点 {{keyword3.DATA}} 就诊时间
	 * {{keyword4.DATA}} 诊所名称 {{keyword5.DATA}}
	 * 
	 * 支持模板 3 模板ID MpM-3sOXk_iZN5pw6tBdGwGjF9LFnjOTtcZ0ZgRPEro 标题 日程提醒 关键词 日程时间
	 * {{keyword1.DATA}} 日程主题 {{keyword2.DATA}}
	 */

	private Map<String, PageData> data;// 模板内容，不填则下发空模板
	private String touser;// 是 接收者（用户）的 openid
	private String template_id;// 所需下发的模板消息的id
	private String page;// 点击模板卡片后的跳转页面，仅限本小程序内的页面。支持带参数,（示例index?foo=bar）。该字段不填则模板无跳转。
	private String form_id;// 表单提交场景下，为 submit 事件带上的 formId；支付场景下，为本次支付的 prepay_id
	private String color;// 模板内容字体的颜色，不填默认黑色
	private String emphasis_keyword;// 模板需要放大的关键词，不填则默认无放大

	public Template(Map<String, PageData> data, String touser, String template_id, String page, String form_id,
			String color, String emphasis_keyword) {
		super();
		this.data = data;
		this.touser = touser;
		this.template_id = template_id;
		this.page = page;
		this.form_id = form_id;
		this.color = color;
		this.emphasis_keyword = emphasis_keyword;
	}

	public Template() {
		super();
	}

	public Map<String, PageData> getData() {
		return data;
	}

	public void setData(Map<String, PageData> data) {
		this.data = data;
	}

	public String getTouser() {
		return touser;
	}

	public void setTouser(String touser) {
		this.touser = touser;
	}

	public String getTemplate_id() {
		return template_id;
	}

	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public String getForm_id() {
		return form_id;
	}

	public void setForm_id(String form_id) {
		this.form_id = form_id;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getEmphasis_keyword() {
		return emphasis_keyword;
	}

	public void setEmphasis_keyword(String emphasis_keyword) {
		this.emphasis_keyword = emphasis_keyword;
	}

}
