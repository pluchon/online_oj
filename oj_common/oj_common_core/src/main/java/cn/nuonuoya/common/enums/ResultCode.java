package cn.nuonuoya.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 统一状态码
@Getter
@AllArgsConstructor
public enum ResultCode {
    //操作成功
    SUCCESS (1000, "操作成功"),

    //服务器内部错误，友好提⽰
    ERROR (2000, "服务繁忙请稍后重试"),

    //操作失败，但是服务器不存在异常
    FAILED (3000, "操作失败"),
    FAILED_UNAUTHORIZED (3001, "未授权"),
    FAILED_PARAMS_VALIDATE (3002, "参数校验失败"),
    FAILED_NOT_EXISTS (3003, "资源不存在"),
    FAILED_ALREADY_EXISTS (3004, "资源已存在"),
    FAILED_USER_EXISTS (3101, "⽤⼾已存在"),
    FAILED_USER_NOT_EXISTS (3102, "⽤⼾不存在"),
    FAILED_LOGIN (3103, "⽤⼾名或密码错误"),
    FAILED_USER_BANNED (3104, "您已被列⼊⿊名单, 请联系管理员."),
    FAILED_FREQUENT (3105, "操作过于频繁，请稍后再试"),
    FAILED_SEND_SMS_EXCEED (3106, "今日验证码发送次数已超限，请明日再试"),
    FAILED_SEND_SMS (3107, "短信发送失败，请稍后重试"),
    FAILED_CODE_ERROR (3108, "验证码错误或已失效"),

    // 竞赛业务错误码（32xx）
    FAILED_EXAM_EXISTS (3201, "竞赛已存在"),
    FAILED_EXAM_DATE_ERROR (3202, "竞赛开始时间不能晚于或等于结束时间"),
    FAILED_EXAM_START_TIME_ERROR (3203, "竞赛开始时间不能早于当前时间"),
    FAILED_EXAM_NOT_ADD_QUESTION (3204, "未添加题目的竞赛不允许发布"),
    FAILED_EXAM_IS_STARTED (3205, "竞赛已开赛，不允许操作"),
    FAILED_EXAM_QUESTION_EXISTS (3206, "题目已存在于该竞赛中"),
    FAILED_EXAM_IS_PUBLISHED (3207, "已发布的竞赛不允许直接删除，请先撤销发布"),
    FAILED_EXAM_IS_FINISHED (3208, "竞赛已结束，不允许操作"),
    FAILED_USER_EXAM_EXISTS (3209, "您已报名该竞赛，请勿重复报名"),
    FAILED_EXAM_STARTED_OR_FINISHED (3210, "竞赛已开赛或已结束，无法报名");

    private final int code;
    private final String msg;
}
