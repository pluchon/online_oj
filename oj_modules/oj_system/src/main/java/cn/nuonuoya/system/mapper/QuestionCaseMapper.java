package cn.nuonuoya.system.mapper;

import cn.nuonuoya.system.domain.TbQuestionCase;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

// 题目测试用例数据访问接口
@Mapper
public interface QuestionCaseMapper extends BaseMapper<TbQuestionCase> {

    // 按判题顺序查询题目的全部用例（公开示例在前，再按排序号与主键；与 oj-friend 判题时的顺序一致）
    default List<TbQuestionCase> selectJudgeOrdered(Long questionId) {
        return selectList(new LambdaQueryWrapper<TbQuestionCase>()
                .eq(TbQuestionCase::getQuestionId, questionId)
                .orderByDesc(TbQuestionCase::getIsSample)
                .orderByAsc(TbQuestionCase::getSortOrder, TbQuestionCase::getCaseId));
    }
}
