package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 提交记录分页结果
@Getter
@Setter
@ToString
public class FriendSubmitPageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 总记录数
    private long total;

    // 当前页数据
    private List<FriendSubmitVO> rows;
}
