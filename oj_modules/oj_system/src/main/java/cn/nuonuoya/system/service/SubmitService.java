package cn.nuonuoya.system.service;

import cn.nuonuoya.system.vo.RejudgePreviewVO;

// 按题重判业务接口（提交记录归 oj-friend，本服务在管理员修改用例后发起重判）
public interface SubmitService {

    // 预览按题重判的影响范围
    RejudgePreviewVO previewRejudge(Long questionId);

    // 按题重判，返回本次投递判题的条数
    int rejudge(Long questionId);
}
