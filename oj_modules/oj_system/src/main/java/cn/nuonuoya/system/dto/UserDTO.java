package cn.nuonuoya.system.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 用户列表分页查询DTO
@Getter
@Setter
@ToString
@Schema(description = "用户列表分页查询参数")
public class UserDTO extends PageQuery {

    // 用户ID（精确查询）
    @Schema(description = "用户ID（精确查询）")
    private Long userId;

    // 用户昵称（模糊查询）
    @Schema(description = "用户昵称（模糊查询）")
    private String nickName;
}
