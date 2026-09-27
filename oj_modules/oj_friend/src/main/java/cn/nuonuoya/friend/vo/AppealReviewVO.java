package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// AI 初审结果视图对象（文案由后端按结论选定，不含 AI 原文，避免泄露隐藏用例）
@Getter
@Setter
@Schema(description = "AI 初审结果")
public class AppealReviewVO {

    // 是否可以提交正式申诉
    @Schema(description = "是否可以提交正式申诉")
    private Boolean allowed;

    // 给学员看的说明
    @Schema(description = "给学员看的说明")
    private String message;

    // 今日剩余次数
    @Schema(description = "今日剩余次数")
    private AppealQuotaVO quota;
}
