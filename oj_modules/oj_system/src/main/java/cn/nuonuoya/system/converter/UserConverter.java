package cn.nuonuoya.system.converter;

import cn.nuonuoya.system.domain.TbUser;
import cn.nuonuoya.system.dto.UserEditDTO;
import cn.nuonuoya.system.enums.UserSex;
import cn.nuonuoya.system.enums.UserStatus;
import cn.nuonuoya.system.vo.UserVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 用户实体与视图对象转换器
public class UserConverter {

    // 将用户实体转换为VO
    public static UserVO toVO(TbUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setNickName(user.getNickName());
        vo.setHeadImage(user.getHeadImage());
        vo.setSex(user.getSex());
        vo.setSexDesc(UserSex.getDescByValue(user.getSex()));
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setWechat(user.getWechat());
        vo.setQq(user.getQq());
        vo.setSchoolName(user.getSchoolName());
        vo.setMajorName(user.getMajorName());
        vo.setIntroduce(user.getIntroduce());
        vo.setStatus(user.getStatus());
        vo.setStatusDesc(UserStatus.getDescByValue(user.getStatus()));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    // 编辑请求转换为更新实体（去除首尾空白，邮箱统一小写，空值写为空串）
    public static TbUser toEditEntity(UserEditDTO editDTO) {
        TbUser user = new TbUser();
        user.setUserId(editDTO.getUserId());
        user.setNickName(editDTO.getNickName().trim());
        user.setSex(editDTO.getSex());
        user.setPhone(editDTO.getPhone().trim());
        user.setEmail(trimToEmpty(editDTO.getEmail()).toLowerCase());
        user.setWechat(trimToEmpty(editDTO.getWechat()));
        user.setSchoolName(trimToEmpty(editDTO.getSchoolName()));
        user.setMajorName(trimToEmpty(editDTO.getMajorName()));
        user.setIntroduce(trimToEmpty(editDTO.getIntroduce()));
        return user;
    }

    // 去除首尾空白，null 转为空串
    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    // 将用户实体列表批量转换为VO列表
    public static List<UserVO> toVOList(List<TbUser> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserVO> voList = new ArrayList<>(list.size());
        for (TbUser user : list) {
            voList.add(toVO(user));
        }
        return voList;
    }
}
