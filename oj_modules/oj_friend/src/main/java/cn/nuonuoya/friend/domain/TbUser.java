package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// C端普通用户实体
@TableName("tb_user")
@Getter
@Setter
@ToString
public class TbUser extends BaseEntity {

    // 用户id (主键，雪花算法)
    @TableId(value = "USER_ID", type = IdType.ASSIGN_ID)
    private Long userId;

    // 用户昵称
    private String nickName;

    // 用户头像
    private String headImage;

    // 用户性别 0: 保密 1: 男 2: 女
    private Integer sex;

    // 手机号
    private String phone;

    // 邮箱
    private String email;

    // 微信号
    private String wechat;

    // 学校
    private String schoolName;

    // 专业
    private String majorName;

    // 个人介绍
    private String introduce;

    // 用户状态 0: 拉黑 1: 正常
    private Integer status;
}
