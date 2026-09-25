package cn.nuonuoya.system.service;

// 题目官方题解业务接口（随题目一起保存）
public interface QuestionEditorialService {

    // 查询题目的题解内容，没有时返回空字符串
    String getContent(Long questionId);

    // 保存题目的题解：内容为空时删除已有题解，否则新增或覆盖
    void save(Long questionId, String content);

    // 删除题目的题解
    void remove(Long questionId);
}
