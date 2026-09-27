package cn.nuonuoya.system.dto;

import cn.nuonuoya.common.domain.PageQuery;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 申诉分页查询DTO（条件为空表示不限）
@Getter
@Setter
@ToString
public class AppealQueryDTO extends PageQuery {

    // 申诉人ID
    private Long userId;

    // 题目名称（模糊查询）
    private String title;

    // 最近多少天内的申诉（按申诉时间）
    private Integer days;
}
