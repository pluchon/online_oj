package cn.nuonuoya.common.domain;

import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 分页查询基础请求类
@ToString
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 当前页码（默认 1）
    private Integer pageNum = 1;

    // 每页条数（默认固定 10，上限 50）
    private Integer pageSize = 10;

    // 获取当前页码（空或小于1时兜底为1）
    public Integer getPageNum() {
        if (pageNum == null || pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    // 设置当前页码
    public void setPageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            this.pageNum = 1;
        } else {
            this.pageNum = pageNum;
        }
    }

    // 获取每页条数（空或非法时兜底为10，超大时上限限制为50）
    public Integer getPageSize() {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        if (pageSize > 50) {
            return 50;
        }
        return pageSize;
    }

    // 设置每页条数
    public void setPageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            this.pageSize = 10;
        } else if (pageSize > 50) {
            this.pageSize = 50;
        } else {
            this.pageSize = pageSize;
        }
    }
}
