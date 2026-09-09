package cn.nuonuoya.system.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 管理端用户实体
@TableName("tb_sys_user")
@Getter
@Setter
@ToString
public class SysUser extends BaseEntity {

    // 用户id
    @TableId(value = "USER_ID", type = IdType.ASSIGN_ID)
    private Long userId;

    // 用户账号
    private String userAccount;

    // 用户密码
    private String password;

    // 用户昵称
    private String nickName;
}
