package cn.nuonuoya.system.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.nuonuoya.api.friend.dto.FriendSubmitQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendRejudgePreviewVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitDetailVO;
import cn.nuonuoya.api.friend.vo.FriendSubmitVO;
import cn.nuonuoya.api.judge.enums.JudgeStatusEnum;
import cn.nuonuoya.system.dto.SubmitQueryDTO;
import cn.nuonuoya.system.vo.RejudgeExamVO;
import cn.nuonuoya.system.vo.RejudgePreviewVO;
import cn.nuonuoya.system.vo.SubmitDetailVO;
import cn.nuonuoya.system.vo.SubmitVO;

import java.util.List;

// 提交记录对象模型转换器
public class SubmitConverter {

    // 将管理端查询条件转换为C端查询条件（昵称条件由调用方解析为用户ID）
    public static FriendSubmitQueryDTO toFriendQuery(SubmitQueryDTO queryDTO) {
        return BeanUtil.copyProperties(queryDTO, FriendSubmitQueryDTO.class);
    }

    // 将C端提交记录列表转换为管理端视图（昵称、题目与竞赛标题由调用方补充）
    public static List<SubmitVO> toVOList(List<FriendSubmitVO> sourceList) {
        List<SubmitVO> voList = BeanUtil.copyToList(sourceList, SubmitVO.class);
        voList.forEach(SubmitConverter::fillJudgeStatusDesc);
        return voList;
    }

    // 将C端提交详情转换为管理端详情视图（未通过用例的输入与预期输出由调用方补充）
    public static SubmitDetailVO toDetailVO(FriendSubmitDetailVO source) {
        SubmitDetailVO vo = BeanUtil.copyProperties(source, SubmitDetailVO.class);
        fillJudgeStatusDesc(vo);
        return vo;
    }

    // 将C端重判影响范围转换为管理端视图（竞赛列表元素类型不同，单独转换）
    public static RejudgePreviewVO toPreviewVO(FriendRejudgePreviewVO source) {
        RejudgePreviewVO vo = BeanUtil.copyProperties(source, RejudgePreviewVO.class, "exams");
        vo.setExams(BeanUtil.copyToList(source.getExams(), RejudgeExamVO.class));
        return vo;
    }

    // 按判题结论编码补充描述
    private static void fillJudgeStatusDesc(SubmitVO vo) {
        JudgeStatusEnum status = JudgeStatusEnum.getByCode(vo.getJudgeStatus());
        vo.setJudgeStatusDesc(status == null ? null : status.getDesc());
    }
}
