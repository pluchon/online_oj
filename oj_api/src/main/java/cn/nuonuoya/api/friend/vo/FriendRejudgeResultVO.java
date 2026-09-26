package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 按题重判的投递结果
@Getter
@Setter
@ToString
public class FriendRejudgeResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 本次重新投递判题的提交数
    private Integer queuedCount;

    // 是否因判题队列投递失败而中途停止（失败的那条记为系统错误，其余未处理的保持原结论）
    private Boolean deliverFailed;
}
