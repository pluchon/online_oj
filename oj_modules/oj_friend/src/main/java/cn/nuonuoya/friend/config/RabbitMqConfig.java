package cn.nuonuoya.friend.config;

import cn.nuonuoya.api.judge.constants.JudgeMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// RabbitMQ 消息队列配置类
@Configuration
public class RabbitMqConfig {

    // 配置通用 JSON 消息转换器
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 声明判题任务直连交换机
    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange(JudgeMqConstants.JUDGE_EXCHANGE, true, false);
    }

    // 声明判题任务队列
    @Bean
    public Queue judgeQueue() {
        return new Queue(JudgeMqConstants.JUDGE_QUEUE, true);
    }

    // 绑定判题任务队列到交换机
    @Bean
    public Binding judgeBinding() {
        return BindingBuilder.bind(judgeQueue()).to(judgeExchange()).with(JudgeMqConstants.JUDGE_ROUTING_KEY);
    }

    // 声明判题结果直连交换机
    @Bean
    public DirectExchange judgeResultExchange() {
        return new DirectExchange(JudgeMqConstants.JUDGE_RESULT_EXCHANGE, true, false);
    }

    // 声明判题结果回传队列
    @Bean
    public Queue judgeResultQueue() {
        return new Queue(JudgeMqConstants.JUDGE_RESULT_QUEUE, true);
    }

    // 绑定判题结果队列到结果交换机
    @Bean
    public Binding judgeResultBinding() {
        return BindingBuilder.bind(judgeResultQueue()).to(judgeResultExchange()).with(JudgeMqConstants.JUDGE_RESULT_ROUTING_KEY);
    }
}
