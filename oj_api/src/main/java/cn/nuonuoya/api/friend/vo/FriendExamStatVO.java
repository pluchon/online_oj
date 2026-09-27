package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 竞赛参与统计
@Getter
@Setter
@ToString
public class FriendExamStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 竞赛ID
    private Long examId;

    // 报名人数
    private Integer enrollCount = 0;

    // 实际提交过代码的人数
    private Integer participantCount = 0;
}
