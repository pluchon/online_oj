package cn.nuonuoya.api.ai.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 竞赛选题结果（候选序号，按由易到难排列）
@Getter
@Setter
@ToString
public class AiExamSelectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 选中的候选序号
    private List<Integer> indexes;
}
