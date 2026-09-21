package cn.nuonuoya.friend.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目列表分页与全文检索DTO
@ToString
@Schema(description = "题目列表分页与全文检索参数")
public class QuestionQueryDTO extends PageQuery {

    // C端题库默认分页条数固定为10
    public static final int DEFAULT_PAGE_SIZE = 10;

    public QuestionQueryDTO() {
        super();
        setPageSize(DEFAULT_PAGE_SIZE);
    }

    // 获取每页条数（默认与兜底固定为10）
    @Override
    public Integer getPageSize() {
        Integer size = super.getPageSize();
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }

    // 设置每页条数（空或非法时兜底为10）
    @Override
    public void setPageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            super.setPageSize(DEFAULT_PAGE_SIZE);
        } else {
            super.setPageSize(pageSize);
        }
    }

    // 搜索关键字（按标题或内容模糊匹配）
    @Schema(description = "搜索关键字（按标题或内容模糊匹配）")
    private String keyword;

    // 题目难度（1:简单 2:中等 3:困难，为空表示全部）
    @Schema(description = "题目难度（1:简单 2:中等 3:困难，为空表示全部）")
    private Integer difficulty;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }
}
