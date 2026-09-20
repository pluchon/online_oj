package cn.nuonuoya.api.judge.enums;

import lombok.Getter;

// 判题执行结果状态枚举
@Getter
public enum JudgeStatusEnum {

    // 运行通过，所有测试用例正确且时空指标达标
    AC(1, "Accepted", "运行通过"),

    // 答案错误，程序输出与预期结果不符
    WA(2, "Wrong Answer", "答案错误"),

    // 运行超时，程序执行时长超出题目限制
    TLE(3, "Time Limit Exceeded", "运行超时"),

    // 内存超限，程序占用内存超出空间限制
    MLE(4, "Memory Limit Exceeded", "内存超限"),

    // 编译错误，源码无法通过编译器编译
    CE(5, "Compile Error", "编译错误"),

    // 运行时异常，程序抛出未捕获异常或非零退出
    RE(6, "Runtime Error", "运行异常"),

    // 输出超限，程序输出量超出规定上限
    OLE(7, "Output Limit Exceeded", "输出超限"),

    // 系统错误，沙箱环境异常或容器调度故障
    SE(8, "System Error", "系统错误");

    // 状态编码
    private final Integer code;

    // 状态标准英文标识
    private final String name;

    // 状态中文描述说明
    private final String desc;

    JudgeStatusEnum(Integer code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    // 根据状态码获取对应枚举实例
    public static JudgeStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JudgeStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
