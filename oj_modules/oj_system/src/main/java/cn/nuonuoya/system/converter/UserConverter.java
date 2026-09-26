package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.system.domain.TbUser;
import cn.nuonuoya.system.dto.UserEditDTO;
import cn.nuonuoya.system.enums.UserSex;
import cn.nuonuoya.system.enums.UserStatus;
import cn.nuonuoya.system.vo.UserVO;

import java.util.Collections;
import java.util.List;

// 用户实体与视图对象转换器
public class UserConverter {

    // 将用户实体转换为VO
    public static UserVO toVO(TbUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
        vo.setSexDesc(UserSex.getDescByValue(user.getSex()));
        vo.setStatusDesc(UserStatus.getDescByValue(user.getStatus()));
        return vo;
    }

    // 将用户实体列表批量转换为VO列表
    public static List<UserVO> toVOList(List<TbUser> list) {
        return CollUtil.isEmpty(list) ? Collections.emptyList() : list.stream().map(UserConverter::toVO).toList();
    }

    // 编辑请求转换为更新实体（去除首尾空白，邮箱统一小写，空值写为空串）
    public static TbUser toEditEntity(UserEditDTO editDTO) {
        TbUser user = new TbUser();
        user.setUserId(editDTO.getUserId());
        user.setNickName(editDTO.getNickName().trim());
        user.setSex(editDTO.getSex());
        user.setPhone(editDTO.getPhone().trim());
        user.setEmail(StrUtil.trimToEmpty(editDTO.getEmail()).toLowerCase());
        user.setWechat(StrUtil.trimToEmpty(editDTO.getWechat()));
        user.setSchoolName(StrUtil.trimToEmpty(editDTO.getSchoolName()));
        user.setMajorName(StrUtil.trimToEmpty(editDTO.getMajorName()));
        user.setIntroduce(StrUtil.trimToEmpty(editDTO.getIntroduce()));
        return user;
    }
}
