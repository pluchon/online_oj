package cn.nuonuoya.sentinel.converter;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.Collections;
import java.util.List;

// YAML 规则转换：Nacos 中的规则以 YAML 列表书写（可带注释），字段名与 Sentinel 规则类一致；内容为空时视为没有规则
public abstract class AbstractYamlRuleConverter<T> implements Converter<String, List<T>> {

    // 忽略未知字段，便于在规则里保留说明性字段
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // 规则类型
    private final Class<T> ruleType;

    protected AbstractYamlRuleConverter(Class<T> ruleType) {
        this.ruleType = ruleType;
    }

    // 解析 YAML 列表为规则
    @Override
    public List<T> convert(String source) {
        if (source == null || source.isBlank()) {
            return Collections.emptyList();
        }
        Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(source);
        if (loaded == null) {
            return Collections.emptyList();
        }
        if (!(loaded instanceof List<?>)) {
            throw new IllegalArgumentException("Sentinel 规则必须是 YAML 列表");
        }
        return MAPPER.convertValue(loaded, MAPPER.getTypeFactory().constructCollectionType(List.class, ruleType));
    }
}
