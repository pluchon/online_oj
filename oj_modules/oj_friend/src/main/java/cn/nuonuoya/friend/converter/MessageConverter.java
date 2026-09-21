package cn.nuonuoya.friend.converter;

import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.domain.TbMessageText;
import cn.nuonuoya.friend.vo.MessageVO;

// 站内消息对象转换器
public class MessageConverter {

    // 正文缺失时的默认标题
    private static final String DEFAULT_TITLE = "系统通知";

    // 消息记录与正文合并为视图对象（正文缺失时使用默认标题与空内容）
    public static MessageVO toVO(TbMessage message, TbMessageText text) {
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getMessageId());
        vo.setTextId(message.getTextId());
        vo.setSendId(message.getSendId());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        vo.setTitle(text != null ? text.getMessageTitle() : DEFAULT_TITLE);
        vo.setContent(text != null ? text.getMessageContent() : "");
        return vo;
    }
}
