package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import cn.nuonuoya.friend.cache.MessageCacheManager;
import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.domain.TbMessageText;
import cn.nuonuoya.friend.mapper.MessageMapper;
import cn.nuonuoya.friend.service.MessageService;
import cn.nuonuoya.friend.vo.MessageVO;
import cn.nuonuoya.security.exception.ServiceException;
import cn.nuonuoya.security.service.TokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 站内消息服务实现类
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageCacheManager messageCacheManager;

    @Autowired
    private TokenService tokenService;

    // 获取当前登录用户ID
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

    // 分页查询当前登录用户的站内消息列表
    @Override
    public TableDataResult<MessageVO> list(PageQuery pageQuery) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        int pageNum = pageQuery != null && pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1;
        int pageSize = pageQuery != null && pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10;

        PageHelper.startPage(pageNum, pageSize);
        List<TbMessage> messageList = messageMapper.selectList(new LambdaQueryWrapper<TbMessage>()
                .eq(TbMessage::getRecId, userId)
                .orderByDesc(TbMessage::getCreateTime));

        if (CollUtil.isEmpty(messageList)) {
            return TableDataResult.empty();
        }

        long total = new PageInfo<>(messageList).getTotal();
        List<MessageVO> voList = new ArrayList<>(messageList.size());

        for (TbMessage message : messageList) {
            MessageVO vo = new MessageVO();
            vo.setMessageId(message.getMessageId());
            vo.setTextId(message.getTextId());
            vo.setSendId(message.getSendId());
            vo.setIsRead(message.getIsRead());
            vo.setCreateTime(message.getCreateTime());

            // 优先通过 Redis String 缓存加载消息正文
            TbMessageText text = messageCacheManager.getMessageText(message.getTextId());
            if (text != null) {
                vo.setTitle(text.getMessageTitle());
                vo.setContent(text.getMessageContent());
            } else {
                vo.setTitle("系统通知");
                vo.setContent("");
            }
            voList.add(vo);
        }

        return TableDataResult.success(voList, total);
    }

    // 获取未读消息数量
    @Override
    public int getUnreadCount() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return 0;
        }
        return messageCacheManager.getUnreadCount(userId);
    }

    // 标记单条消息为已读
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readMessage(Long messageId) {
        if (messageId == null) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        TbMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }

        // 仅限接收人自身标记已读，杜绝越权
        if (!Objects.equals(message.getRecId(), userId)) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        // 若原状态为未读，执行更新并递减缓存计数
        if (Integer.valueOf(0).equals(message.getIsRead())) {
            TbMessage updateEntity = new TbMessage();
            updateEntity.setMessageId(messageId);
            updateEntity.setIsRead(1);
            updateEntity.setUpdateTime(LocalDateTime.now());
            messageMapper.updateById(updateEntity);

            messageCacheManager.decrementUnreadCount(userId);
        }
    }

    // 一键全部标记为已读
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAll() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        // 批量更新数据库中当前用户所有未读消息
        messageMapper.update(null, new LambdaUpdateWrapper<TbMessage>()
                .set(TbMessage::getIsRead, 1)
                .set(TbMessage::getUpdateTime, LocalDateTime.now())
                .eq(TbMessage::getRecId, userId)
                .eq(TbMessage::getIsRead, 0));

        // 清空缓存中的未读计数
        messageCacheManager.clearUnreadCount(userId);
    }
}
