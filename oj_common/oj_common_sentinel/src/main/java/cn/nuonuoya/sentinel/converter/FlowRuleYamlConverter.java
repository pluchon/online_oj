package cn.nuonuoya.sentinel.converter;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;

// 限流规则 YAML 转换（在数据源配置 converter-class 中引用）
public class FlowRuleYamlConverter extends AbstractYamlRuleConverter<FlowRule> {

    public FlowRuleYamlConverter() {
        super(FlowRule.class);
    }
}
