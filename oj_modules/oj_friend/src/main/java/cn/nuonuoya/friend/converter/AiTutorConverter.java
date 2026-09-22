package cn.nuonuoya.friend.converter;

import cn.nuonuoya.friend.domain.TbAiChatMessage;
import cn.nuonuoya.friend.enums.AiChatRoleEnum;
import cn.nuonuoya.friend.vo.AiTutorMessageVO;

import java.util.List;

// AI 辅导消息转换
public class AiTutorConverter {

    private AiTutorConverter() {
    }

    // 消息实体转视图
    public static AiTutorMessageVO toMessageVO(TbAiChatMessage message) {
        AiTutorMessageVO vo = new AiTutorMessageVO();
        vo.setMessageId(message.getMessageId());
        vo.setFromUser(AiChatRoleEnum.USER.getCode().equals(message.getRole()));
        vo.setAction(message.getAction());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    // 批量转换
    public static List<AiTutorMessageVO> toMessageVOList(List<TbAiChatMessage> messages) {
        return messages.stream().map(AiTutorConverter::toMessageVO).toList();
    }
}
