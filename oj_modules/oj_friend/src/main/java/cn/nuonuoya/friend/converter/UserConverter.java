package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.friend.domain.TbUser;
import cn.nuonuoya.friend.enums.UserSexEnum;
import cn.nuonuoya.friend.vo.UserVO;

// 用户实体转换器
public class UserConverter {

    // 实体转换为个人中心视图对象（手机号脱敏）
    public static UserVO toVO(TbUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
        vo.setSexDesc(UserSexEnum.getDescByCode(user.getSex()));
        vo.setPhone(maskPhone(user.getPhone()));
        return vo;
    }

    // 手机号脱敏（前3后4，中间用 * 代替；空值返回空串），展示与日志共用
    public static String maskPhone(String phone) {
        return DesensitizedUtil.mobilePhone(StrUtil.trim(phone));
    }
}
