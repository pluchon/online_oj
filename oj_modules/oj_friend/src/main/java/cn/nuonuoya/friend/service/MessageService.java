package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.dto.MessageQueryDTO;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.vo.MessageVO;

// 站内消息服务接口
public interface MessageService {

    // 分页查询当前登录用户的消息列表
    TableDataResult<MessageVO> list(MessageQueryDTO queryDTO);

    // 获取当前登录用户未读消息数量
    int getUnreadCount();

    // 标记单条消息为已读
    void readMessage(Long messageId);

    // 一键全部标记为已读
    void readAll();
}
