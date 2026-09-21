package cn.nuonuoya.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.system.cache.ExamCacheManager;
import cn.nuonuoya.system.converter.ExamConverter;
import cn.nuonuoya.system.domain.SysUser;
import cn.nuonuoya.system.domain.TbExam;
import cn.nuonuoya.system.domain.TbExamQuestion;
import cn.nuonuoya.system.domain.TbQuestion;
import cn.nuonuoya.system.dto.ExamAddDTO;
import cn.nuonuoya.system.dto.ExamDTO;
import cn.nuonuoya.system.dto.ExamEditDTO;
import cn.nuonuoya.system.dto.ExamQuestionAddDTO;
import cn.nuonuoya.system.enums.ExamStatus;
import cn.nuonuoya.system.enums.QuestionDifficulty;
import cn.nuonuoya.system.mapper.ExamMapper;
import cn.nuonuoya.system.mapper.ExamQuestionMapper;
import cn.nuonuoya.system.mapper.QuestionMapper;
import cn.nuonuoya.system.mapper.SysUserMapper;
import cn.nuonuoya.system.service.ExamService;
import cn.nuonuoya.system.utils.TransactionUtils;
import cn.nuonuoya.system.vo.ExamDetailVO;
import cn.nuonuoya.system.vo.ExamVO;
import cn.nuonuoya.system.vo.QuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 竞赛业务实现类
@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamCacheManager examCacheManager;

    // 分页查询竞赛列表实现
    @Override
    public List<ExamVO> list(ExamDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new ExamDTO();
        }
        // 开启 PageHelper 物理分页
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        // 执行联表查询，PageHelper 会自动拦截生成 COUNT 语句与物理 LIMIT 分页
        List<ExamVO> list = examMapper.selectExamList(queryDTO);
        if (CollUtil.isNotEmpty(list)) {
            // 通过业务枚举为视图对象补充发布状态描述文案
            for (ExamVO vo : list) {
                vo.setStatusDesc(ExamStatus.getDescByValue(vo.getStatus()));
            }
        }
        return list;
    }

    // 新增竞赛基本信息实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String add(ExamAddDTO addDTO) {
        // 校验竞赛起止时间合法性
        checkExamTime(addDTO.getStartTime(), addDTO.getEndTime());
        // 校验竞赛名称全局唯一性
        checkExamTitleUnique(addDTO.getTitle(), null);
        // 组装实体，初始发布状态必须为未发布
        TbExam exam = new TbExam();
        exam.setTitle(addDTO.getTitle().trim());
        exam.setStartTime(addDTO.getStartTime());
        exam.setEndTime(addDTO.getEndTime());
        exam.setStatus(ExamStatus.UNPUBLISHED.getValue());
        // 插入记录，MyBatis-Plus 自动回填雪花算法生成的 examId
        examMapper.insert(exam);
        return exam.getExamId().toString();
    }

    // 查询竞赛详情实现
    @Override
    public ExamDetailVO getDetail(Long examId) {
        TbExam exam = getExamById(examId);
        // 查询创建人昵称
        String creatorName = null;
        if (exam.getCreateBy() != null) {
            SysUser user = sysUserMapper.selectById(exam.getCreateBy());
            if (user != null) {
                creatorName = user.getNickName();
            }
        }
        return ExamConverter.toDetailVO(exam, creatorName);
    }

    // 编辑竞赛基本信息实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int edit(ExamEditDTO editDTO) {
        if (editDTO == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验竞赛存在性及未开赛状态
        TbExam exam = checkExamUnstarted(editDTO.getExamId());
        // 校验竞赛起止时间合法性
        checkExamTime(editDTO.getStartTime(), editDTO.getEndTime());
        // 校验竞赛名称唯一性（排除自身）
        checkExamTitleUnique(editDTO.getTitle(), editDTO.getExamId());
        // 更新竞赛基本信息
        TbExam updateExam = new TbExam();
        updateExam.setExamId(editDTO.getExamId());
        updateExam.setTitle(editDTO.getTitle().trim());
        updateExam.setStartTime(editDTO.getStartTime());
        updateExam.setEndTime(editDTO.getEndTime());
        int rows = examMapper.updateById(updateExam);
        // 已发布的竞赛在事务提交后同步更新缓存
        if (isPublished(exam)) {
            Long examId = editDTO.getExamId();
            TransactionUtils.afterCommit(() -> {
                examCacheManager.saveExamDetail(examMapper.selectById(examId));
                examCacheManager.refreshUnfinishList();
            });
        }
        return rows;
    }

    // 删除竞赛实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long examId) {
        // 校验竞赛存在性及未开赛状态
        TbExam exam = checkExamUnstarted(examId);
        // 校验发布状态：已发布的竞赛不允许直接删除，必须先撤销发布
        if (isPublished(exam)) {
            throw new ServiceException(ResultCode.FAILED_EXAM_IS_PUBLISHED);
        }
        // 级联删除关联的题目关系记录
        examQuestionMapper.delete(new LambdaQueryWrapper<TbExamQuestion>()
                .eq(TbExamQuestion::getExamId, examId));
        // 删除竞赛主体记录
        int rows = examMapper.deleteById(examId);
        TransactionUtils.afterCommit(() -> examCacheManager.removeExam(examId));
        return rows;
    }

    // 发布竞赛实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publish(Long examId) {
        // 校验竞赛存在性及未开赛状态
        TbExam exam = checkExamUnstarted(examId);
        // 业务规则校验：未添加题目的竞赛不允许发布
        Long questionCount = examQuestionMapper.selectCount(new LambdaQueryWrapper<TbExamQuestion>()
                .eq(TbExamQuestion::getExamId, examId));
        if (questionCount == null || questionCount == 0) {
            throw new ServiceException(ResultCode.FAILED_EXAM_NOT_ADD_QUESTION);
        }
        TbExam updateExam = new TbExam();
        updateExam.setExamId(examId);
        updateExam.setStatus(ExamStatus.PUBLISHED.getValue());
        int rows = examMapper.updateById(updateExam);
        exam.setStatus(ExamStatus.PUBLISHED.getValue());
        TransactionUtils.afterCommit(() -> {
            examCacheManager.saveExamDetail(exam);
            examCacheManager.refreshUnfinishList();
        });
        return rows;
    }

    // 撤销发布竞赛实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelPublish(Long examId) {
        // 校验竞赛存在性及未开赛状态
        checkExamUnstarted(examId);
        TbExam updateExam = new TbExam();
        updateExam.setExamId(examId);
        updateExam.setStatus(ExamStatus.UNPUBLISHED.getValue());
        int rows = examMapper.updateById(updateExam);
        TransactionUtils.afterCommit(() -> examCacheManager.removeExam(examId));
        return rows;
    }

    // 绑定题目到竞赛实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addQuestion(ExamQuestionAddDTO addDTO) {
        if (addDTO == null || CollUtil.isEmpty(addDTO.getQuestionIds())) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验竞赛存在性及未开赛状态
        checkExamUnstarted(addDTO.getExamId());
        // 查询当前竞赛已绑定的题目列表
        List<TbExamQuestion> existingList = examQuestionMapper.selectList(new LambdaQueryWrapper<TbExamQuestion>()
                .eq(TbExamQuestion::getExamId, addDTO.getExamId())
                .orderByDesc(TbExamQuestion::getQuestionOrder));
        Set<Long> existingQuestionIds = existingList.stream()
                .map(TbExamQuestion::getQuestionId)
                .collect(Collectors.toSet());
        // 过滤出未绑定的待新增题目ID
        List<Long> toAddIds = addDTO.getQuestionIds().stream()
                .distinct()
                .filter(id -> !existingQuestionIds.contains(id))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(toAddIds)) {
            throw new ServiceException(ResultCode.FAILED_EXAM_QUESTION_EXISTS);
        }
        // 待绑定的题目必须全部存在
        Long foundCount = questionMapper.selectCount(new LambdaQueryWrapper<TbQuestion>()
                .in(TbQuestion::getQuestionId, toAddIds));
        if (foundCount == null || foundCount != toAddIds.size()) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        // 获取当前最大排序号
        int currentOrder = existingList.isEmpty() ? 0 : (existingList.get(0).getQuestionOrder() == null ? 0 : existingList.get(0).getQuestionOrder());
        List<TbExamQuestion> batchList = new ArrayList<>(toAddIds.size());
        for (Long questionId : toAddIds) {
            currentOrder++;
            TbExamQuestion eq = new TbExamQuestion();
            eq.setExamId(addDTO.getExamId());
            eq.setQuestionId(questionId);
            eq.setQuestionOrder(currentOrder);
            batchList.add(eq);
        }
        // 使用 MyBatis-Plus 工具类 Db.saveBatch 进行批量插入，避免循环单条写入
        Db.saveBatch(batchList);
        return batchList.size();
    }

    // 查询竞赛关联题目列表实现
    @Override
    public List<QuestionVO> getQuestionList(Long examId) {
        // 校验竞赛存在性
        getExamById(examId);
        List<QuestionVO> list = examQuestionMapper.selectQuestionListByExamId(examId);
        if (CollUtil.isNotEmpty(list)) {
            for (QuestionVO vo : list) {
                vo.setDifficultyDesc(QuestionDifficulty.getDescByValue(vo.getDifficulty()));
            }
        }
        return list;
    }

    // 从竞赛中移除指定题目实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteQuestion(Long examId, Long questionId) {
        if (questionId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        // 校验竞赛存在性及未开赛状态
        TbExam exam = checkExamUnstarted(examId);
        // 业务规则校验：若竞赛已处于发布状态，移除题目后题目数量不能为0
        if (isPublished(exam)) {
            Long count = examQuestionMapper.selectCount(new LambdaQueryWrapper<TbExamQuestion>()
                    .eq(TbExamQuestion::getExamId, examId));
            if (count != null && count <= 1) {
                throw new ServiceException(ResultCode.FAILED_EXAM_NOT_ADD_QUESTION);
            }
        }
        return examQuestionMapper.delete(new LambdaQueryWrapper<TbExamQuestion>()
                .eq(TbExamQuestion::getExamId, examId)
                .eq(TbExamQuestion::getQuestionId, questionId));
    }

    // 判断竞赛是否已发布
    private boolean isPublished(TbExam exam) {
        return Objects.equals(exam.getStatus(), ExamStatus.PUBLISHED.getValue());
    }

    // 根据ID查询竞赛实体并校验存在性
    private TbExam getExamById(Long examId) {
        if (examId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        TbExam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        return exam;
    }

    // 校验竞赛未开赛且未结束并返回竞赛实体
    private TbExam checkExamUnstarted(Long examId) {
        TbExam exam = getExamById(examId);
        LocalDateTime now = LocalDateTime.now();
        if (exam.getEndTime() != null && !now.isBefore(exam.getEndTime())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_IS_FINISHED);
        }
        if (exam.getStartTime() != null && !now.isBefore(exam.getStartTime())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_IS_STARTED);
        }
        return exam;
    }

    // 校验竞赛起止时间合法性
    private void checkExamTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        if (!startTime.isBefore(endTime)) {
            throw new ServiceException(ResultCode.FAILED_EXAM_DATE_ERROR);
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new ServiceException(ResultCode.FAILED_EXAM_START_TIME_ERROR);
        }
    }

    // 校验竞赛名称全局唯一性
    private void checkExamTitleUnique(String title, Long excludeExamId) {
        LambdaQueryWrapper<TbExam> wrapper = new LambdaQueryWrapper<TbExam>()
                .eq(TbExam::getTitle, title.trim());
        if (excludeExamId != null) {
            wrapper.ne(TbExam::getExamId, excludeExamId);
        }
        Long count = examMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException(ResultCode.FAILED_EXAM_EXISTS);
        }
    }

    // 同步预热所有已发布竞赛缓存
    @Override
    public void syncCache() {
        examCacheManager.syncAllExamCache();
    }
}
