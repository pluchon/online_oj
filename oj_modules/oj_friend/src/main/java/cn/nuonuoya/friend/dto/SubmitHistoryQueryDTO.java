package cn.nuonuoya.friend.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 本题提交记录分页查询DTO
@Getter
@Setter
@Schema(description = "本题提交记录分页查询DTO")
public class SubmitHistoryQueryDTO extends PageQuery {

    // 题目ID
    @NotNull(message = "题目ID不能为空")
    @Schema(description = "题目ID")
    private Long questionId;
}
