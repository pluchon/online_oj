package cn.nuonuoya.system.service;

import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.system.dto.SubmitQueryDTO;
import cn.nuonuoya.system.vo.RejudgePreviewVO;
import cn.nuonuoya.system.vo.SubmitDetailVO;
import cn.nuonuoya.system.vo.SubmitVO;

// 提交记录管理业务接口（提交记录归 oj-friend，本服务通过内部接口查询与重判）
public interface SubmitService {

    // 分页查询提交记录，补充用户昵称、题目与竞赛标题
    TableDataResult<SubmitVO> list(SubmitQueryDTO queryDTO);

    // 查询单条提交详情（含代码与首个未通过用例）
    SubmitDetailVO getDetail(Long submitId);

    // 预览按题重判的影响范围
    RejudgePreviewVO previewRejudge(Long questionId);

    // 按题重判，返回本次投递判题的条数
    int rejudge(Long questionId);
}
