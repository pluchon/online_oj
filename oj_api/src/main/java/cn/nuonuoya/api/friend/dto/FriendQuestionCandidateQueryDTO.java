package cn.nuonuoya.api.friend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 候选题目检索条件
@Getter
@Setter
@ToString
public class FriendQuestionCandidateQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 检索词（主题关键词或描述，可为空）
    private String query;

    // 难度（1:简单 2:中等 3:困难）
    private Integer difficulty;

    // 需要的候选数量
    private Integer size;

    // 需要排除的题目
    private List<Long> excludeIds;
}
