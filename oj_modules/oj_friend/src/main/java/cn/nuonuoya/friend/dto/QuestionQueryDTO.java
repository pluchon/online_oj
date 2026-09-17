package cn.nuonuoya.friend.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 题目列表分页与全文检索DTO
@Getter
@Setter
@ToString
@Schema(description = "题目列表分页与全文检索参数")
public class QuestionQueryDTO extends PageQuery {

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
