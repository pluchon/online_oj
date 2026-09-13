package cn.nuonuoya.common.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 分页查询基础请求类
@Getter
@Setter
@ToString
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 当前页码
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码不能小于1")
    private Integer pageNum = 1;

    // 每页条数
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数不能小于1")
    private Integer pageSize = 10;
}
