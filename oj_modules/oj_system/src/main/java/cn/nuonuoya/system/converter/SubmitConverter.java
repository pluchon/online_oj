package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.system.vo.RejudgeExamVO;
import cn.nuonuoya.system.vo.RejudgePreviewVO;

// 按题重判对象转换器
public class SubmitConverter {

    private SubmitConverter() {
    }

    // 将C端重判影响范围转换为管理端视图（竞赛列表元素类型不同，单独转换）
    public static RejudgePreviewVO toPreviewVO(FriendRejudgePreviewVO source) {
        RejudgePreviewVO vo = BeanUtil.copyProperties(source, RejudgePreviewVO.class, "exams");
        vo.setExams(BeanUtil.copyToList(source.getExams(), RejudgeExamVO.class));
        return vo;
    }
}
