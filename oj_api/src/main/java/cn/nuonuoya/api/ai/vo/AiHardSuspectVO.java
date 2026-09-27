package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// AI 对一道可疑题的判断
@Getter
@Setter
@ToString
public class AiHardSuspectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 可疑题序号（与请求中的 index 对应）
    private Integer index;

    // 一句话判断：更可能是用例或题面有误，还是学员的普遍错误，以及依据
    private String comment;
}
