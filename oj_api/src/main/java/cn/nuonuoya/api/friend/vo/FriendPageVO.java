package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 分页结果（C端内部接口通用）
@Getter
@Setter
@ToString
public class FriendPageVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 总记录数
    private long total;

    // 当前页数据
    private List<T> rows;
}
