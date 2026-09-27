package cn.nuonuoya.api.friend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// 申诉分页查询条件（条件为空表示不限）
@Getter
@Setter
@ToString
public class FriendAppealQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 当前页码
    private Integer pageNum;

    // 每页条数
    private Integer pageSize;

    // 申诉人ID
    private Long userId;

    // 题目ID范围（调用方按题目名称解析出的题，传空列表表示没有匹配的题）
    private List<Long> questionIds;

    // 申诉时间下限
    private LocalDateTime startTime;
}
