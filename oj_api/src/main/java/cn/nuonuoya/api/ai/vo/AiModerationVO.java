package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 内容审核结论
@Getter
@Setter
@ToString
public class AiModerationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 是否通过
    private Boolean pass;

    // 不通过时的违规类别（中文，如"色情低俗"），通过时为空
    private String category;
}
