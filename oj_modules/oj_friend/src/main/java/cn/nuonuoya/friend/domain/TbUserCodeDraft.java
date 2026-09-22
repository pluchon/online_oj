package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 做题代码草稿实体（每个用户每道题一份）
@TableName("tb_user_code_draft")
@Getter
@Setter
@ToString
public class TbUserCodeDraft extends BaseEntity {

    // 草稿id（雪花算法）
    @TableId(value = "DRAFT_ID", type = IdType.ASSIGN_ID)
    private Long draftId;

    // 用户id
    private Long userId;

    // 题目id
    private Long questionId;

    // 代码内容
    private String code;
}
