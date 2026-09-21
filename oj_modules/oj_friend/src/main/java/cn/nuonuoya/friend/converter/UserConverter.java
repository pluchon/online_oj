package cn.nuonuoya.friend.converter;

import cn.nuonuoya.friend.domain.TbUser;
import cn.nuonuoya.friend.vo.UserVO;

// 用户实体转换器
public class UserConverter {

    // 实体转换为个人中心视图对象
    public static UserVO toVO(TbUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setNickName(user.getNickName());
        vo.setHeadImage(user.getHeadImage());
        vo.setSex(user.getSex());

        // 性别枚举转换
        if (Integer.valueOf(1).equals(user.getSex())) {
            vo.setSexDesc("男");
        } else if (Integer.valueOf(2).equals(user.getSex())) {
            vo.setSexDesc("女");
        } else {
            vo.setSexDesc("保密");
        }

        // 手机号掩码脱敏
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setEmail(user.getEmail());
        vo.setWechat(user.getWechat());
        vo.setQq(user.getQq());
        vo.setSchoolName(user.getSchoolName());
        vo.setMajorName(user.getMajorName());
        vo.setIntroduce(user.getIntroduce());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    // 手机号掩码脱敏处理（前3后4，中间保留4位掩码）
    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        String trimmed = phone.trim();
        if (trimmed.length() == 11) {
            return trimmed.substring(0, 3) + "****" + trimmed.substring(7);
        } else if (trimmed.length() >= 7) {
            return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
        }
        return trimmed;
    }
}
