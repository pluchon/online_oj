package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.enums.UserIdentity;
import cn.nuonuoya.friend.cache.UserCacheManager;
import cn.nuonuoya.friend.domain.TbUser;
import cn.nuonuoya.friend.dto.UserLoginDTO;
import cn.nuonuoya.friend.dto.UserSendCodeDTO;
import cn.nuonuoya.friend.mapper.UserMapper;
import cn.nuonuoya.friend.service.UserService;
import cn.nuonuoya.message.sms.config.SmsProperties;
import cn.nuonuoya.message.sms.service.SmsService;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.security.service.TokenService;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import cn.nuonuoya.friend.converter.UserConverter;
import cn.nuonuoya.friend.dto.UserProfileUpdateDTO;
import cn.nuonuoya.friend.service.OssService;
import cn.nuonuoya.friend.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

// C端用户业务实现类
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

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

    // 发送短信验证码具体实现
    @Override
    public OJResult<Void> sendCode(UserSendCodeDTO sendCodeDTO) {
        String phone = sendCodeDTO.getPhone();
        // 1. 手机号防守校验
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        String intervalKey = CacheConstants.SMS_CODE_INTERVAL_KEY + phone;
        String countKey = CacheConstants.SMS_CODE_COUNT_KEY + phone;
        String codeKey = CacheConstants.SMS_CODE_KEY + phone;

        // 2. 校验发送频率（防刷冷却，默认60秒）
        if (Boolean.TRUE.equals(redisService.hasKey(intervalKey))) {
            Long expire = redisService.getExpire(intervalKey, TimeUnit.SECONDS);
            long seconds = (expire != null && expire > 0) ? expire : 60;
            return OJResult.fail(ResultCode.FAILED_FREQUENT.getCode(), "操作过于频繁，请在 " + seconds + " 秒后再试");
        }

        // 3. 校验单手机号单日累计发送上限
        int maxDailyCount = smsProperties.getMaxDailyCount() != null ? smsProperties.getMaxDailyCount() : 10;
        Integer currentCount = redisService.getCacheObject(countKey, Integer.class);
        if (currentCount != null && currentCount >= maxDailyCount) {
            return OJResult.fail(ResultCode.FAILED_SEND_SMS_EXCEED);
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
            // 模拟发码模式：跳过远程调用，只打印日志，方便本地/测试环境零资费调试
            log.info("【模拟短信发码模式已启用(isConfirm=false)】向手机号 {} 模拟发码成功，验证码: {}", phone, code);
            sendSuccess = true;
        }

        if (!sendSuccess) {
            return OJResult.fail(ResultCode.FAILED_SEND_SMS);
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

        return OJResult.ok();
    }

    // 用户短信验证码登录与自动注册实现
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OJResult<String> login(UserLoginDTO loginDTO) {
        String phone = loginDTO.getPhone();
        String code = loginDTO.getCode();

        // 1. 参数防御校验
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            return OJResult.fail(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 2. 校验短信验证码（比对 Redis 中缓存的验证码）
        String codeKey = CacheConstants.SMS_CODE_KEY + phone;
        String cachedCode = redisService.getCacheObject(codeKey, String.class);
        if (!StringUtils.hasText(cachedCode) || !cachedCode.equals(code.trim())) {
            return OJResult.fail(ResultCode.FAILED_CODE_ERROR);
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
        return OJResult.ok(token);
    }

    // 自动注册新用户
    private TbUser registerNewUser(String phone) {
        TbUser newUser = new TbUser();
        newUser.setPhone(phone);
        // 初始化默认昵称，例如：用户_后4位_随机4位
        String phoneTail = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        newUser.setNickName("用户_" + phoneTail + "_" + RandomUtil.randomString(4));
        newUser.setHeadImage("");
        // 性别默认为 0（保密）
        newUser.setSex(0);
        // 初始状态为 1（正常）
        newUser.setStatus(1);
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

    // 获取当前登录用户个人资料
    @Override
    public OJResult<UserVO> getUserProfile() {
        Long userId = getCurrentUserId();
        log.info("[个人中心] 获取当前用户, userId: {}", userId);
        if (userId == null) {
            log.warn("[个人中心] 当前上下文 userId 为空");
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        UserVO vo = userCacheManager.getUserById(userId);
        if (vo == null) {
            log.warn("[个人中心] 数据库未查到用户记录, userId: {}", userId);
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return OJResult.ok(vo);
    }

    // 更新当前登录用户个人资料
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OJResult<Void> updateUserProfile(UserProfileUpdateDTO updateDTO) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        TbUser existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        // 构造更新实体
        TbUser updateEntity = new TbUser();
        updateEntity.setUserId(userId);
        updateEntity.setNickName(updateDTO.getNickName().trim());
        if (StringUtils.hasText(updateDTO.getHeadImage())) {
            updateEntity.setHeadImage(updateDTO.getHeadImage().trim());
        }
        updateEntity.setSex(updateDTO.getSex());
        updateEntity.setEmail(updateDTO.getEmail() != null ? updateDTO.getEmail().trim() : "");
        updateEntity.setWechat(updateDTO.getWechat() != null ? updateDTO.getWechat().trim() : "");
        updateEntity.setSchoolName(updateDTO.getSchoolName() != null ? updateDTO.getSchoolName().trim() : "");
        updateEntity.setMajorName(updateDTO.getMajorName() != null ? updateDTO.getMajorName().trim() : "");
        updateEntity.setIntroduce(updateDTO.getIntroduce() != null ? updateDTO.getIntroduce().trim() : "");
        userMapper.updateById(updateEntity);

        // 主动剔除用户详情缓存，保证下次获取最新数据
        userCacheManager.deleteUserCache(userId);

        // 若用户昵称发生变更，同步刷新Redis会话中的登录用户信息
        String userKey = getCurrentUserKey();
        if (StringUtils.hasText(userKey)) {
            String tokenKey = CacheConstants.LOGIN_TOKEN_KEY + userKey;
            LoginUser loginUser = redisService.getCacheObject(tokenKey, LoginUser.class);
            if (loginUser != null) {
                loginUser.setNickName(updateDTO.getNickName().trim());
                Long expire = redisService.getExpire(tokenKey, TimeUnit.MINUTES);
                if (expire != null && expire > 0) {
                    redisService.setCacheObject(tokenKey, loginUser, expire, TimeUnit.MINUTES);
                } else {
                    redisService.setCacheObject(tokenKey, loginUser, CacheConstants.EXPIRATION, TimeUnit.MINUTES);
                }
            }
        }

        return OJResult.ok();
    }

    // 上传当前登录用户头像至OSS并更新资料
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OJResult<String> uploadAvatar(MultipartFile file) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        TbUser existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            return OJResult.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        // 调用OSS上传服务存储至指定目录
        String avatarUrl = ossService.uploadAvatar(file);

        // 同步持久化至用户表
        TbUser updateEntity = new TbUser();
        updateEntity.setUserId(userId);
        updateEntity.setHeadImage(avatarUrl);
        userMapper.updateById(updateEntity);

        // 主动剔除用户详情缓存
        userCacheManager.deleteUserCache(userId);

        return OJResult.ok(avatarUrl);
    }

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
            if (StringUtils.hasText(headerUserId)) {
                return cn.hutool.core.convert.Convert.toLong(headerUserId);
            }
            String token = request.getHeader(HttpConstants.AUTHENTICATION);
            if (StringUtils.hasText(token)) {
                return tokenService.getUserId(tokenService.cleanToken(token));
            }
        }
        return null;
    }

    // 获取当前用户会话Key（优先ThreadLocal，兼顾HttpServletRequest兜底）
    private String getCurrentUserKey() {
        String userKey = ThreadLocalUtil.get(HttpConstants.USER_KEY, String.class);
        if (StringUtils.hasText(userKey)) {
            return userKey;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String headerUserKey = request.getHeader(HttpConstants.USER_KEY);
            if (StringUtils.hasText(headerUserKey)) {
                return headerUserKey;
            }
            String token = request.getHeader(HttpConstants.AUTHENTICATION);
            if (StringUtils.hasText(token)) {
                return tokenService.getUserKey(tokenService.cleanToken(token));
            }
        }
        return null;
    }
}
