package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 运行示例用例结果视图对象
@Getter
@Setter
@Schema(description = "运行示例用例结果视图对象")
public class QuestionRunResultVO {

    // 判题状态（1:AC 2:WA 3:TLE 4:MLE 5:CE 6:RE 8:SE）
    @Schema(description = "判题状态 1:AC 2:WA 3:TLE 4:MLE 5:CE 6:RE 8:SE")
    private Integer status;

    // 通过用例数
    @Schema(description = "通过用例数")
    private Integer passCount;

    // 总用例数
    @Schema(description = "总用例数")
    private Integer totalCount;

    // 执行耗时（毫秒）
    @Schema(description = "执行耗时(ms)")
    private Long timeCost;

    // 编译错误、运行异常等原始输出
    @Schema(description = "错误信息")
    private String exeMessage;

    // 逐用例结果
    @Schema(description = "逐用例结果")
    private List<CaseResultVO> caseResults;
}
