package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 申诉 AI 初审结果
@Getter
@Setter
@ToString
public class AiAppealReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 是否可能判错（用例或判题有误）
    private Boolean suspicious;

    // 判断依据（可能涉及隐藏用例，只给管理员看）
    private String analysis;
}
