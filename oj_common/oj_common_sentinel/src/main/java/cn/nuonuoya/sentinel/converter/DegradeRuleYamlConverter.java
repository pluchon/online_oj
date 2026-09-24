package cn.nuonuoya.sentinel.converter;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;

// 熔断规则 YAML 转换（在数据源配置 converter-class 中引用）
public class DegradeRuleYamlConverter extends AbstractYamlRuleConverter<DegradeRule> {

    public DegradeRuleYamlConverter() {
        super(DegradeRule.class);
    }
}
