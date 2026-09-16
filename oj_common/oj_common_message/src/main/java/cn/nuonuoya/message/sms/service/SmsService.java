package cn.nuonuoya.message.sms.service;

import java.util.Map;

// 短信服务业务接口
public interface SmsService {

    // 发送默认模板短信验证码
    boolean sendCode(String phone, String code);

    // 发送指定有效期的短信验证码
    boolean sendCode(String phone, String code, int expireMin);

    // 发送指定模板与参数Map的短信
    boolean sendMessage(String phone, String templateCode, Map<String, Object> paramMap);

    // 发送指定模板与JSON参数字符串的短信
    boolean sendMessage(String phone, String templateCode, String templateParamJson);
}
