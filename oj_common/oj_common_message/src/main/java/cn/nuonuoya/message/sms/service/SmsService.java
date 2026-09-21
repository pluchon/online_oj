package cn.nuonuoya.message.sms.service;

// 短信服务接口
public interface SmsService {

    // 发送短信验证码，发送成功返回 true
    boolean sendCode(String phone, String code, int expireMin);
}
