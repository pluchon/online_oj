package cn.nuonuoya.system.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目标签实体
@TableName("tb_tag")
@Getter
@Setter
@ToString
public class TbTag extends BaseEntity {

    // 标签ID（雪花算法）
    @TableId(value = "TAG_ID", type = IdType.ASSIGN_ID)
    private Long tagId;

    // 标签名称
    private String tagName;

    // 标签分类（见 TagCategory）
    private Integer category;

    // 逻辑删除标识（0: 正常 1: 已删除）
    @TableLogic
    private Integer deleteState;
}
