package cn.nuonuoya.friend.service.impl;

import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.converter.QuestionEditorialConverter;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbQuestionEditorial;
import cn.nuonuoya.friend.enums.QuestionPurposeEnum;
import cn.nuonuoya.friend.mapper.QuestionEditorialMapper;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.service.ExamService;
import cn.nuonuoya.friend.service.QuestionEditorialService;
import cn.nuonuoya.friend.vo.QuestionEditorialVO;
import cn.nuonuoya.security.exception.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 题目官方题解查询服务实现
@Service
public class QuestionEditorialServiceImpl implements QuestionEditorialService {

    @Autowired
    private QuestionEditorialMapper questionEditorialMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamService examService;

    // 查询题解：题目不存在报错；所在竞赛未全部结束的竞赛题、正被进行中竞赛使用的题拒绝（不依赖前端是否带竞赛ID）；没有题解返回 null
    @Override
    public QuestionEditorialVO getEditorial(Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        TbQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        // 竞赛题在所在竞赛全部结束前不提供题解；进行中竞赛的判断保留作兜底
        boolean lockedContest = QuestionPurposeEnum.isContest(question.getPurpose()) && !examService.isQuestionExamsFinished(questionId);
        if (lockedContest || examService.isQuestionInOngoingExam(questionId)) {
            throw new ServiceException(ResultCode.FAILED_EDITORIAL_IN_EXAM);
        }
        TbQuestionEditorial editorial = questionEditorialMapper.selectOne(new LambdaQueryWrapper<TbQuestionEditorial>()
                .eq(TbQuestionEditorial::getQuestionId, questionId));
        return editorial == null ? null : QuestionEditorialConverter.toVO(editorial);
    }
}
