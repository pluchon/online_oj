package cn.nuonuoya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.system.domain.TbQuestionEditorial;
import cn.nuonuoya.system.mapper.QuestionEditorialMapper;
import cn.nuonuoya.system.service.QuestionEditorialService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 题目官方题解业务实现
@Service
public class QuestionEditorialServiceImpl implements QuestionEditorialService {

    @Autowired
    private QuestionEditorialMapper questionEditorialMapper;

    // 查询题目的题解内容
    @Override
    public String getContent(Long questionId) {
        TbQuestionEditorial editorial = findByQuestionId(questionId);
        return editorial == null ? "" : editorial.getContent();
    }

    // 保存题解：空内容删除，已有则覆盖（内容未变时不写库），没有则新增
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Long questionId, String content) {
        String text = StrUtil.trimToEmpty(content);
        TbQuestionEditorial existing = findByQuestionId(questionId);
        if (text.isEmpty()) {
            if (existing != null) {
                questionEditorialMapper.deleteById(existing.getEditorialId());
            }
            return;
        }
        if (existing == null) {
            TbQuestionEditorial editorial = new TbQuestionEditorial();
            editorial.setQuestionId(questionId);
            editorial.setContent(text);
            questionEditorialMapper.insert(editorial);
            return;
        }
        if (!text.equals(existing.getContent())) {
            TbQuestionEditorial update = new TbQuestionEditorial();
            update.setEditorialId(existing.getEditorialId());
            update.setContent(text);
            questionEditorialMapper.updateById(update);
        }
    }

    // 删除题目的题解
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long questionId) {
        questionEditorialMapper.delete(new LambdaQueryWrapper<TbQuestionEditorial>()
                .eq(TbQuestionEditorial::getQuestionId, questionId));
    }

    // 查询题目当前的题解
    private TbQuestionEditorial findByQuestionId(Long questionId) {
        return questionEditorialMapper.selectOne(new LambdaQueryWrapper<TbQuestionEditorial>()
                .eq(TbQuestionEditorial::getQuestionId, questionId));
    }
}
