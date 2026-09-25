package cn.nuonuoya.friend.service.impl;

import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.converter.QuestionEditorialConverter;
import cn.nuonuoya.friend.domain.TbQuestionEditorial;
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

    // 查询题解：题目不存在报错；题目正被进行中的竞赛使用时拒绝（不依赖前端是否带竞赛ID）；没有题解返回 null
    @Override
    public QuestionEditorialVO getEditorial(Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        if (questionMapper.selectById(questionId) == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        if (examService.isQuestionInOngoingExam(questionId)) {
            throw new ServiceException(ResultCode.FAILED_EDITORIAL_IN_EXAM);
        }
        TbQuestionEditorial editorial = questionEditorialMapper.selectOne(new LambdaQueryWrapper<TbQuestionEditorial>()
                .eq(TbQuestionEditorial::getQuestionId, questionId));
        return editorial == null ? null : QuestionEditorialConverter.toVO(editorial);
    }
}
