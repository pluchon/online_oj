package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 公开示例
@Getter
@Setter
@ToString
public class AiTutorSampleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 展示用输入
    private String input;

    // 展示用输出
    private String output;
}
