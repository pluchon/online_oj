package cn.nuonuoya.friend.service.impl;

import cn.nuonuoya.friend.cache.ExamCacheManager;
import cn.nuonuoya.friend.converter.ExamConverter;
import cn.nuonuoya.friend.domain.TbExam;
import cn.nuonuoya.friend.dto.ExamQueryDTO;
import cn.nuonuoya.friend.mapper.ExamMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.service.ExamService;
import cn.nuonuoya.friend.vo.ExamVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

// C端竞赛业务实现类
@Service
public class ExamServiceImpl implements ExamService {

    // 已发布状态常量
    private static final int STATUS_PUBLISHED = 1;

    // 未完赛分类标识
    private static final int TYPE_UNFINISH = 0;

    // 历史竞赛分类标识
    private static final int TYPE_HISTORY = 1;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    @Autowired
    private ExamCacheManager examCacheManager;

    // 分页查询竞赛列表实现
    @Override
    public List<ExamVO> list(ExamQueryDTO queryDTO) {
        int targetType = queryDTO.getType() != null && queryDTO.getType() == TYPE_HISTORY ? TYPE_HISTORY : TYPE_UNFINISH;

        // 若无附加搜索条件，走 Redis 缓存快速通道
        boolean hasFilter = StringUtils.hasText(queryDTO.getTitle())
                || StringUtils.hasText(queryDTO.getStartTime())
                || StringUtils.hasText(queryDTO.getEndTime());

        if (!hasFilter) {
            return examCacheManager.getExamList(targetType, queryDTO.getPageNum(), queryDTO.getPageSize());
        }

        // 带有标题或时间区间检索时走数据库查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<TbExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbExam::getStatus, STATUS_PUBLISHED);

        // 标题模糊筛选
        if (StringUtils.hasText(queryDTO.getTitle())) {
            wrapper.like(TbExam::getTitle, queryDTO.getTitle().trim());
        }

        // 时间区间筛选
        if (StringUtils.hasText(queryDTO.getStartTime())) {
            wrapper.ge(TbExam::getStartTime, queryDTO.getStartTime());
        }
        if (StringUtils.hasText(queryDTO.getEndTime())) {
            wrapper.le(TbExam::getEndTime, queryDTO.getEndTime());
        }

        LocalDateTime now = LocalDateTime.now();
        if (targetType == TYPE_UNFINISH) {
            wrapper.gt(TbExam::getEndTime, now);
            wrapper.orderByAsc(TbExam::getStartTime);
        } else {
            wrapper.le(TbExam::getEndTime, now);
            wrapper.orderByDesc(TbExam::getEndTime);
        }

        List<TbExam> examList = examMapper.selectList(wrapper);
        List<ExamVO> voList = ExamConverter.toVOList(examList);
        Long currentUserId = cn.nuonuoya.common.utils.ThreadLocalUtil.get(cn.nuonuoya.common.constants.HttpConstants.USER_ID, Long.class);
        examCacheManager.populateIsEnter(voList, currentUserId);
        return voList;
    }

    // 分页查询未完赛竞赛列表实现
    @Override
    public List<ExamVO> getUnfinishList(ExamQueryDTO queryDTO) {
        queryDTO.setType(TYPE_UNFINISH);
        return list(queryDTO);
    }

    // 分页查询历史竞赛列表实现
    @Override
    public List<ExamVO> getHistoryList(ExamQueryDTO queryDTO) {
        queryDTO.setType(TYPE_HISTORY);
        return list(queryDTO);
    }

    // 报名参加竞赛
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void enroll(cn.nuonuoya.friend.dto.ExamEnrollDTO enrollDTO) {
        Long userId = cn.nuonuoya.common.utils.ThreadLocalUtil.get(cn.nuonuoya.common.constants.HttpConstants.USER_ID, Long.class);
        if (userId == null) {
            throw new cn.nuonuoya.security.exception.ServiceException(cn.nuonuoya.common.enums.ResultCode.FAILED_UNAUTHORIZED);
        }

        Long examId = enrollDTO.getExamId();
        TbExam exam = examMapper.selectById(examId);
        if (exam == null || exam.getStatus() != STATUS_PUBLISHED) {
            throw new cn.nuonuoya.security.exception.ServiceException(cn.nuonuoya.common.enums.ResultCode.FAILED_NOT_EXISTS);
        }

        // 边界校验：已开赛或已结束的竞赛不可报名
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isAfter(exam.getStartTime())) {
            throw new cn.nuonuoya.security.exception.ServiceException(cn.nuonuoya.common.enums.ResultCode.FAILED_EXAM_STARTED_OR_FINISHED);
        }

        // 边界校验：防重复报名
        Long count = userExamMapper.selectCount(new LambdaQueryWrapper<cn.nuonuoya.friend.domain.TbUserExam>()
                .eq(cn.nuonuoya.friend.domain.TbUserExam::getUserId, userId)
                .eq(cn.nuonuoya.friend.domain.TbUserExam::getExamId, examId));
        if (count != null && count > 0) {
            throw new cn.nuonuoya.security.exception.ServiceException(cn.nuonuoya.common.enums.ResultCode.FAILED_USER_EXAM_EXISTS);
        }

        // 插入关联记录
        cn.nuonuoya.friend.domain.TbUserExam userExam = new cn.nuonuoya.friend.domain.TbUserExam();
        userExam.setUserId(userId);
        userExam.setExamId(examId);
        userExamMapper.insert(userExam);

        // 同步写入Redis已报名列表缓存
        examCacheManager.addUserExamCache(userId, examId);
    }

    // 分页查询当前用户已报名的竞赛列表
    @Override
    public List<cn.nuonuoya.friend.vo.UserExamVO> getMyExamList(cn.nuonuoya.common.domain.PageQuery pageQuery) {
        Long userId = cn.nuonuoya.common.utils.ThreadLocalUtil.get(cn.nuonuoya.common.constants.HttpConstants.USER_ID, Long.class);
        if (userId == null) {
            throw new cn.nuonuoya.security.exception.ServiceException(cn.nuonuoya.common.enums.ResultCode.FAILED_UNAUTHORIZED);
        }
        int pageNum = pageQuery != null && pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1;
        int pageSize = pageQuery != null && pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10;
        return examCacheManager.getMyExamList(userId, pageNum, pageSize);
    }
}
