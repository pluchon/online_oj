package cn.nuonuoya.friend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.friend.cache.MessageCacheManager;
import cn.nuonuoya.friend.dto.MessageQueryDTO;
import cn.nuonuoya.mybatis.utils.TransactionUtils;
import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.domain.TbMessageText;
import cn.nuonuoya.friend.enums.MessageTypeEnum;
import cn.nuonuoya.friend.enums.MessageReadStatusEnum;
import cn.nuonuoya.friend.mapper.MessageMapper;
import cn.nuonuoya.friend.mapper.MessageTextMapper;
import cn.nuonuoya.friend.service.MessageService;
import cn.nuonuoya.friend.vo.MessageVO;
import cn.nuonuoya.security.utils.SecurityUtils;
import cn.nuonuoya.security.exception.ServiceException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// 站内消息服务实现类
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    // 系统消息发送方标识
    private static final Long SYSTEM_SENDER_ID = 0L;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageTextMapper messageTextMapper;

    @Autowired
    private MessageCacheManager messageCacheManager;

    // 分页查询当前登录用户的站内消息列表（按类型、关键词在数据库侧筛选后分页）
    @Override
    public TableDataResult<MessageVO> list(MessageQueryDTO queryDTO) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }
        queryDTO.setKeyword(StrUtil.trimToNull(queryDTO.getKeyword()));

        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<MessageVO> voList = messageMapper.selectUserMessageList(userId, queryDTO);
        if (CollUtil.isEmpty(voList)) {
            return TableDataResult.empty();
        }
        return TableDataResult.success(voList, new PageInfo<>(voList).getTotal());
    }

    // 获取未读消息数量
    @Override
    public int getUnreadCount() {
        Long userId = SecurityUtils.getUserId();
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
        Long userId = SecurityUtils.getUserId();
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
        if (MessageReadStatusEnum.UNREAD.getCode().equals(message.getIsRead())) {
            TbMessage updateEntity = new TbMessage();
            updateEntity.setMessageId(messageId);
            updateEntity.setIsRead(MessageReadStatusEnum.READ.getCode());
            updateEntity.setUpdateTime(LocalDateTime.now());
            messageMapper.updateById(updateEntity);
            TransactionUtils.afterCommit(() -> messageCacheManager.decrementUnreadCount(userId));
        }
    }

    // 一键全部标记为已读
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAll() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_UNAUTHORIZED);
        }

        // 批量更新数据库中当前用户所有未读消息
        messageMapper.update(null, new LambdaUpdateWrapper<TbMessage>()
                .set(TbMessage::getIsRead, MessageReadStatusEnum.READ.getCode())
                .set(TbMessage::getUpdateTime, LocalDateTime.now())
                .eq(TbMessage::getRecId, userId)
                .eq(TbMessage::getIsRead, MessageReadStatusEnum.UNREAD.getCode()));

        // 事务提交后清空缓存中的未读计数
        TransactionUtils.afterCommit(() -> messageCacheManager.clearUnreadCount(userId));
    }

    // 以系统身份写入消息正文与投递记录，事务提交后给收件人未读数加一
    @Override
    public void sendSystemMessage(Long recId, MessageTypeEnum type, String title, String content) {
        LocalDateTime now = LocalDateTime.now();
        TbMessageText text = new TbMessageText();
        text.setMessageType(type.getCode());
        text.setMessageTitle(title);
        text.setMessageContent(content);
        text.setCreateBy(SYSTEM_SENDER_ID);
        text.setCreateTime(now);
        messageTextMapper.insert(text);

        TbMessage message = new TbMessage();
        message.setTextId(text.getTextId());
        message.setSendId(SYSTEM_SENDER_ID);
        message.setRecId(recId);
        message.setIsRead(MessageReadStatusEnum.UNREAD.getCode());
        message.setCreateBy(SYSTEM_SENDER_ID);
        message.setCreateTime(now);
        messageMapper.insert(message);
        TransactionUtils.afterCommit(() -> messageCacheManager.incrementUnreadCount(recId));
    }
}
