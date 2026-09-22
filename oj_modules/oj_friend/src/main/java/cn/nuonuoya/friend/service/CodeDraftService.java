package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.vo.CodeDraftVO;

// 做题代码草稿业务
public interface CodeDraftService {

    // 查询当前用户在该题的草稿，没有时返回 null
    CodeDraftVO getDraft(Long questionId);

    // 保存当前用户在该题的草稿（存在则覆盖）
    CodeDraftVO saveDraft(Long questionId, String code);

    // 读取指定用户在该题保存的代码，没有时返回 null
    String getSavedCode(Long userId, Long questionId);
}
