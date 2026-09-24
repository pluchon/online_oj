package cn.nuonuoya.friend.service.impl;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.Setter;

// 一次辅导流式回复的累积状态（只在同一个订阅的回调中使用）
@Getter
@Setter
class AiTutorStreamState {

    // 已收到的回复全文
    private final StringBuilder reply = new StringBuilder();

    // 结束事件的数据（模型与用量），未收到时为空
    private JSONObject done;

    // 是否正常完成：收到结束事件且回复非空
    boolean isCompleted() {
        return done != null && !reply.isEmpty();
    }
}
