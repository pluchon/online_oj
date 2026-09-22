package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// AI 辅导会话视图（历史消息与快捷操作所需状态）
@Getter
@Setter
@Schema(description = "AI 辅导会话")
public class AiTutorSessionVO {

    // 历史消息（按时间升序）
    @Schema(description = "历史消息")
    private List<AiTutorMessageVO> messages;

    // 每日可提问次数
    @Schema(description = "每日可提问次数")
    private Integer dailyLimit;

    // 今日剩余次数
    @Schema(description = "今日剩余次数")
    private Integer remaining;

    // 本题最近一次已出结果的提交的判题状态（无提交时为空）
    @Schema(description = "最近一次提交的判题状态")
    private Integer latestJudgeStatus;

    // 本题是否已有通过的提交
    @Schema(description = "是否已通过本题")
    private Boolean accepted;

    // 当前是否可用（参加中的竞赛包含本题时不可用）
    @Schema(description = "当前是否可用")
    private Boolean available;
}
