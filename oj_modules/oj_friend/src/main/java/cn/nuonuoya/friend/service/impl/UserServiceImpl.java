package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.friend.constants.FriendCacheConstants;
import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.enums.UserIdentity;
import cn.nuonuoya.friend.cache.UserCacheManager;
import cn.nuonuoya.friend.domain.TbQuestion;
import cn.nuonuoya.friend.domain.TbUser;
import cn.nuonuoya.friend.domain.TbUserExam;
import cn.nuonuoya.friend.domain.TbUserSubmit;
import cn.nuonuoya.friend.dto.UserCalendarQueryDTO;
import cn.nuonuoya.friend.dto.UserLoginDTO;
import cn.nuonuoya.friend.dto.UserOverviewQueryDTO;
import cn.nuonuoya.friend.dto.UserProfileUpdateDTO;
import cn.nuonuoya.friend.dto.UserSendCodeDTO;
import cn.nuonuoya.friend.enums.QuestionDifficultyEnum;
import cn.nuonuoya.friend.enums.SubmitPassEnum;
import cn.nuonuoya.friend.enums.TimeRangeEnum;
import cn.nuonuoya.friend.enums.UserSexEnum;
import cn.nuonuoya.friend.enums.UserStatusEnum;
import cn.nuonuoya.friend.mapper.QuestionMapper;
import cn.nuonuoya.friend.mapper.UserExamMapper;
import cn.nuonuoya.friend.mapper.UserMapper;
import cn.nuonuoya.friend.mapper.UserSubmitMapper;
import cn.nuonuoya.friend.service.OssService;
import cn.nuonuoya.friend.client.AiModerationClient;
import cn.nuonuoya.api.ai.vo.AiModerationVO;
import org.springframework.http.MediaType;
import cn.nuonuoya.friend.service.UserService;
import cn.nuonuoya.friend.vo.UserAbilityRadarVO;
import cn.nuonuoya.friend.vo.UserCalendarItemVO;
import cn.nuonuoya.friend.vo.UserCalendarVO;
import cn.nuonuoya.friend.vo.UserOverviewVO;
import cn.nuonuoya.friend.vo.UserVO;
import cn.nuonuoya.security.utils.SecurityUtils;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.message.sms.config.SmsProperties;
import cn.nuonuoya.message.sms.service.SmsService;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.security.service.TokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// C端用户业务实现类
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    @Autowired
    private SmsService smsService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SmsProperties smsProperties;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserCacheManager userCacheManager;

    @Autowired
    private OssService ossService;

    // AI 内容审核
    @Autowired
    private AiModerationClient aiModerationClient;

    // 发送短信验证码具体实现
    @Override
    public void sendCode(UserSendCodeDTO sendCodeDTO) {
        String phone = sendCodeDTO.getPhone();
        // 1. 手机号防守校验
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        String intervalKey = FriendCacheConstants.SMS_CODE_INTERVAL_KEY + phone;
        String countKey = FriendCacheConstants.SMS_CODE_COUNT_KEY + phone;
        String codeKey = FriendCacheConstants.SMS_CODE_KEY + phone;

        // 2. 校验发送频率（防刷冷却，默认60秒）
        if (Boolean.TRUE.equals(redisService.hasKey(intervalKey))) {
            Long expire = redisService.getExpire(intervalKey, TimeUnit.SECONDS);
            long seconds = (expire != null && expire > 0) ? expire : 60;
            throw new ServiceException(ResultCode.FAILED_FREQUENT, "操作过于频繁，请在 " + seconds + " 秒后再试");
        }

        // 3. 校验单手机号单日累计发送上限
        int maxDailyCount = smsProperties.getMaxDailyCount() != null ? smsProperties.getMaxDailyCount() : 10;
        Integer currentCount = redisService.getCacheObject(countKey, Integer.class);
        if (currentCount != null && currentCount >= maxDailyCount) {
            throw new ServiceException(ResultCode.FAILED_SEND_SMS_EXCEED);
        }

        // 4. 生成 6 位随机数字验证码
        String code = RandomUtil.randomNumbers(6);

        // 5. 调用阿里云短信服务发送验证码（支持通过 isConfirm 开关控制真实发码与模拟发码，节省短信资费）
        int expireMin = smsProperties.getExpireMin() != null ? smsProperties.getExpireMin() : 5;
        boolean isConfirm = smsProperties.getIsConfirm() == null || smsProperties.getIsConfirm();

        boolean sendSuccess;
        if (isConfirm) {
            // 真实调用阿里云短信服务
            sendSuccess = smsService.sendCode(phone, code, expireMin);
        } else {
            // 模拟发码模式：不发短信，在日志中输出验证码供本地登录使用（真实发码模式绝不输出验证码）
            log.info("[模拟发码] 手机号: {}, 验证码: {}（有效期 {} 分钟，未真实发送短信）", maskPhone(phone), code, expireMin);
            sendSuccess = true;
        }

        if (!sendSuccess) {
            throw new ServiceException(ResultCode.FAILED_SEND_SMS);
        }

        // 6. 短信发送成功后设置过期时间的时机
        // 6.1 缓存验证码主体，设置有效期（默认5分钟）
        redisService.setCacheObject(codeKey, code, (long) expireMin, TimeUnit.MINUTES);

        // 6.2 记录防刷冷却标记（默认60秒冷却）
        long intervalSeconds = smsProperties.getIntervalSeconds() != null ? smsProperties.getIntervalSeconds() : 60L;
        redisService.setCacheObject(intervalKey, "1", intervalSeconds, TimeUnit.SECONDS);

        // 6.3 累计当日发送次数，首次发码设置TTL至当天 23:59:59
        if (currentCount == null) {
            redisService.setCacheObject(countKey, 1);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime midnight = now.plusDays(1).with(LocalTime.MIN);
            long secondsToMidnight = Duration.between(now, midnight).getSeconds();
            redisService.expire(countKey, secondsToMidnight, TimeUnit.SECONDS);
        } else {
            redisService.increment(countKey);
        }

    }

    // 用户短信验证码登录与自动注册实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String login(UserLoginDTO loginDTO) {
        String phone = loginDTO.getPhone();
        String code = loginDTO.getCode();

        // 1. 参数防御校验
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 2. 校验短信验证码（比对 Redis 中缓存的验证码）
        String codeKey = FriendCacheConstants.SMS_CODE_KEY + phone;
        String cachedCode = redisService.getCacheObject(codeKey, String.class);
        if (!StringUtils.hasText(cachedCode) || !cachedCode.equals(code.trim())) {
            throw new ServiceException(ResultCode.FAILED_CODE_ERROR);
        }

        // 验证通过后立即物理删除 Redis 中的验证码，杜绝重放攻击
        redisService.deleteObject(codeKey);

        // 3. 根据手机号查询用户是否存在
        TbUser user = userMapper.selectOne(
                new LambdaQueryWrapper<TbUser>().eq(TbUser::getPhone, phone)
        );

        // 4. 新老用户分流与自动注册
        if (user == null) {
            // 新用户：执行自动注册入库
            user = registerNewUser(phone);
        }

        // 5. 组装用户信息生成 JWT Token 并存入 Redis 会话
        String token = generateUserToken(user);
        return token;
    }

    // 自动注册新用户
    private TbUser registerNewUser(String phone) {
        TbUser newUser = new TbUser();
        newUser.setPhone(phone);
        // 初始化默认昵称，例如：用户_后4位_随机4位
        String phoneTail = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        newUser.setNickName("用户_" + phoneTail + "_" + RandomUtil.randomString(4));
        newUser.setHeadImage("");
        newUser.setSex(UserSexEnum.SECRET.getCode());
        newUser.setStatus(UserStatusEnum.NORMAL.getCode());
        userMapper.insert(newUser);
        return newUser;
    }

    // 抽取新老用户公共逻辑：生成 JWT Token 并建立 Redis 会话
    private String generateUserToken(TbUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setIdentity(UserIdentity.ORDINARY.getValue());
        loginUser.setNickName(user.getNickName());
        return tokenService.createToken(user.getUserId(), loginUser);
    }

    // 当前用户退出登录（销毁服务端会话）
    @Override
    public void logout() {
        tokenService.deleteLoginUserByKey(SecurityUtils.getUserKey());
    }

    // 获取当前登录用户个人资料
    @Override
    public UserVO getUserProfile() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            log.warn("[个人中心] 当前上下文 userId 为空");
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        UserVO vo = userCacheManager.getUserById(userId);
        if (vo == null) {
            log.warn("[个人中心] 数据库未查到用户记录, userId: {}", userId);
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return vo;
    }

    // 更新当前登录用户个人资料
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserProfile(UserProfileUpdateDTO updateDTO) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        TbUser existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        // 昵称与个人介绍有变化时先做内容审核
        List<String> changedTexts = new ArrayList<>();
        String nickName = updateDTO.getNickName().trim();
        String introduce = updateDTO.getIntroduce() != null ? updateDTO.getIntroduce().trim() : "";
        if (!nickName.equals(existingUser.getNickName())) {
            changedTexts.add(nickName);
        }
        if (StringUtils.hasText(introduce) && !introduce.equals(existingUser.getIntroduce())) {
            changedTexts.add(introduce);
        }
        if (!changedTexts.isEmpty()) {
            rejectIfViolated(aiModerationClient.moderateText(changedTexts), "昵称或个人介绍");
        }

        // 构造更新实体
        TbUser updateEntity = new TbUser();
        updateEntity.setUserId(userId);
        updateEntity.setNickName(nickName);
        if (StringUtils.hasText(updateDTO.getHeadImage())) {
            updateEntity.setHeadImage(updateDTO.getHeadImage().trim());
        }
        updateEntity.setSex(updateDTO.getSex());
        updateEntity.setEmail(updateDTO.getEmail() != null ? updateDTO.getEmail().trim() : "");
        updateEntity.setWechat(updateDTO.getWechat() != null ? updateDTO.getWechat().trim() : "");
        updateEntity.setQq(updateDTO.getQq() != null ? updateDTO.getQq().trim() : "");
        updateEntity.setSchoolName(updateDTO.getSchoolName() != null ? updateDTO.getSchoolName().trim() : "");
        updateEntity.setMajorName(updateDTO.getMajorName() != null ? updateDTO.getMajorName().trim() : "");
        updateEntity.setIntroduce(updateDTO.getIntroduce() != null ? updateDTO.getIntroduce().trim() : "");
        userMapper.updateById(updateEntity);

        // 主动剔除用户详情缓存，保证下次获取最新数据
        userCacheManager.deleteUserCache(userId);

        // 若用户昵称发生变更，同步刷新Redis会话中的登录用户信息
        String userKey = SecurityUtils.getUserKey();
        LoginUser loginUser = tokenService.getLoginUserByKey(userKey);
        if (loginUser != null) {
            loginUser.setNickName(updateDTO.getNickName().trim());
            tokenService.updateLoginUser(userKey, loginUser);
        }

    }

    // 上传当前登录用户头像至OSS并更新资料
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(MultipartFile file) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        TbUser existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        // 校验文件后先做图片审核，再上传至OSS
        ossService.validateAvatar(file);
        rejectIfViolated(moderateAvatar(file), "头像");
        String avatarUrl = ossService.uploadAvatar(file);

        // 同步持久化至用户表
        TbUser updateEntity = new TbUser();
        updateEntity.setUserId(userId);
        updateEntity.setHeadImage(avatarUrl);
        userMapper.updateById(updateEntity);

        // 主动剔除用户详情缓存
        userCacheManager.deleteUserCache(userId);

        return avatarUrl;
    }

    // 获取当前登录用户数据总览统计（支持时间范围筛选）
    @Override
    public UserOverviewVO getUserOverview(UserOverviewQueryDTO queryDTO) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        // 1. 解析时间范围起点
        String rangeCode = queryDTO != null ? queryDTO.getTimeRange() : "all";
        TimeRangeEnum rangeEnum = TimeRangeEnum.of(rangeCode);
        LocalDateTime startTime = null;
        LocalDateTime now = LocalDateTime.now();

        switch (rangeEnum) {
            case WEEK -> startTime = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
            case MONTH -> startTime = now.minusMonths(1);
            case YEAR -> startTime = now.minusYears(1);
            case ALL -> startTime = null;
        }

        // 2. 查询用户在时间范围内的代码提交记录
        LambdaQueryWrapper<TbUserSubmit> submitWrapper = new LambdaQueryWrapper<TbUserSubmit>()
                .eq(TbUserSubmit::getUserId, userId);
        if (startTime != null) {
            submitWrapper.ge(TbUserSubmit::getCreateTime, startTime);
        }
        List<TbUserSubmit> submits = userSubmitMapper.selectList(submitWrapper);

        UserOverviewVO overviewVO = new UserOverviewVO();
        if (CollUtil.isEmpty(submits)) {
            overviewVO.setSolvedCount(0);
            overviewVO.setTryingCount(0);
            overviewVO.setSubmitCount(0);
            overviewVO.setPassRate("0%");
            UserAbilityRadarVO emptyRadar = new UserAbilityRadarVO();
            emptyRadar.setDataStructure(0);
            emptyRadar.setAlgorithm(0);
            emptyRadar.setImplementation(0);
            emptyRadar.setMath(0);
            emptyRadar.setCompetition(0);
            overviewVO.setRadarScores(emptyRadar);
            overviewVO.setAbilityRadar(emptyRadar);
            return overviewVO;
        }

        // 3. 统计提交与解题概况
        int submitCount = submits.size();
        long passSubmits = submits.stream().filter(s -> SubmitPassEnum.PASS.getCode().equals(s.getPass())).count();
        String passRate = Math.round((double) passSubmits * 100.0 / submitCount) + "%";

        Set<Long> solvedQuestionIds = new HashSet<>();
        Set<Long> attemptedQuestionIds = new HashSet<>();
        for (TbUserSubmit submit : submits) {
            Long qId = submit.getQuestionId();
            if (qId != null) {
                attemptedQuestionIds.add(qId);
                if (SubmitPassEnum.PASS.getCode().equals(submit.getPass())) {
                    solvedQuestionIds.add(qId);
                }
            }
        }
        int solvedCount = solvedQuestionIds.size();
        int tryingCount = Math.max(0, attemptedQuestionIds.size() - solvedCount);

        overviewVO.setSolvedCount(solvedCount);
        overviewVO.setTryingCount(tryingCount);
        overviewVO.setSubmitCount(submitCount);
        overviewVO.setPassRate(passRate);

        // 4. 计算学员五维能力模型雷达图得分
        UserAbilityRadarVO radarVO = calculateAbilityRadar(userId, startTime, submits, solvedQuestionIds, passSubmits);
        overviewVO.setRadarScores(radarVO);
        overviewVO.setAbilityRadar(radarVO);

        return overviewVO;
    }

    // 评估学员五维能力模型雷达图得分（分值 0~100）
    private UserAbilityRadarVO calculateAbilityRadar(Long userId, LocalDateTime startTime, List<TbUserSubmit> submits, Set<Long> solvedQuestionIds, long passSubmits) {
        UserAbilityRadarVO radar = new UserAbilityRadarVO();
        int submitCount = submits.size();
        int solvedCount = solvedQuestionIds.size();
        double passRatio = submitCount > 0 ? (double) passSubmits / submitCount : 0.0;

        // 批量获取通过题目的详情以解析题目分类与难度
        List<TbQuestion> solvedQuestions = CollUtil.isNotEmpty(solvedQuestionIds)
                ? questionMapper.selectBatchIds(solvedQuestionIds)
                : Collections.emptyList();

        int dsCount = 0;
        int algoCount = 0;
        int mathCount = 0;
        int mediumCount = 0;
        int hardCount = 0;

        for (TbQuestion q : solvedQuestions) {
            String title = StrUtil.nullToEmpty(q.getTitle());
            String content = StrUtil.nullToEmpty(q.getContent());
            String text = title + " " + content;

            // 数据结构特征识别：数组、哈希表、栈、队列、链表、树、二叉树、图、堆等
            if (text.contains("数组") || text.contains("哈希") || text.contains("栈")
                    || text.contains("队列") || text.contains("链表") || text.contains("树")
                    || text.contains("图") || text.contains("堆") || text.contains("二叉")) {
                dsCount++;
            }

            // 算法思维特征识别：动态规划、dp、贪心、回溯、二分、搜索、双指针、递归、排序等
            if (text.contains("动态规划") || text.contains("贪心") || text.contains("回溯")
                    || text.contains("二分") || text.contains("双指针") || text.contains("递归")
                    || text.contains("深度优先") || text.contains("广度优先") || text.contains("滑动窗口")) {
                algoCount++;
            }

            // 数学逻辑特征识别：数学、位运算、质数、公约数、矩阵、几何、概率、异或等
            if (text.contains("数学") || text.contains("位运算") || text.contains("质数")
                    || text.contains("进制") || text.contains("异或") || text.contains("倍数")
                    || text.contains("整除") || text.contains("几何")) {
                mathCount++;
            }

            // 难度统计
            if (QuestionDifficultyEnum.MEDIUM.getCode().equals(q.getDifficulty())) {
                mediumCount++;
            } else if (QuestionDifficultyEnum.HARD.getCode().equals(q.getDifficulty())) {
                hardCount++;
            }
        }

        // 1. 数据结构能力评分 (0~100)
        int dsScore = 20 + Math.min(30, solvedCount * 5) + Math.min(35, dsCount * 8) + (int) (passRatio * 15);
        radar.setDataStructure(Math.min(100, Math.max(0, dsScore)));

        // 2. 算法思维能力评分 (0~100)
        int algoScore = 20 + Math.min(30, algoCount * 8) + Math.min(30, mediumCount * 6 + hardCount * 12) + Math.min(20, solvedCount * 3);
        radar.setAlgorithm(Math.min(100, Math.max(0, algoScore)));

        // 3. 工程实现能力评分 (0~100)
        int implScore = (int) (passRatio * 40) + Math.min(35, solvedCount * 5) + Math.min(25, submitCount * 2);
        radar.setImplementation(Math.min(100, Math.max(0, implScore)));

        // 4. 数学逻辑能力评分 (0~100)
        int mathScore = 20 + Math.min(40, mathCount * 10) + (int) (passRatio * 20) + Math.min(20, solvedCount * 3);
        radar.setMath(Math.min(100, Math.max(0, mathScore)));

        // 5. 竞赛实战能力评分 (0~100)
        LambdaQueryWrapper<TbUserExam> examWrapper = new LambdaQueryWrapper<TbUserExam>()
                .eq(TbUserExam::getUserId, userId);
        if (startTime != null) {
            examWrapper.ge(TbUserExam::getCreateTime, startTime);
        }
        List<TbUserExam> userExams = userExamMapper.selectList(examWrapper);
        int compScore;
        if (CollUtil.isNotEmpty(userExams)) {
            int examCount = userExams.size();
            int maxExamScore = userExams.stream().mapToInt(e -> e.getScore() != null ? e.getScore() : 0).max().orElse(0);
            compScore = 30 + Math.min(30, examCount * 15) + (int) (maxExamScore * 0.4);
        } else {
            long examSubmits = submits.stream().filter(s -> s.getExamId() != null).count();
            if (examSubmits > 0) {
                compScore = 25 + Math.min(35, (int) examSubmits * 8);
            } else {
                compScore = Math.min(40, 15 + solvedCount * 3);
            }
        }
        radar.setCompetition(Math.min(100, Math.max(0, compScore)));

        return radar;
    }

    // 获取当前登录用户解题日历按年份统计
    @Override
    public UserCalendarVO getUserCalendar(UserCalendarQueryDTO queryDTO) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        // 1. 确定目标年份，选填默认为当前自然年
        int currentYear = LocalDate.now().getYear();
        int targetYear = (queryDTO != null && queryDTO.getYear() != null && queryDTO.getYear() >= 2000 && queryDTO.getYear() <= 2100)
                ? queryDTO.getYear()
                : currentYear;

        // 2. 计算目标自然年起止时间
        LocalDateTime yearStart = LocalDateTime.of(targetYear, 1, 1, 0, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(targetYear, 12, 31, 23, 59, 59);

        // 3. 统计该用户在目标自然年内的全部提交记录
        LambdaQueryWrapper<TbUserSubmit> wrapper = new LambdaQueryWrapper<TbUserSubmit>()
                .select(TbUserSubmit::getCreateTime)
                .eq(TbUserSubmit::getUserId, userId)
                .ge(TbUserSubmit::getCreateTime, yearStart)
                .le(TbUserSubmit::getCreateTime, yearEnd);
        List<TbUserSubmit> submits = userSubmitMapper.selectList(wrapper);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Integer> dayCountMap = new HashMap<>();
        if (CollUtil.isNotEmpty(submits)) {
            for (TbUserSubmit submit : submits) {
                if (submit.getCreateTime() != null) {
                    String dateStr = submit.getCreateTime().format(formatter);
                    dayCountMap.merge(dateStr, 1, Integer::sum);
                }
            }
        }

        // 4. 构建自然年内的连续每日打卡统计列表
        LocalDate currDate = LocalDate.of(targetYear, 1, 1);
        LocalDate lastDate = LocalDate.of(targetYear, 12, 31);
        List<UserCalendarItemVO> calendarData = new ArrayList<>();
        while (!currDate.isAfter(lastDate)) {
            String dateStr = currDate.format(formatter);
            calendarData.add(new UserCalendarItemVO(dateStr, dayCountMap.getOrDefault(dateStr, 0)));
            currDate = currDate.plusDays(1);
        }

        UserCalendarVO calendarVO = new UserCalendarVO();
        calendarVO.setYear(targetYear);
        calendarVO.setTotalSubmissions(submits != null ? submits.size() : 0);
        calendarVO.setCalendarData(calendarData);

        return calendarVO;
    }

    // 清除指定用户的详情缓存
    @Override
    public void evictUserCache(Long userId) {
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        userCacheManager.deleteUserCache(userId);
    }

    // 手机号脱敏（保留前3后4）
    // 审核头像图片，读取失败或审核服务不可用时返回 null
    private AiModerationVO moderateAvatar(MultipartFile file) {
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : MediaType.IMAGE_PNG_VALUE;
        try {
            return aiModerationClient.moderateImage(contentType, file.getBytes());
        } catch (IOException e) {
            log.warn("读取头像内容失败，跳过图片审核: {}", e.getMessage());
            return null;
        }
    }

    // 审核不通过时拒绝本次操作；审核服务不可用（结论为空）时放行
    private void rejectIfViolated(AiModerationVO result, String target) {
        if (result != null && Boolean.FALSE.equals(result.getPass())) {
            log.info("内容审核未通过, target = {}, category = {}", target, result.getCategory());
            throw new ServiceException(ResultCode.FAILED_AI_CONTENT_REJECTED,
                    target + "可能涉及「" + result.getCategory() + "」，请修改后重试");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
