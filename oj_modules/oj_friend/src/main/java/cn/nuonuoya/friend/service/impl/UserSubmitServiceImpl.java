package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.api.judge.constants.JudgeMqConstants;
import cn.nuonuoya.api.judge.dto.JudgeRequestDTO;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.UserSubmitDTO;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.UserSubmitService;
import cn.nuonuoya.friend.vo.UserSubmitResultVO;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

// 用户代码提交业务实现类（集成 RabbitMQ 异步判题）
@Slf4j
@Service
public class UserSubmitServiceImpl implements UserSubmitService {

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TokenService tokenService;

    // 获取当前请求登录用户ID（优先ThreadLocal，兼顾HttpServletRequest兜底）
    private Long getCurrentUserId() {
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (userId != null) {
            return userId;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String headerUserId = request.getHeader(HttpConstants.USER_ID);
            if (StrUtil.isNotBlank(headerUserId)) {
                return Convert.toLong(headerUserId);
            }
            String token = request.getHeader(HttpConstants.AUTHENTICATION);
            if (StrUtil.isNotBlank(token)) {
                return tokenService.getUserId(tokenService.cleanToken(token));
            }
        }
        return null;
    }

    // 提交代码、落库初始化记录并向 RabbitMQ 投递异步判题任务
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSubmitResultVO submit(UserSubmitDTO submitDTO) {
        if (submitDTO == null || submitDTO.getQuestionId() == null || StrUtil.isBlank(submitDTO.getUserCode())) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 从认证上下文提取用户身份，严格保障安全性
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        // 查询题目元数据
        TbQuestion question = questionMapper.selectById(submitDTO.getQuestionId());
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }

        // 初始化提交记录（状态设为 2: 评测中）
        TbUserSubmit submit = new TbUserSubmit();
        submit.setUserId(userId);
        submit.setQuestionId(submitDTO.getQuestionId());
        submit.setExamId(submitDTO.getExamId());
        submit.setProgramType(submitDTO.getProgramType());
        submit.setUserCode(submitDTO.getUserCode());
        submit.setPass(2);
        submit.setScore(0);
        submit.setExeMessage("代码已提交，正在沙箱容器中排队评测...");
        submit.setCreateBy(userId);
        submit.setCreateTime(LocalDateTime.now());

        // 持久化落库至 tb_user_submit 表，生成 submitId
        userSubmitMapper.insert(submit);

        // 拼接可编译运行的完整 Java 源代码
        String completeCode = buildCompleteCode(submitDTO.getUserCode(), question.getMainFunc());

        // 组装跨服务判题请求 DTO
        JudgeRequestDTO requestDTO = new JudgeRequestDTO();
        requestDTO.setSubmitId(submit.getSubmitId());
        requestDTO.setUserId(userId);
        requestDTO.setQuestionId(question.getQuestionId());
        requestDTO.setProgramType(submit.getProgramType());
        requestDTO.setUserCode(submit.getUserCode());
        requestDTO.setCompleteCode(completeCode);
        requestDTO.setTimeLimit(question.getTimeLimit() != null ? question.getTimeLimit() : 1000);
        requestDTO.setSpaceLimit(question.getSpaceLimit() != null ? question.getSpaceLimit() : 128);
        requestDTO.setDifficulty(question.getDifficulty());

        // 异步发送至 RabbitMQ 交换机与队列
        try {
            rabbitTemplate.convertAndSend(
                    JudgeMqConstants.JUDGE_EXCHANGE,
                    JudgeMqConstants.JUDGE_ROUTING_KEY,
                    requestDTO
            );
            log.info("成功投递判题消息到 RabbitMQ: submitId = {}, questionId = {}",
                    submit.getSubmitId(), question.getQuestionId());
        } catch (Exception e) {
            log.error("投递 RabbitMQ 判题消息失败, submitId = {}, error: {}", submit.getSubmitId(), e.getMessage(), e);
            submit.setPass(0);
            submit.setExeMessage("系统异常：判题任务队列投递失败");
            userSubmitMapper.updateById(submit);
        }

        // 组装当前评测状态返回给调用方
        UserSubmitResultVO vo = new UserSubmitResultVO();
        vo.setSubmitId(submit.getSubmitId());
        vo.setQuestionId(submit.getQuestionId());
        vo.setExamId(submit.getExamId());
        vo.setProgramType(submit.getProgramType());
        vo.setPass(submit.getPass());
        vo.setExeMessage(submit.getExeMessage());
        vo.setScore(submit.getScore());
        vo.setCreateTime(submit.getCreateTime());
        return vo;
    }

    // 查询提交记录评测结果详情
    @Override
    public UserSubmitResultVO getSubmitResult(Long submitId) {
        if (submitId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        TbUserSubmit submit = userSubmitMapper.selectById(submitId);
        if (submit == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }

        UserSubmitResultVO vo = new UserSubmitResultVO();
        vo.setSubmitId(submit.getSubmitId());
        vo.setQuestionId(submit.getQuestionId());
        vo.setExamId(submit.getExamId());
        vo.setProgramType(submit.getProgramType());
        vo.setPass(submit.getPass());
        vo.setExeMessage(submit.getExeMessage());
        vo.setScore(submit.getScore());
        vo.setCreateTime(submit.getCreateTime());
        return vo;
    }

    // 拼接用户源码与题库主驱动函数生成完整可运行代码
    private String buildCompleteCode(String userCode, String mainFunc) {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.*;\n");
        sb.append("import java.io.*;\n\n");

        if (userCode != null && userCode.contains("class Solution")) {
            // 用户代码已包含类定义，将 mainFunc 嵌入类中
            int lastBraceIndex = userCode.lastIndexOf('}');
            if (lastBraceIndex != -1) {
                sb.append(userCode, 0, lastBraceIndex);
                sb.append("\n\n");
                if (StrUtil.isNotBlank(mainFunc)) {
                    sb.append("    ").append(mainFunc).append("\n");
                }
                sb.append("}\n\n");
            } else {
                sb.append(userCode).append("\n\n");
            }
        } else {
            // 用户代码为纯方法，包装进 Solution 类中
            sb.append("public class Solution {\n\n");
            sb.append(userCode).append("\n\n");
            if (StrUtil.isNotBlank(mainFunc)) {
                sb.append(mainFunc).append("\n\n");
            }
            sb.append("}\n\n");
        }

        // 兼容主函数测试用例调用的 Main 类型引用
        sb.append("class Main extends Solution {}\n");
        return sb.toString();
    }
}
