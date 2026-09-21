package cn.nuonuoya.common.domain;

import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 分页查询基础请求类（入参在 setter 中统一纠正）
@Getter
@ToString
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 默认页码
    private static final int DEFAULT_PAGE_NUM = 1;

    // 默认每页条数
    private static final int DEFAULT_PAGE_SIZE = 10;

    // 每页条数上限
    private static final int MAX_PAGE_SIZE = 50;

    // 当前页码
    private Integer pageNum = DEFAULT_PAGE_NUM;

    // 每页条数
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    // 设置当前页码（空或小于1时取默认值）
    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    // 设置每页条数（空或非法时取默认值，超过上限时取上限）
    public void setPageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            this.pageSize = DEFAULT_PAGE_SIZE;
        } else {
            this.pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        }
    }
}
