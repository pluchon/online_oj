package cn.nuonuoya.common.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 创建人（插入时自动填充）
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    // 创建时间（插入时自动填充）
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 更新人（更新时自动填充）
    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;

    // 更新时间（更新时自动填充）
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}