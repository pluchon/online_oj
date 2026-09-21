package cn.nuonuoya.friend.converter;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.vo.SubmitHistoryVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 用户提交记录对象模型转换器
public class UserSubmitConverter {

    // 将提交记录实体列表转换为本题提交记录视图列表
    public static List<SubmitHistoryVO> toHistoryVOList(List<TbUserSubmit> submitList) {
        if (CollUtil.isEmpty(submitList)) {
            return Collections.emptyList();
        }
        List<SubmitHistoryVO> voList = new ArrayList<>(submitList.size());
        for (TbUserSubmit submit : submitList) {
            SubmitHistoryVO vo = new SubmitHistoryVO();
            vo.setSubmitId(submit.getSubmitId());
            vo.setPass(submit.getPass());
            vo.setStatus(submit.getJudgeStatus());
            vo.setPassCount(submit.getPassCount());
            vo.setTotalCount(submit.getTotalCount());
            vo.setTimeCost(submit.getTimeCost());
            vo.setScore(submit.getScore());
            vo.setUserCode(submit.getUserCode());
            vo.setCreateTime(submit.getCreateTime());
            voList.add(vo);
        }
        return voList;
    }
}
