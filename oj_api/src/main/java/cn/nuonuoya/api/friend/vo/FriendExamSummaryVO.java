package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 一组竞赛的报名与参赛人数
@Getter
@Setter
@ToString
public class FriendExamSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 报名人数（按用户去重）
    private Integer enrollCount;

    // 参赛人数（在这些竞赛里交过代码的用户，去重）
    private Integer participantCount;

    // 每场竞赛各自的人数（没有报名也没有提交的竞赛不返回）
    private List<FriendExamStatVO> exams;
}
