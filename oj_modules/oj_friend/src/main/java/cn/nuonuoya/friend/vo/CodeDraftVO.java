package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 代码草稿视图
@Getter
@Setter
@Schema(description = "代码草稿")
public class CodeDraftVO {

    // 代码内容
    @Schema(description = "代码内容")
    private String code;

    // 最后保存时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后保存时间")
    private LocalDateTime savedTime;
}
