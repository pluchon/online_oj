package cn.nuonuoya.friend.converter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.nuonuoya.api.ai.dto.AiQuestionBaseDTO;
import cn.nuonuoya.friend.domain.TbQuestion;

import java.util.Map;

// 题目转换为发给 AI 的题面字段（做题辅导、申诉初审共用）
public class AiQuestionConverter {

    // 名称不同的字段对应关系（defaultCode 同名，自动复制）
    private static final CopyOptions QUESTION_OPTIONS = CopyOptions.create()
            .setFieldMapping(Map.of("title", "questionTitle", "content", "questionContent"));

    private AiQuestionConverter() {
    }

    // 把题面字段填进请求并返回它
    public static <T extends AiQuestionBaseDTO> T fillQuestion(TbQuestion question, T dto) {
        BeanUtil.copyProperties(question, dto, QUESTION_OPTIONS);
        return dto;
    }
}
