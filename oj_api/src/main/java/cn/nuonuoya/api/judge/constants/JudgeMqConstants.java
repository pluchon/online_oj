package cn.nuonuoya.api.judge.constants;

// 判题模块 RabbitMQ 消息队列常量
public class JudgeMqConstants {

    // 判题任务投递交换机
    public static final String JUDGE_EXCHANGE = "oj.judge.exchange";

    // 判题任务路由键
    public static final String JUDGE_ROUTING_KEY = "oj.judge.routing";

    // 判题任务处理队列
    public static final String JUDGE_QUEUE = "oj.judge.queue";

    // 判题结果通知交换机
    public static final String JUDGE_RESULT_EXCHANGE = "oj.judge.result.exchange";

    // 判题结果路由键
    public static final String JUDGE_RESULT_ROUTING_KEY = "oj.judge.result.routing";

    // 判题结果回传队列
    public static final String JUDGE_RESULT_QUEUE = "oj.judge.result.queue";
}
