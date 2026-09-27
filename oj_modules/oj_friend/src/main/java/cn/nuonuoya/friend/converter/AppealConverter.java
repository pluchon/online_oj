package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.friend.vo.FriendAppealDetailVO;
import cn.nuonuoya.api.friend.vo.FriendCaseResultVO;
import cn.nuonuoya.friend.domain.TbSubmitAppeal;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import com.alibaba.fastjson2.JSON;

import java.util.Collections;
import java.util.List;

// 申诉对象转换器
public class AppealConverter {

    private AppealConverter() {
    }

    // 申诉与被申诉的提交合并为详情（提交时间字段名不同，单独赋值）
    public static FriendAppealDetailVO toDetailVO(TbSubmitAppeal appeal, TbUserSubmit submit) {
        FriendAppealDetailVO vo = BeanUtil.copyProperties(appeal, FriendAppealDetailVO.class);
        if (submit != null) {
            BeanUtil.copyProperties(submit, vo, "createTime", "createBy", "updateTime", "updateBy", "examId", "questionId", "userId", "submitId");
            vo.setSubmitTime(submit.getCreateTime());
            vo.setCaseResults(parseCaseResults(submit.getCaseOutputs()));
        }
        return vo;
    }

    // 解析提交表里的逐用例结果 JSON，没有记录时返回空列表
    public static List<FriendCaseResultVO> parseCaseResults(String caseOutputs) {
        return StrUtil.isBlank(caseOutputs) ? Collections.emptyList() : JSON.parseArray(caseOutputs, FriendCaseResultVO.class);
    }
}
